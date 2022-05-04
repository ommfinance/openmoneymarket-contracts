package finance.omm.score.core.addreess.manager;

import finance.omm.core.score.interfaces.AddressManager;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.libs.structs.ReserveAddressDetails;
import finance.omm.score.core.addreess.manager.exception.AddressManagerException;
import finance.omm.utils.db.EnumerableSet;
import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.External;
import scorex.util.HashMap;

import java.util.Map;

public class AddressManagerImpl  implements AddressManager {


    public static final String TAG = "Address Provider"; // TAG NAME SAME AS PYTHON

    private static final String RESERVES = "reserves_es_entries";
    private static final String O_TOKENS = "o_tokens_es_entries";
    private static final String D_TOKENS = "d_tokens_es_entries";

    private final DictDB<String,Address> addresses = Context.newDictDB("address",Address.class);
    private final EnumerableSet<String> _reserves = new EnumerableSet<>(RESERVES,String.class);
    private final EnumerableSet<String> _dTokens = new EnumerableSet<>(D_TOKENS,String.class);
    private final EnumerableSet<String> _oTokens = new EnumerableSet<>(O_TOKENS,String.class);

    public AddressManagerImpl(){}

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
    }

    @External
    public void addReserveAddress(ReserveAddressDetails _reserveAddressDetails, boolean _overwrite) {
        checkOwner();

        addReserve(_reserveAddressDetails,_overwrite);
        addOToken(_reserveAddressDetails,_overwrite);
        addDToken(_reserveAddressDetails,_overwrite);
    }

    // duplicate code extracted
//    private Map<String, Address> getAllAddressMap(EnumerableSet<String> token) {
//        Map<String, Address> reserves = new HashMap<>();
//        for (int i = 0; i < token.length(); i++) {
//            String key = token.at(i); // reserve name
//            Address address = addresses.get(key); // chekced name in address db
//            if (address !=null){
//                reserves.put(key,address);
//            }
//        }
//        return reserves;
//    }

    private Map<String,Address> getAllReserveAddresses(){
        Map<String, Address> reserves = new HashMap<>();
        for (int i = 0; i < _reserves.length(); i++) {
            String key = _reserves.at(i); // reserve name
            Address address = addresses.get(key); // chekced name in address db
            if (address !=null){
                reserves.put(key,address);
            }
        }
        return reserves;
    }

    private Map<String,Address> getAllOTokenAddresses(){
        Map<String, Address> oTokens = new HashMap<>();
        for (int i = 0; i < _oTokens.length(); i++) {
            String key = _oTokens.at(i); // reserve name
            Address address = addresses.get(key); // chekced name in address db
            if (address !=null){
                oTokens.put(key,address);
            }
        }
        return oTokens;
    }

    private Map<String,Address> getAllDTokenAddresses(){
        Map<String, Address> dTokens = new HashMap<>();
        for (int i = 0; i < _dTokens.length(); i++) {
            String key = _dTokens.at(i); // reserve name
            Address address = addresses.get(key); // chekced name in address db
            if (address !=null){
                dTokens.put(key,address);
            }
        }
        return dTokens;
    }

