package finance.omm.gradle.plugin.utils;

import java.math.BigInteger;

public enum Network {
    SEJONG("https://sejong.net.solidwallet.io/api/v3", BigInteger.valueOf(83L)),
    MAINNET("https://sejong.net.solidwallet.io/api/v3", BigInteger.valueOf(1L)),
    LOCAL("http://localhost:9082/api/v3", BigInteger.valueOf(3L));

    private final String url;
    private final BigInteger nid;

    Network(String url, BigInteger nid) {
        this.url = url;
        this.nid = nid;
    }

    public String getUrl() {
        return url;
    }

    public BigInteger getNid() {
        return nid;
    }

    public static Network getNetwork(String network) {
        switch (network.toLowerCase()) {
            case "sejong":
                return SEJONG;
            case "mainnet":
                return MAINNET;
            default:
                return LOCAL;
        }
    }
}
