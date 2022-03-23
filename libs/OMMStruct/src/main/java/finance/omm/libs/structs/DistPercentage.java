package finance.omm.libs.structs;

import java.math.BigInteger;

public class DistPercentage {

    public String recipient;
    public BigInteger percentage;

    public DistPercentage() {
    }

    public DistPercentage(String recipient, BigInteger percentage) {
        this.recipient = recipient;
        this.percentage = percentage;
    }
}
