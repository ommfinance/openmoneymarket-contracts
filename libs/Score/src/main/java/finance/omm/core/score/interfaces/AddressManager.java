package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.AddressDetails;
import finance.omm.libs.structs.ReserveAddressDetails;
import score.Address;

import java.util.Map;
// same method name
public interface AddressManager {

    String name();

    void addReserveAddress(ReserveAddressDetails _reserveAddressDetails, boolean _overwrite);

    void setAddresses(AddressDetails[] _addressDetails);

    Address getAddress(String name);

    Map<String,Address> getReserveAddresses();

    Map<String,?> getAllAddresses();

    void setSCOREAddresses();

    void addAddress(String _to, String _key, Address _value);

    void addAddressToScore(String _to, String[] _names);

    void setLendingPoolAddresses();

    void setLendingPoolCoreAddresses();

    void setLendingPoolDataProviderAddresses();

    void setLiquidationManagerAddresses();

    void setOmmTokenAddresses();

    void setoICXAddresses();

    void setoUSDsAddresses();

    void setoIUSDCAddresses();

    void setdICXAddresses();

    void setdUSDsAddresses();

    void setdIUSDCAddresses();

    void setDelegationAddresses();

    void setRewardAddresses();

    void setPriceOracleAddress();

    void setStakedLpAddresses();

    void setGovernanceAddresses();

    void setDaoFundAddresses();

    void setFeeProviderAddresses();

}