//    private Map<String, Address> getAllReserveAddresses(){
//        return getAllAddressMap(_reserves);
//    }
//
//    private Map<String,Address> getAllOTokenAddresses(){
//        return getAllAddressMap(_oTokens);
//    }
//
//    private Map<String,Address> getAllDTokenAddresses(){
//        return getAllAddressMap(_dTokens);
//    }


    @External
    public void setAddresses(AddressDetails[] _addressDetails) {
        checkOwner();
        for (AddressDetails addressDetails :_addressDetails ){
            addresses.set(addressDetails.name,addressDetails.address);
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
        Map<String,Address> system = Map.ofEntries(
                Map.entry("LendingPool",getAddress(Contracts.LENDING_POOL.getKey())),
                Map.entry("LendingPoolCore", getAddress(Contracts.LENDING_POOL_CORE.getKey())),
                Map.entry("LendingPoolDataProvider",getAddress(Contracts.LENDING_POOL_DATA_PROVIDER.getKey())),
                Map.entry("Staking",getAddress(Contracts.STAKING.getKey())),
                Map.entry( "Governance",getAddress(Contracts.GOVERNANCE.getKey())),
                Map.entry("Delegation",getAddress(Contracts.DELEGATION.getKey())),
                Map.entry("OmmToken",getAddress(Contracts.OMM_TOKEN.getKey())),
                Map.entry("Rewards",getAddress(Contracts.REWARDS.getKey())),
                Map.entry("PriceOracle",getAddress(Contracts.PRICE_ORACLE.getKey())),
                Map.entry("StakedLp",getAddress(Contracts.STAKED_LP.getKey())),
                Map.entry("DEX",getAddress(Contracts.DEX.getKey()))
        );
        return Map.of(
                "collateral",getAllReserveAddresses(),
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
        if (score==null ){
            Context.revert(TAG + ": score name " + _to + " not matched.");
        }

        AddressDetails [] addressDetails =new AddressDetails[]{ new AddressDetails(_key,_value)};
        Object[] params = new Object[]{
                addressDetails
        };
        call(score,"setAddresses",params);
    }

    @External
    public void addAddressToScore(String _to, String[] _names) {
        checkOwner();
        Address score = addresses.get(_to);
        if (score == null){
            Context.revert(TAG + ": score name " + _to + " not matched.");
        }
        int size = _names.length;
        AddressDetails[] addressDetails = new AddressDetails[size];

        for (int i = 0; i < size ;i ++) {
            String name = _names[i];
            Address address = addresses.get(name);
            if (address ==null){
                Context.revert(TAG + ": wrong score name in the list.");
            }
            addressDetails[i] = new AddressDetails(name,address);
        }
        Object[] params = new Object[]{
                addressDetails
        };
        call(score,"setAddresses",params);
    }

    @External
    public void setLendingPoolAddresses() {
        checkOwner();
        AddressDetails[] lendingPoolAddressDetails = new AddressDetails[11];
        lendingPoolAddressDetails[0] = new AddressDetails(Contracts.LIQUIDATION_MANAGER.getKey(), addresses.get(Contracts.LIQUIDATION_MANAGER.getKey()));
        lendingPoolAddressDetails[1] = new AddressDetails(Contracts.sICX.getKey(), addresses.get(Contracts.sICX.getKey()));
        lendingPoolAddressDetails[2] = new AddressDetails(Contracts.oICX.getKey(), addresses.get(Contracts.oICX.getKey()));
        lendingPoolAddressDetails[3] = new AddressDetails(Contracts.STAKING.getKey(), addresses.get(Contracts.STAKING.getKey()));
        lendingPoolAddressDetails[4] = new AddressDetails(Contracts.LENDING_POOL_DATA_PROVIDER.getKey(), addresses.get(Contracts.LENDING_POOL_DATA_PROVIDER.getKey()));
        lendingPoolAddressDetails[5] = new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(), addresses.get(Contracts.LENDING_POOL_CORE.getKey()));
        lendingPoolAddressDetails[6] = new AddressDetails(Contracts.FEE_PROVIDER.getKey(), addresses.get(Contracts.FEE_PROVIDER.getKey()));
        lendingPoolAddressDetails[7] = new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey()));
        lendingPoolAddressDetails[8] = new AddressDetails(Contracts.BRIDGE_O_TOKEN.getKey(), addresses.get(Contracts.BRIDGE_O_TOKEN.getKey()));
        lendingPoolAddressDetails[9] = new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress());
        lendingPoolAddressDetails[10] = new AddressDetails(Contracts.OMM_TOKEN.getKey(), addresses.get(Contracts.OMM_TOKEN.getKey()));
        Object[] params = new Object[]{
                lendingPoolAddressDetails
        };
        call(Contracts.LENDING_POOL,"setAddresses",params);
    }

    @External
    public void setLendingPoolCoreAddresses() {
        checkOwner();
        AddressDetails[] lendingPoolCoreAddressDetails = new AddressDetails[8];
        lendingPoolCoreAddressDetails[0] = new AddressDetails(Contracts.LENDING_POOL.getKey(), addresses.get(Contracts.LENDING_POOL.getKey()));
        lendingPoolCoreAddressDetails[1] = new AddressDetails(Contracts.LIQUIDATION_MANAGER.getKey(), addresses.get(Contracts.LIQUIDATION_MANAGER.getKey()));
        lendingPoolCoreAddressDetails[2] = new AddressDetails(Contracts.STAKING.getKey(), addresses.get(Contracts.STAKING.getKey()));
        lendingPoolCoreAddressDetails[3] = new AddressDetails(Contracts.FEE_PROVIDER.getKey(), addresses.get(Contracts.FEE_PROVIDER.getKey()));
        lendingPoolCoreAddressDetails[4] = new AddressDetails(Contracts.DELEGATION.getKey(), addresses.get(Contracts.DELEGATION.getKey()));
        lendingPoolCoreAddressDetails[5] = new AddressDetails(Contracts.GOVERNANCE.getKey(), addresses.get(Contracts.GOVERNANCE.getKey()));
        lendingPoolCoreAddressDetails[6] = new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress());
        lendingPoolCoreAddressDetails[7] = new AddressDetails(Contracts.OMM_TOKEN.getKey(), addresses.get(Contracts.OMM_TOKEN.getKey()));
        Object[] params = new Object[]{
                lendingPoolCoreAddressDetails
        };
        call(Contracts.LENDING_POOL_CORE,"setAddresses",params);

    }

    @External
    public void setLendingPoolDataProviderAddresses() {
        checkOwner();
        AddressDetails[] lendingPoolDataProviderAddressDetails = new AddressDetails[8];
        lendingPoolDataProviderAddressDetails[0] = new AddressDetails(Contracts.LENDING_POOL.getKey(), addresses.get(Contracts.LENDING_POOL.getKey()));
        lendingPoolDataProviderAddressDetails[1] = new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(), addresses.get(Contracts.LENDING_POOL_CORE.getKey()));
        lendingPoolDataProviderAddressDetails[2] = new AddressDetails(Contracts.LIQUIDATION_MANAGER.getKey(), addresses.get(Contracts.LIQUIDATION_MANAGER.getKey()));
        lendingPoolDataProviderAddressDetails[3] = new AddressDetails(Contracts.STAKING.getKey(), addresses.get(Contracts.STAKING.getKey()));
        lendingPoolDataProviderAddressDetails[4] = new AddressDetails(Contracts.FEE_PROVIDER.getKey(), addresses.get(Contracts.FEE_PROVIDER.getKey()));
        lendingPoolDataProviderAddressDetails[5] = new AddressDetails(Contracts.PRICE_ORACLE.getKey(), addresses.get(Contracts.PRICE_ORACLE.getKey()));
        lendingPoolDataProviderAddressDetails[6] = new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey()));
        lendingPoolDataProviderAddressDetails[7] = new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress());

        Object[] params = new Object[]{
                lendingPoolDataProviderAddressDetails
        };
        call(Contracts.LENDING_POOL_DATA_PROVIDER,"setAddresses",params);


    }


    @External
    public void setLiquidationManagerAddresses() {
        checkOwner();
        AddressDetails[] liquidationManagerAddressDetails = new AddressDetails[7];
        liquidationManagerAddressDetails[0] = new AddressDetails(Contracts.LENDING_POOL_DATA_PROVIDER.getKey(), addresses.get(Contracts.LENDING_POOL_DATA_PROVIDER.getKey()));
        liquidationManagerAddressDetails[1] = new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(), addresses.get(Contracts.LENDING_POOL_CORE.getKey()));
        liquidationManagerAddressDetails[2] = new AddressDetails(Contracts.LENDING_POOL.getKey(), addresses.get(Contracts.LENDING_POOL.getKey()));
        liquidationManagerAddressDetails[3] = new AddressDetails(Contracts.STAKING.getKey(), addresses.get(Contracts.STAKING.getKey()));
        liquidationManagerAddressDetails[4] = new AddressDetails(Contracts.FEE_PROVIDER.getKey(), addresses.get(Contracts.FEE_PROVIDER.getKey()));
        liquidationManagerAddressDetails[5] = new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(),Context.getAddress() );
        liquidationManagerAddressDetails[6] = new AddressDetails(Contracts.PRICE_ORACLE.getKey(), addresses.get(Contracts.PRICE_ORACLE.getKey()));


        Object[] params = new Object[]{
                liquidationManagerAddressDetails
        };
        call(Contracts.LIQUIDATION_MANAGER,"setAddresses",params);
    }

    @External
    public void setOmmTokenAddresses() {
        checkOwner();
        AddressDetails[] ommTokenAddressDetails = new AddressDetails[4];
        ommTokenAddressDetails[0] = new AddressDetails(Contracts.LENDING_POOL.getKey(),addresses.get(Contracts.LENDING_POOL.getKey()));
        ommTokenAddressDetails[1] = new AddressDetails(Contracts.DELEGATION.getKey(),addresses.get(Contracts.DELEGATION.getKey()));
        ommTokenAddressDetails[2] = new AddressDetails(Contracts.REWARDS.getKey(),addresses.get(Contracts.REWARDS.getKey()));
        ommTokenAddressDetails[3] = new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(),Context.getAddress());

        Object[] params = new Object[]{
                ommTokenAddressDetails
        };
        call(Contracts.OMM_TOKEN,"setAddresses",params);
    }

    @External
    public void setoICXAddresses() {
        checkOwner();
        AddressDetails[] oICXAddressDetails = new AddressDetails[7];
        oICXAddressDetails[0] = new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(), addresses.get(Contracts.LENDING_POOL_CORE.getKey()));
        oICXAddressDetails[1] = new AddressDetails(Contracts.RESERVE.getKey(), addresses.get(Contracts.sICX.getKey()));
        oICXAddressDetails[2] = new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(),Context.getAddress() );
        oICXAddressDetails[3] = new AddressDetails(Contracts.LENDING_POOL.getKey(), addresses.get(Contracts.LENDING_POOL.getKey()));
        oICXAddressDetails[4] = new AddressDetails(Contracts.LENDING_POOL_DATA_PROVIDER.getKey(), addresses.get(Contracts.LENDING_POOL_DATA_PROVIDER.getKey()));
        oICXAddressDetails[5] = new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey()));
        oICXAddressDetails[6] = new AddressDetails(Contracts.LIQUIDATION_MANAGER.getKey(), addresses.get(Contracts.LIQUIDATION_MANAGER.getKey()));
        Object[] params = new Object[]{
                oICXAddressDetails
        };
        call(Contracts.oICX,"setAddresses",params);
    }

    @External
    public void setoUSDsAddresses() {
        checkOwner();
        AddressDetails[] oUSDsAddressDetails = new AddressDetails[7];
        oUSDsAddressDetails[0] = new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(), addresses.get(Contracts.LENDING_POOL_CORE.getKey()));
        oUSDsAddressDetails[1] = new AddressDetails(Contracts.RESERVE.getKey(), addresses.get(Contracts.USDS.getKey()));
        oUSDsAddressDetails[2] = new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress());
        oUSDsAddressDetails[3] = new AddressDetails(Contracts.LENDING_POOL.getKey(), addresses.get(Contracts.LENDING_POOL.getKey()));
        oUSDsAddressDetails[4] = new AddressDetails(Contracts.LENDING_POOL_DATA_PROVIDER.getKey(), addresses.get(Contracts.LENDING_POOL_DATA_PROVIDER.getKey()));
        oUSDsAddressDetails[5] = new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey()));
        oUSDsAddressDetails[6] = new AddressDetails(Contracts.LIQUIDATION_MANAGER.getKey(), addresses.get(Contracts.LIQUIDATION_MANAGER.getKey()));

        Object[] params = new Object[]{
                oUSDsAddressDetails
        };
        call(Contracts.oUSDs,"setAddresses",params);
    }

    @External
    public void setoIUSDCAddresses() {
        checkOwner();
        AddressDetails[] oIUSDCAddressDetails = new AddressDetails[7];
        oIUSDCAddressDetails[0] = new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(), addresses.get(Contracts.LENDING_POOL_CORE.getKey()));
        oIUSDCAddressDetails[1] = new AddressDetails(Contracts.RESERVE.getKey(), addresses.get(Contracts.IUSDC.getKey()));
        oIUSDCAddressDetails[2] = new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress());
        oIUSDCAddressDetails[3] = new AddressDetails(Contracts.LENDING_POOL.getKey(), addresses.get(Contracts.LENDING_POOL.getKey()));
        oIUSDCAddressDetails[4] = new AddressDetails(Contracts.LENDING_POOL_DATA_PROVIDER.getKey(), addresses.get(Contracts.LENDING_POOL_DATA_PROVIDER.getKey()));
        oIUSDCAddressDetails[5] = new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey()));
        oIUSDCAddressDetails[6] = new AddressDetails(Contracts.LIQUIDATION_MANAGER.getKey(), addresses.get(Contracts.LIQUIDATION_MANAGER.getKey()));

        Object[] params = new Object[]{
                oIUSDCAddressDetails
        };
        call(Contracts.oIUSDC,"setAddresses",params);
    }

    @External
    public void setdICXAddresses() {
        checkOwner();
        AddressDetails[] dICXAddressDetails = new AddressDetails[4];
        dICXAddressDetails[0] = new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(), addresses.get(Contracts.LENDING_POOL_CORE.getKey()));
        dICXAddressDetails[1] = new AddressDetails(Contracts.RESERVE.getKey(), addresses.get(Contracts.sICX.getKey()));
        dICXAddressDetails[2] = new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress());
        dICXAddressDetails[3] = new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey()));
        Object[] params = new Object[]{
                dICXAddressDetails
        };
        call(Contracts.dICX,"setAddresses",params);
    }

    @External
    public void setdUSDsAddresses() {
        checkOwner();
        AddressDetails[] dUSDsAddressDetails = new AddressDetails[4];
        dUSDsAddressDetails[0] = new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(), addresses.get(Contracts.LENDING_POOL_CORE.getKey()));
        dUSDsAddressDetails[1] = new AddressDetails(Contracts.RESERVE.getKey(), addresses.get(Contracts.USDS.getKey()));
        dUSDsAddressDetails[2] = new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress());
        dUSDsAddressDetails[3] = new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey()));

        Object[] params = new Object[]{
                dUSDsAddressDetails
        };
        call(Contracts.dUSDs,"setAddresses",params);
    }

    @External
    public void setdIUSDCAddresses() {
        checkOwner();
        AddressDetails[] dIUSDCAddressDetails = new AddressDetails[4];
        dIUSDCAddressDetails[0] = new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(), addresses.get(Contracts.LENDING_POOL_CORE.getKey()));
        dIUSDCAddressDetails[1] = new AddressDetails(Contracts.RESERVE.getKey(), addresses.get(Contracts.IUSDC.getKey()));
        dIUSDCAddressDetails[2] = new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress());
        dIUSDCAddressDetails[3] = new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey()));


        Object[] params = new Object[]{
                dIUSDCAddressDetails
        };
        call(Contracts.dIUSDC,"setAddresses",params);
    }

    @External
    public void setDelegationAddresses() {
        checkOwner();
        AddressDetails[] delegationAddressDetails = new AddressDetails[3];
        delegationAddressDetails[0] = new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(), addresses.get(Contracts.LENDING_POOL_CORE.getKey()));
        delegationAddressDetails[1] = new AddressDetails(Contracts.OMM_TOKEN.getKey(), addresses.get(Contracts.OMM_TOKEN.getKey()));
        delegationAddressDetails[2] = new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress());
        Object[] params = new Object[]{
                delegationAddressDetails
        };
        call(Contracts.DELEGATION,"setAddresses",params);
    }

    @External
    public void setRewardAddresses() {
        checkOwner();
        AddressDetails[] rewardAddressDetails = new AddressDetails[8];
        rewardAddressDetails[0] = new AddressDetails(Contracts.LENDING_POOL_DATA_PROVIDER.getKey(), addresses.get(Contracts.LENDING_POOL_DATA_PROVIDER.getKey()));
        rewardAddressDetails[1] = new AddressDetails(Contracts.OMM_TOKEN.getKey(), addresses.get(Contracts.OMM_TOKEN.getKey()));
        rewardAddressDetails[2] = new AddressDetails(Contracts.WORKER_TOKEN.getKey(), addresses.get(Contracts.WORKER_TOKEN.getKey()));
        rewardAddressDetails[3] = new AddressDetails(Contracts.DAO_FUND.getKey(), addresses.get(Contracts.DAO_FUND.getKey()));
        rewardAddressDetails[4] = new AddressDetails(Contracts.LENDING_POOL.getKey(), addresses.get(Contracts.LENDING_POOL.getKey()));
        rewardAddressDetails[5] = new AddressDetails(Contracts.GOVERNANCE.getKey(), addresses.get(Contracts.GOVERNANCE.getKey()));
        rewardAddressDetails[6] = new AddressDetails(Contracts.STAKED_LP.getKey(), addresses.get(Contracts.STAKED_LP.getKey()));
        rewardAddressDetails[7] = new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress());
        Object[] params = new Object[]{
                rewardAddressDetails
        };
        call(Contracts.REWARDS,"setAddresses",params);
    }

    @External
    public void setPriceOracleAddress() {
        checkOwner();
        AddressDetails[] priceOracleAddresses = new AddressDetails[4];
        priceOracleAddresses[0] = new AddressDetails(Contracts.BAND_ORACLE.getKey(), addresses.get(Contracts.BAND_ORACLE.getKey()));
        priceOracleAddresses[1] = new AddressDetails(Contracts.DEX.getKey(), addresses.get(Contracts.DEX.getKey()));
        priceOracleAddresses[2] = new AddressDetails(Contracts.LENDING_POOL_DATA_PROVIDER.getKey(), addresses.get(Contracts.LENDING_POOL_DATA_PROVIDER.getKey()));
        priceOracleAddresses[3] = new AddressDetails(Contracts.ADDRESS_PROVIDER.getKey(), Context.getAddress());

        Object[] params = new Object[]{
                priceOracleAddresses
        };
        call(Contracts.PRICE_ORACLE,"setAddresses",params);
    }

    @External
    public void setStakedLpAddresses() {
        checkOwner();
        AddressDetails[] stakedLpAddresses = new AddressDetails[3];
        stakedLpAddresses[0] = new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey()));
        stakedLpAddresses[1] = new AddressDetails(Contracts.GOVERNANCE.getKey(), addresses.get(Contracts.GOVERNANCE.getKey()));
        stakedLpAddresses[2] = new AddressDetails(Contracts.DEX.getKey(), addresses.get(Contracts.DEX.getKey()));

        Object[] params = new Object[]{
                stakedLpAddresses
        };
        call(Contracts.STAKED_LP,"setAddresses",params);
    }

    @External
    public void setGovernanceAddresses() {
        checkOwner();
        AddressDetails[] governanceAddresses = new AddressDetails[5];
        governanceAddresses[0] = new AddressDetails(Contracts.REWARDS.getKey(), addresses.get(Contracts.REWARDS.getKey()));
        governanceAddresses[1] = new AddressDetails(Contracts.STAKED_LP.getKey(), addresses.get(Contracts.STAKED_LP.getKey()));
        governanceAddresses[2] = new AddressDetails(Contracts.LENDING_POOL_CORE.getKey(), addresses.get(Contracts.LENDING_POOL_CORE.getKey()));
        governanceAddresses[3] = new AddressDetails(Contracts.DAO_FUND.getKey(), addresses.get(Contracts.DAO_FUND.getKey()));
        governanceAddresses[4] = new AddressDetails(Contracts.FEE_PROVIDER.getKey(), addresses.get(Contracts.FEE_PROVIDER.getKey()));

        Object[] params = new Object[]{
                governanceAddresses
        };
        call(Contracts.GOVERNANCE,"setAddresses",params);
    }

    @External
    public void setDaoFundAddresses() {
        checkOwner();
        AddressDetails[] daoFundAddresses = new AddressDetails[2];
        daoFundAddresses[0] = new AddressDetails(Contracts.GOVERNANCE.getKey(), addresses.get(Contracts.GOVERNANCE.getKey()));
        daoFundAddresses[1] = new AddressDetails(Contracts.OMM_TOKEN.getKey(), addresses.get(Contracts.OMM_TOKEN.getKey()));

        Object[] params = new Object[]{
                daoFundAddresses
        };
        call(Contracts.DAO_FUND,"setAddresses",params);
    }

    @External
    public void setFeeProviderAddresses() {
        checkOwner();
        AddressDetails[] feeProviderAddresses = new AddressDetails[1];
        feeProviderAddresses[0] = new AddressDetails(Contracts.GOVERNANCE.getKey(), addresses.get(Contracts.GOVERNANCE.getKey()));

        Object[] params = new Object[]{
                feeProviderAddresses
        };
        call(Contracts.FEE_PROVIDER,"setAddresses",params);
    }


    private void addReserve(ReserveAddressDetails reserveAddressDetails, Boolean overwrite){
        boolean isReserveExists = Boolean.FALSE;
        Address reserve= addresses.get(reserveAddressDetails.reserveName);
        // reserve ko address ni compare garyo
        if (reserve !=null && _reserves.contains(reserveAddressDetails.reserveName) &&
                reserve.equals(reserveAddressDetails.reserveAddress)){
            isReserveExists = true;
        }
        if (isReserveExists && !overwrite){
            Context.revert("reserve name " + reserveAddressDetails.reserveName + " already exits.");
        }
        addresses.set(reserveAddressDetails.reserveName, reserveAddressDetails.reserveAddress);
        _reserves.add(reserveAddressDetails.reserveName);
    }

    private void addOToken(ReserveAddressDetails oTokenDetails, Boolean overwrite){
        boolean isoTokenExists = Boolean.FALSE;
        Address oTokenAddress = addresses.get(oTokenDetails.oTokenName);
        if (oTokenAddress !=null && _oTokens.contains(oTokenDetails.oTokenName) &&
                oTokenAddress.equals(oTokenDetails.oTokenAddress)){
            isoTokenExists = true;
        }
        if (isoTokenExists && !overwrite){
            Context.revert("oToken name "+ oTokenDetails.oTokenName + " already exists.");
        }

        addresses.set(oTokenDetails.oTokenName,oTokenDetails.oTokenAddress);
        _oTokens.add(oTokenDetails.oTokenName);
    }

    private void addDToken(ReserveAddressDetails dTokenDetails, Boolean overwrite){
        boolean isdTokenExists = Boolean.FALSE;
        Address dTokenAddress = addresses.get(dTokenDetails.dTokenName);
        if (dTokenAddress != null && _dTokens.contains(dTokenDetails.dTokenName) &&
                dTokenAddress.equals(dTokenDetails.dTokenAddress)){
            isdTokenExists = true;
        }
        if (isdTokenExists && !overwrite){
            Context.revert("dToken name "+ dTokenDetails.dTokenName + " already exists.");
        }
        addresses.set(dTokenDetails.dTokenName,dTokenDetails.dTokenAddress);
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
