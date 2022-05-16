package finance.omm.utils.constants;

import score.Address;

public class AddressConstant {

    public static final Address ZERO_ADDRESS = new Address(new byte[21]);
    public static final Address ZERO_SCORE_ADDRESS = Address.fromString("cx0000000000000000000000000000000000000000");
}
