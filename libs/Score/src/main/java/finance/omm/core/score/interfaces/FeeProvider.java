package finance.omm.core.score.interfaces;

import java.math.BigInteger;

import score.Address;

public interface FeeProvider extends AddressProvider {

    void setLoanOriginationFeePercentage(BigInteger _percentage);
    String name();
    BigInteger calculateOriginationFee(BigInteger _amount);
    BigInteger getLoanOriginationFeePercentage();
    void tokenFallback(Address _from, BigInteger _value, byte[] _data);
    void transferFund(Address _token, BigInteger _value, Address _to);
}
