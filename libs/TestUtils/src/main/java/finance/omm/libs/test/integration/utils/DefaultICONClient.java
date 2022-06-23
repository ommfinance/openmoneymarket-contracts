package finance.omm.libs.test.integration.utils;


import static foundation.icon.score.client.DefaultScoreClient.callData;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import finance.omm.libs.test.integration.Environment.Chain;
import foundation.icon.icx.Wallet;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.IconJsonModule;
import foundation.icon.jsonrpc.JsonrpcClient;
import foundation.icon.jsonrpc.SendTransactionParamSerializer;
import foundation.icon.jsonrpc.model.DeployData;
import foundation.icon.jsonrpc.model.Hash;
import foundation.icon.jsonrpc.model.SendTransactionParam;
import foundation.icon.jsonrpc.model.TransactionParam;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.RevertedException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.util.encoders.Base64;
import score.UserRevertedException;

/**
 * inspired from foundation.icon.score.client.DefaultScoreClient
 */

public class DefaultICONClient {

    public static final Address ZERO_ADDRESS = new Address("cx0000000000000000000000000000000000000000");
    public static final BigInteger DEFAULT_STEP_LIMIT = new BigInteger("9502f900", 16);
    public static final long BLOCK_INTERVAL = 200;
    public static final long DEFAULT_RESULT_RETRY_WAIT = 1000;
    public static final long DEFAULT_RESULT_TIMEOUT = 1000;

    private final JsonrpcClient client;
    private final BigInteger nid;

    public DefaultICONClient(Chain chain) {
        client = new JsonrpcClient(chain.getEndpointURL());
        client.mapper().registerModule(new IconJsonModule());
        client.mapper().setSerializationInclusion(Include.NON_NULL);

        this.nid = chain.networkId;

    }

    public Address deploy(
            Wallet wallet, Address address,
            String scoreFilePath, Map<String, Object> params) {
        return this.deploy(wallet, address, scoreFilePath, params, DEFAULT_RESULT_TIMEOUT);
    }

    public Address deploy(
            Wallet wallet, Address address,
            String scorePath, Map<String, Object> params,
            long timeout) {

        String contentType;
        if (scorePath.endsWith(".jar")) {
            contentType = "application/java";
        } else if (scorePath.endsWith(".zip")) {
            contentType = "application/zip";
        } else {
            throw new RuntimeException("not supported score file");
        }

        byte[] content;
        try {
            URI uri = getURI(scorePath);
            content = IOUtils.toByteArray(uri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return deploy(wallet, address, params, timeout, content, contentType);
    }

    private Address deploy(Wallet wallet, Address address, Map<String, Object> params,
            long timeout, byte[] content, String contentType) {
        SendTransactionParam tx = new SendTransactionParam(nid, address, null, "deploy",
                new DeployData(contentType, content, params));
        Hash txh = sendTransaction(client, wallet, tx);
        waitBlockInterval();
        TransactionResult txr = result(client, txh, timeout);
        System.out.println("SCORE address: " + txr.getScoreAddress());
        return txr.getScoreAddress();
    }


    public TransactionResult send(Wallet wallet, Address address,
            BigInteger valueForPayable, String method, Map<String, Object> params,
            long timeout) {
        SendTransactionParam tx = new SendTransactionParam(nid, address, valueForPayable, "call",
                callData(method, params));
        Hash txh = sendTransaction(client, wallet, tx);
        waitBlockInterval();
        return result(client, txh, timeout);
    }

    public static URI getURI(String url) {
        try {
            URL obj = new URL(url);
            return obj.toURI();
        } catch (MalformedURLException | URISyntaxException ignored) {
            return Path.of(url).toUri();
        }
    }

    public static TransactionResult result(JsonrpcClient client, Hash txh, long timeout) {
        Map<String, Object> params = Map.of("txHash", txh);
        long etime = System.currentTimeMillis() + timeout;
        TransactionResult txr = null;
        while (txr == null) {
            try {
                txr = client.request(TransactionResult.class, "icx_getTransactionResult", params);
            } catch (JsonrpcClient.JsonrpcError e) {
                if (e.getCode() == -31002 /* pending */
                        || e.getCode() == -31003 /* executing */
                        || e.getCode() == -31004 /* not found */) {
                    if (timeout > 0 && System.currentTimeMillis() >= etime) {
                        throw new RuntimeException("timeout");
                    }
                    try {
                        Thread.sleep(DEFAULT_RESULT_RETRY_WAIT);
                        System.out.println("wait for " + txh);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
        if (!BigInteger.ONE.equals(txr.getStatus())) {
            TransactionResult.Failure failure = txr.getFailure();
            int revertCode = failure.getCode().intValue();
            String revertMessage = failure.getMessage();
            if (revertCode >= 32) {
                throw new UserRevertedException(revertCode - 32, revertMessage);
            } else {
                throw new RevertedException(revertCode, revertMessage);
            }
        }
        return txr;
    }


    static Hash sendTransaction(JsonrpcClient client, Wallet wallet, SendTransactionParam sendTransactionParam) {
        Objects.requireNonNull(client, "client required not null");
        Objects.requireNonNull(wallet, "wallet required not null");
        Objects.requireNonNull(wallet, "sendTransactionParam required not null");

        sendTransactionParam.setFrom(Address.of(wallet));
        if (sendTransactionParam.getTimestamp() == null) {
            sendTransactionParam.setTimestamp(TransactionParam.currentTimestamp());
        }
        if (sendTransactionParam.getStepLimit() == null) {
            sendTransactionParam.setStepLimit(DEFAULT_STEP_LIMIT);
        }
        if (sendTransactionParam.getNid() == null) {
            throw new IllegalArgumentException("nid could not be null");
        }

        Map<String, Object> params = new HashMap<>();
        String serialized;
        try {
            serialized = SendTransactionParamSerializer.serialize(sendTransactionParam, params);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] digest = new SHA3.Digest256().digest(serialized.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.toBase64String(wallet.sign(digest));
        params.put("signature", signature);
        return client.request(Hash.class, "icx_sendTransaction", params);
    }

    static void waitBlockInterval() {
        System.out.printf("wait block interval %d msec%n", BLOCK_INTERVAL);
        try {
            Thread.sleep(BLOCK_INTERVAL);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }
}
