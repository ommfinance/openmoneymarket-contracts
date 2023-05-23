package finanace.omm.score.core.multicall;

import finance.omm.core.score.interfaces.MultiCall;
import score.Address;
import score.Context;
import score.annotation.External;
import score.annotation.Keep;

import java.math.BigInteger;
import java.util.Map;

public class Multicall implements MultiCall {

    public static final String TAG = "Multicall";

    public Multicall() {
    }

    public static class Result {
        public Result(boolean b, Object result) {
            this.success = b;
            this.returnData = result;
        }

        @Keep
        public boolean success;

        @Keep
        public Object returnData;
    }

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
    }

    private static Object[] convertToType(String[] params) {
        Object[] results = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            if (params[i].startsWith("hx") || params[i].startsWith("cx")) {
                results[i] = Address.fromString(params[i]);
            } else if (params[i].equals("false") || params[i].equals("true")) {
                results[i] = Boolean.parseBoolean(params[i]);
            } else if (params[i].startsWith("0x")) {
                results[i] = new BigInteger(params[i].substring(2), 16);
            } else {
                results[i] = params[i];
            }
        }
        return results;
    }

    @External(readonly = true)
    public Map<String, Object> aggregate(Call[] calls) {
        long blockNumber = Context.getBlockHeight();
        Object[] returnData = new Object[calls.length];

        for (int i = 0; i < calls.length; i++) {

            Address contractAddress = calls[i].target;
            String method = calls[i].method;
            try {
                Object[] params = convertToType(calls[i].params);

                if (calls[i].params.length == 0) {
                    returnData[i] = Context.call(contractAddress, method);
                } else {
                    returnData[i] = Context.call(contractAddress, method, params);
                }
            } catch (Exception e) {
                Context.println(e.getMessage());
                Context.revert(e + ": Multicall aggregate: call failed " + contractAddress + " method: " + method);
            }
        }
        return Map.of("blockNumber", blockNumber, "returnData", returnData);

    }

    @External(readonly = true)
    public Map<String, Object> tryAggregate(boolean requireSuccess, Call[] calls) {
        Result[] returnData = new Result[calls.length];
        for (int i = 0; i < calls.length; i++) {
            Address contractAddress = calls[i].target;
            String method = calls[i].method;
            try {
                Object[] params = convertToType(calls[i].params);
                Object result = new Object();
                if (calls[i].params.length == 0) {
                    result = Context.call(contractAddress, method);
                } else {
                    result = Context.call(contractAddress, method, params);
                }
                returnData[i] = new Result(Boolean.TRUE, result);
            } catch (Exception e) {
                String errMessage = "Multicall tryAggregate: call failed " + contractAddress + " method: " + method;
                if (requireSuccess) {
                    Context.println(e.getMessage());
                    Context.revert(
                            e + ": " + errMessage);
                }
                returnData[i] = new Result(Boolean.FALSE, errMessage);
            }
        }
        return Map.of("returnData", returnData);
    }


    @External(readonly = true)
    public BigInteger getBalance(Address addr) {
        return Context.getBalance(addr);
    }

    @External(readonly = true)
    public long getCurrentBlockTimestamp() {
        return Context.getBlockTimestamp();
    }

    @External(readonly = true)
    public long getBlockNumber() {
        return Context.getBlockHeight();
    }
}
