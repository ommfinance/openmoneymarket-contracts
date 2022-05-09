package finance.omm.score.core.addreess.manager;

import finance.omm.core.score.interfaces.AddressManager;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.libs.structs.ReserveAddressDetails;
import finance.omm.score.core.addreess.manager.exception.AddressManagerException;
import finance.omm.utils.db.EnumerableSet;
import java.util.Map;
import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.External;
import score.annotation.Optional;
import scorex.util.HashMap;

public class AddressManagerImpl implements AddressManager {


    public static final String TAG = "Address Provider"; // TAG NAME SAME AS PYTHON

    private static final String RESERVES = "reserves";
    private static final String O_TOKENS = "o_tokens";
    private static final String D_TOKENS = "d_tokens";

    private final DictDB<String, Address> addresses = Context.newDictDB("address", Address.class);
    private final EnumerableSet<String> _reserves = new EnumerableSet<>(RESERVES, String.class);
    private final EnumerableSet<String> _dTokens = new EnumerableSet<>(D_TOKENS, String.class);
    private final EnumerableSet<String> _oTokens = new EnumerableSet<>(O_TOKENS, String.class);

    public AddressManagerImpl() {}

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
    }

    @External
    public void addReserveAddress(ReserveAddressDetails _reserveAddressDetails, @Optional boolean _overwrite) {
        checkOwner();
        addReserve(_reserveAddressDetails, _overwrite);
        addOToken(_reserveAddressDetails, _overwrite);
        addDToken(_reserveAddressDetails, _overwrite);
    }

    private Map<String, Address> getAllReserveAddresses() {
        Map<String, Address> reserves = new HashMap<>();
        int length = _reserves.length();
        for (int i = 0; i < length; i++) {
            String key = _reserves.at(i); // reserve name
            Address address = addresses.get(key); // chekced name in address db
            if (address != null) {
                reserves.put(key, address);
            }
        }
        return reserves;
    }

    private Map<String, Address> getAllOTokenAddresses() {
        Map<String, Address> oTokens = new HashMap<>();
        int length = _oTokens.length();
        for (int i = 0; i < length; i++) {
            String key = _oTokens.at(i); // reserve name
            Address address = addresses.get(key); // chekced name in address db
            if (address != null) {
                oTokens.put(key, address);
            }
        }
        return oTokens;
    }

    private Map<String, Address> getAllDTokenAddresses() {
        Map<String, Address> dTokens = new HashMap<>();
        int length = _dTokens.length();
        for (int i = 0; i < length; i++) {
            String key = _dTokens.at(i); // reserve name
            Address address = addresses.get(key); // chekced name in address db
            if (address != null) {
                dTokens.put(key, address);
            }
        }
        return dTokens;
    }

    @External
    public void setAddresses(AddressDetails[] _addressDetails) {
        checkOwner();
        for (AddressDetails addressDetails : _addressDetails) {
            addresses.set(addressDetails.name, addressDetails.address);
        }
    }

    @External(readonly = true)
    public Address getAddress(String name) {
        return addresses.get(name);
    }

    @External(readonly = true)
    public Map<String, Address> getReserveAddresses() {
        return getAllReserveAddresses();
    }

    @External(readonly = true)
    public Map<String, ?> getAllAddresses() {
        Map<String, Address> system = Map.ofEntries(
                Map.entry("LendingPool", getAddress(Contracts.LENDING_POOL.getKey())),
                Map.entry("LendingPoolCore", getAddress(Contracts.LENDING_POOL_CORE.getKey())),
                Map.entry("LendingPoolDataProvider", getAddress(Contracts.LENDING_POOL_DATA_PROVIDER.getKey())),
                Map.entry("Staking", getAddress(Contracts.STAKING.getKey())),
                Map.entry("Governance", getAddress(Contracts.GOVERNANCE.getKey())),
                Map.entry("Delegation", getAddress(Contracts.DELEGATION.getKey())),
                Map.entry("OmmToken", getAddress(Contracts.OMM_TOKEN.getKey())),
                Map.entry("Rewards", getAddress(Contracts.REWARDS.getKey())),
                Map.entry("PriceOracle", getAddress(Contracts.PRICE_ORACLE.getKey())),
                Map.entry("StakedLp", getAddress(Contracts.STAKED_LP.getKey())),
                Map.entry("DEX", getAddress(Contracts.DEX.getKey()))
        );
        return Map.of(
                "collateral", getAllReserveAddresses(),
                "oTokens", getAllOTokenAddresses(),
                "dTokens", getAllDTokenAddresses(),
                "systemContract", system
        );

    }

    @External
    public void setSCOREAddresses() {
        checkOwner();
        setLendingPoolAddresses();
        setLendingPoolCoreAddresses();
        setLendingPoolDataProviderAddresses();
        setLiquidationManagerAddresses();
        setOmmTokenAddresses();
        setoICXAddresses();
        setoUSDsAddresses();
        setoIUSDCAddresses();
        setdICXAddresses();
        setdUSDsAddresses();
        setdIUSDCAddresses();
        setDelegationAddresses();
        setRewardAddresses();
        setGovernanceAddresses();
        setStakedLpAddresses();
        setPriceOracleAddress();
        setDaoFundAddresses();
        setFeeProviderAddresses();
    }

    @External
    public void addAddress(String _to, String _key, Address _value) {
        checkOwner();
        Address score = addresses.get(_to);
        if (score == null) {
            throw AddressManagerException.unknown(TAG + ": score name " + _to + " not matched.");
        }

        AddressDetails[] addressDetails = new AddressDetails[]{new AddressDetails(_key, _value)};
        Object[] params = new Object[]{
                addressDetails
        };
        call(score, "setAddresses", params);
    }

    @External
    public void addAddressToScore(String _to, String[] _names) {
        checkOwner();
        Address score = addresses.get(_to);
        if (score == null) {
            throw AddressManagerException.unknown(TAG + ": score name " + _to + " not matched.");
        }
        int size = _names.length;
        AddressDetails[] addressDetails = new AddressDetails[size];

        for (int i = 0; i < size; i++) {
            String name = _names[i];
            Address address = addresses.get(name);
            if (address == null) {
                throw AddressManagerException.unknown(TAG + ": wrong score name in the list.");
            }
            addressDetails[i] = new AddressDetails(name, address);
        }
        Object[] params = new Object[]{
                addressDetails
        };
        call(score, "setAddresses", params);
    }

    @External
    public void setLendingPoolAddresses() {
        checkOwner();
        AddressDetails[] lendingPoolAddressDetails = new AddressDetails[]{
                new AddressDetails(Contracts.LIQUIDATION_MANAGER.getKey(),
                        addresses.get(Contracts.LIQUIDATION_MANAGER.getKey())),
                new AddressDetails(Contracts.sICX.getKey(), addresses.get(Contracts.sICX.getKey())),
                new AddressDetails(Contracts.oICX.getKey(), addresses.get(Contracts.oICX.getKey())),
                new AddressDetails(Contracts.STAKING.getKey(), addresses.get(Contracts.STAKING.getKey())),
                new AddressDetails(Contracts.LENDING_POOL_DATA_PROVIDER.getKey(),
                        addresses.get(Contracts.LENDING_POOL_DATA_PROVIDER.getKey())),
                new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(),
                        addresses.get(Contracts.LENDING_POOL_CORE.getKey())),
                new AddressDetails(Contracts.FEE_PROVIDER.getKey(), addresses.get(Contracts.FEE_PROVIDER.getKey())),
                new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey())),
                new AddressDetails(Contracts.BRIDGE_O_TOKEN.getKey(), addresses.get(Contracts.BRIDGE_O_TOKEN.getKey())),
                new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress()),
                new AddressDetails(Contracts.OMM_TOKEN.getKey(), addresses.get(Contracts.OMM_TOKEN.getKey()))
        };
        Object[] params = new Object[]{
                lendingPoolAddressDetails
        };
        call(Contracts.LENDING_POOL, "setAddresses", params);
    }

    @External
    public void setLendingPoolCoreAddresses() {
        checkOwner();
        AddressDetails[] lendingPoolCoreAddressDetails = new AddressDetails[]{
                new AddressDetails(Contracts.LENDING_POOL.getKey(), addresses.get(Contracts.LENDING_POOL.getKey())),
                new AddressDetails(Contracts.LIQUIDATION_MANAGER.getKey(),
                        addresses.get(Contracts.LIQUIDATION_MANAGER.getKey())),
                new AddressDetails(Contracts.STAKING.getKey(), addresses.get(Contracts.STAKING.getKey())),
                new AddressDetails(Contracts.FEE_PROVIDER.getKey(), addresses.get(Contracts.FEE_PROVIDER.getKey())),
                new AddressDetails(Contracts.DELEGATION.getKey(), addresses.get(Contracts.DELEGATION.getKey())),
                new AddressDetails(Contracts.GOVERNANCE.getKey(), addresses.get(Contracts.GOVERNANCE.getKey())),
                new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress()),
                new AddressDetails(Contracts.OMM_TOKEN.getKey(), addresses.get(Contracts.OMM_TOKEN.getKey()))
        };

        Object[] params = new Object[]{
                lendingPoolCoreAddressDetails
        };
        call(Contracts.LENDING_POOL_CORE, "setAddresses", params);

    }

    @External
    public void setLendingPoolDataProviderAddresses() {
        checkOwner();
        AddressDetails[] lendingPoolDataProviderAddressDetails = new AddressDetails[]{

                new AddressDetails(Contracts.LENDING_POOL.getKey(), addresses.get(Contracts.LENDING_POOL.getKey())),
                new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(),
                        addresses.get(Contracts.LENDING_POOL_CORE.getKey())),
                new AddressDetails(Contracts.LIQUIDATION_MANAGER.getKey(),
                        addresses.get(Contracts.LIQUIDATION_MANAGER.getKey())),
                new AddressDetails(Contracts.STAKING.getKey(), addresses.get(Contracts.STAKING.getKey())),
                new AddressDetails(Contracts.FEE_PROVIDER.getKey(), addresses.get(Contracts.FEE_PROVIDER.getKey())),
                new AddressDetails(Contracts.PRICE_ORACLE.getKey(), addresses.get(Contracts.PRICE_ORACLE.getKey())),
                new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey())),
                new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress())
        };
        Object[] params = new Object[]{
                lendingPoolDataProviderAddressDetails
        };
        call(Contracts.LENDING_POOL_DATA_PROVIDER, "setAddresses", params);
    }


    @External
    public void setLiquidationManagerAddresses() {
        checkOwner();
        AddressDetails[] liquidationManagerAddressDetails = new AddressDetails[]{
                new AddressDetails(Contracts.LENDING_POOL_DATA_PROVIDER.getKey(),
                        addresses.get(Contracts.LENDING_POOL_DATA_PROVIDER.getKey())),
                new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(),
                        addresses.get(Contracts.LENDING_POOL_CORE.getKey())),
                new AddressDetails(Contracts.LENDING_POOL.getKey(), addresses.get(Contracts.LENDING_POOL.getKey())),
                new AddressDetails(Contracts.STAKING.getKey(), addresses.get(Contracts.STAKING.getKey())),
                new AddressDetails(Contracts.FEE_PROVIDER.getKey(), addresses.get(Contracts.FEE_PROVIDER.getKey())),
                new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress()),
                new AddressDetails(Contracts.PRICE_ORACLE.getKey(), addresses.get(Contracts.PRICE_ORACLE.getKey()))
        };

        Object[] params = new Object[]{
                liquidationManagerAddressDetails
        };
        call(Contracts.LIQUIDATION_MANAGER, "setAddresses", params);
    }

    @External
    public void setOmmTokenAddresses() {
        checkOwner();
        AddressDetails[] ommTokenAddressDetails = new AddressDetails[]{
                new AddressDetails(Contracts.LENDING_POOL.getKey(), addresses.get(Contracts.LENDING_POOL.getKey())),
                new AddressDetails(Contracts.DELEGATION.getKey(), addresses.get(Contracts.DELEGATION.getKey())),
                new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey())),
                new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress())
        };

        Object[] params = new Object[]{
                ommTokenAddressDetails
        };
        call(Contracts.OMM_TOKEN, "setAddresses", params);
    }

    @External
    public void setoICXAddresses() {
        checkOwner();
        AddressDetails[] oICXAddressDetails = new AddressDetails[]{
                new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(),
                        addresses.get(Contracts.LENDING_POOL_CORE.getKey())),
                new AddressDetails(Contracts.RESERVE.getKey(), addresses.get(Contracts.sICX.getKey())),
                new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress()),
                new AddressDetails(Contracts.LENDING_POOL.getKey(), addresses.get(Contracts.LENDING_POOL.getKey())),
                new AddressDetails(Contracts.LENDING_POOL_DATA_PROVIDER.getKey(),
                        addresses.get(Contracts.LENDING_POOL_DATA_PROVIDER.getKey())),
                new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey())),
                new AddressDetails(Contracts.LIQUIDATION_MANAGER.getKey(),
                        addresses.get(Contracts.LIQUIDATION_MANAGER.getKey()))
        };

        Object[] params = new Object[]{
                oICXAddressDetails
        };
        call(Contracts.oICX, "setAddresses", params);
    }

    @External
    public void setoUSDsAddresses() {
        checkOwner();
        AddressDetails[] oUSDsAddressDetails = new AddressDetails[]{
                new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(),
                        addresses.get(Contracts.LENDING_POOL_CORE.getKey())),
                new AddressDetails(Contracts.RESERVE.getKey(), addresses.get(Contracts.USDS.getKey())),
                new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress()),
                new AddressDetails(Contracts.LENDING_POOL.getKey(), addresses.get(Contracts.LENDING_POOL.getKey())),
                new AddressDetails(Contracts.LENDING_POOL_DATA_PROVIDER.getKey(),
                        addresses.get(Contracts.LENDING_POOL_DATA_PROVIDER.getKey())),
                new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey())),
                new AddressDetails(Contracts.LIQUIDATION_MANAGER.getKey(),
                        addresses.get(Contracts.LIQUIDATION_MANAGER.getKey()))
        };

        Object[] params = new Object[]{
                oUSDsAddressDetails
        };
        call(Contracts.oUSDs, "setAddresses", params);
    }

    @External
    public void setoIUSDCAddresses() {
        checkOwner();
        AddressDetails[] oIUSDCAddressDetails = new AddressDetails[]{
                new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(),
                        addresses.get(Contracts.LENDING_POOL_CORE.getKey())),
                new AddressDetails(Contracts.RESERVE.getKey(), addresses.get(Contracts.IUSDC.getKey())),
                new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress()),
                new AddressDetails(Contracts.LENDING_POOL.getKey(), addresses.get(Contracts.LENDING_POOL.getKey())),
                new AddressDetails(Contracts.LENDING_POOL_DATA_PROVIDER.getKey(),
                        addresses.get(Contracts.LENDING_POOL_DATA_PROVIDER.getKey())),
                new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey())),
                new AddressDetails(Contracts.LIQUIDATION_MANAGER.getKey(),
                        addresses.get(Contracts.LIQUIDATION_MANAGER.getKey()))
        };

        Object[] params = new Object[]{
                oIUSDCAddressDetails
        };
        call(Contracts.oIUSDC, "setAddresses", params);
    }

    @External
    public void setdICXAddresses() {
        checkOwner();
        AddressDetails[] dICXAddressDetails = new AddressDetails[]{
                new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(),
                        addresses.get(Contracts.LENDING_POOL_CORE.getKey())),
                new AddressDetails(Contracts.RESERVE.getKey(), addresses.get(Contracts.sICX.getKey())),
                new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress()),
                new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey()))
        };

        Object[] params = new Object[]{
                dICXAddressDetails
        };
        call(Contracts.dICX, "setAddresses", params);
    }

    @External
    public void setdUSDsAddresses() {
        checkOwner();
        AddressDetails[] dUSDsAddressDetails = new AddressDetails[]{
                new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(),
                        addresses.get(Contracts.LENDING_POOL_CORE.getKey())),
                new AddressDetails(Contracts.RESERVE.getKey(), addresses.get(Contracts.USDS.getKey())),
                new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress()),
                new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey()))
        };

        Object[] params = new Object[]{
                dUSDsAddressDetails
        };
        call(Contracts.dUSDs, "setAddresses", params);
    }

    @External
    public void setdIUSDCAddresses() {
        checkOwner();
        AddressDetails[] dIUSDCAddressDetails = new AddressDetails[]{
                new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(),
                        addresses.get(Contracts.LENDING_POOL_CORE.getKey())),
                new AddressDetails(Contracts.RESERVE.getKey(), addresses.get(Contracts.IUSDC.getKey())),
                new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress()),
                new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey()))
        };

        Object[] params = new Object[]{
                dIUSDCAddressDetails
        };
        call(Contracts.dIUSDC, "setAddresses", params);
    }

    @External
    public void setDelegationAddresses() {
        checkOwner();
        AddressDetails[] delegationAddressDetails = new AddressDetails[]{
                new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(),
                        addresses.get(Contracts.LENDING_POOL_CORE.getKey())),
                new AddressDetails(Contracts.OMM_TOKEN.getKey(), addresses.get(Contracts.OMM_TOKEN.getKey())),
                new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress())
        };

        Object[] params = new Object[]{
                delegationAddressDetails
        };
        call(Contracts.DELEGATION, "setAddresses", params);
    }

    @External
    public void setRewardAddresses() {
        checkOwner();
        AddressDetails[] rewardAddressDetails = new AddressDetails[]{
                new AddressDetails(Contracts.LENDING_POOL_DATA_PROVIDER.getKey(),
                        addresses.get(Contracts.LENDING_POOL_DATA_PROVIDER.getKey())),
                new AddressDetails(Contracts.OMM_TOKEN.getKey(), addresses.get(Contracts.OMM_TOKEN.getKey())),
                new AddressDetails(Contracts.WORKER_TOKEN.getKey(), addresses.get(Contracts.WORKER_TOKEN.getKey())),
                new AddressDetails(Contracts.DAO_FUND.getKey(), addresses.get(Contracts.DAO_FUND.getKey())),
                new AddressDetails(Contracts.LENDING_POOL.getKey(), addresses.get(Contracts.LENDING_POOL.getKey())),
                new AddressDetails(Contracts.GOVERNANCE.getKey(), addresses.get(Contracts.GOVERNANCE.getKey())),
                new AddressDetails(Contracts.STAKED_LP.getKey(), addresses.get(Contracts.STAKED_LP.getKey())),
                new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress())
        };

        Object[] params = new Object[]{
                rewardAddressDetails
        };
        call(Contracts.REWARDS, "setAddresses", params);
    }

    @External
    public void setPriceOracleAddress() {
        checkOwner();
        AddressDetails[] priceOracleAddresses = new AddressDetails[]{
                new AddressDetails(Contracts.BAND_ORACLE.getKey(), addresses.get(Contracts.BAND_ORACLE.getKey())),
                new AddressDetails(Contracts.DEX.getKey(), addresses.get(Contracts.DEX.getKey())),
                new AddressDetails(Contracts.LENDING_POOL_DATA_PROVIDER.getKey(),
                        addresses.get(Contracts.LENDING_POOL_DATA_PROVIDER.getKey())),
                new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress())
        };

        Object[] params = new Object[]{
                priceOracleAddresses
        };
        call(Contracts.PRICE_ORACLE, "setAddresses", params);
    }

    @External
    public void setStakedLpAddresses() {
        checkOwner();
        AddressDetails[] stakedLpAddresses = new AddressDetails[]{
                new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey())),
                new AddressDetails(Contracts.GOVERNANCE.getKey(), addresses.get(Contracts.GOVERNANCE.getKey())),
                new AddressDetails(Contracts.DEX.getKey(), addresses.get(Contracts.DEX.getKey()))
        };

        Object[] params = new Object[]{
                stakedLpAddresses
        };
        call(Contracts.STAKED_LP, "setAddresses", params);
    }

    @External
    public void setGovernanceAddresses() {
        checkOwner();
        AddressDetails[] governanceAddresses = new AddressDetails[]{
                new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey())),
                new AddressDetails(Contracts.STAKED_LP.getKey(), addresses.get(Contracts.STAKED_LP.getKey())),
                new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(),
                        addresses.get(Contracts.LENDING_POOL_CORE.getKey())),
                new AddressDetails(Contracts.DAO_FUND.getKey(), addresses.get(Contracts.DAO_FUND.getKey())),
                new AddressDetails(Contracts.FEE_PROVIDER.getKey(), addresses.get(Contracts.FEE_PROVIDER.getKey()))
        };

        Object[] params = new Object[]{
                governanceAddresses
        };
        call(Contracts.GOVERNANCE, "setAddresses", params);
    }

    @External
    public void setDaoFundAddresses() {
        checkOwner();
        AddressDetails[] daoFundAddresses = new AddressDetails[]{
                new AddressDetails(Contracts.GOVERNANCE.getKey(), addresses.get(Contracts.GOVERNANCE.getKey())),
                new AddressDetails(Contracts.OMM_TOKEN.getKey(), addresses.get(Contracts.OMM_TOKEN.getKey()))
        };

        Object[] params = new Object[]{
                daoFundAddresses
        };
        call(Contracts.DAO_FUND, "setAddresses", params);
    }

    @External
    public void setFeeProviderAddresses() {
        checkOwner();
        AddressDetails[] feeProviderAddresses = new AddressDetails[]{
                new AddressDetails(Contracts.GOVERNANCE.getKey(), addresses.get(Contracts.GOVERNANCE.getKey()))
        };

        Object[] params = new Object[]{
                feeProviderAddresses
        };
        call(Contracts.FEE_PROVIDER, "setAddresses", params);
    }


    private void addReserve(ReserveAddressDetails reserveAddressDetails, Boolean overwrite) {
        String _reserveName = reserveAddressDetails.reserveName;
        Address reserve = addresses.get(reserveAddressDetails.reserveName);
        boolean _is_reserve_exists = reserve != null || _reserves.contains(_reserveName);
        if (_is_reserve_exists && !overwrite) {
            throw AddressManagerException.unknown(
                    "reserve name " + reserveAddressDetails.reserveName + " already exits.");
        }
        addresses.set(reserveAddressDetails.reserveName, reserveAddressDetails.reserveAddress);
        _reserves.add(reserveAddressDetails.reserveName);
    }

    private void addOToken(ReserveAddressDetails oTokenDetails, Boolean overwrite) {
        String _oTokenName = oTokenDetails.oTokenName;
        Address oTokenAddress = addresses.get(oTokenDetails.oTokenName);
        boolean _is_oToken_exists = oTokenAddress != null || _oTokens.contains(_oTokenName);
        if (_is_oToken_exists && !overwrite) {
            throw AddressManagerException.unknown("oToken name " + oTokenDetails.oTokenName + " already exits.");
        }
        addresses.set(oTokenDetails.oTokenName, oTokenDetails.oTokenAddress);
        _oTokens.add(oTokenDetails.oTokenName);
    }

    private void addDToken(ReserveAddressDetails dTokenDetails, Boolean overwrite) {
        String _dTokenName = dTokenDetails.dTokenName;
        Address dTokenAddress = addresses.get(dTokenDetails.dTokenName);
        boolean _is_dToken_exists = dTokenAddress != null || _dTokens.contains(_dTokenName);
        if (_is_dToken_exists && !overwrite) {
            throw AddressManagerException.unknown("dToken name " + dTokenDetails.dTokenName + " already exits.");
        }
        addresses.set(dTokenDetails.dTokenName, dTokenDetails.dTokenAddress);
        _dTokens.add(dTokenDetails.dTokenName);
    }

    protected void checkOwner() {
        if (!Context.getOwner().equals(Context.getCaller())) {
            throw AddressManagerException.notOwner();
        }
    }

    public void call(Contracts contract, String method, Object... params) {
        Context.call(getAddress(contract.getKey()), method, params);
    }

    public void call(Address contract, String method, Object... params) {
        Context.call(contract, method, params);
    }

}
