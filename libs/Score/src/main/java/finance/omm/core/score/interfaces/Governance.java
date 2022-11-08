package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.AssetConfig;
import finance.omm.libs.structs.TypeWeightStruct;
import finance.omm.libs.structs.WeightStruct;
import finance.omm.libs.structs.governance.ReserveAttributes;
import finance.omm.libs.structs.governance.ReserveConstant;
import foundation.icon.score.client.ScoreInterface;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.annotation.Optional;

@ScoreInterface(suffix = "Client")
public interface Governance extends AddressProvider {

    String name();

    void setReserveActiveStatus(Address _reserve, boolean _status);

    void setReserveFreezeStatus(Address _reserve, boolean _status);

    void setReserveConstants(ReserveConstant[] _constants);

    void initializeReserve(ReserveAttributes _reserve);

    void updateBaseLTVasCollateral(Address _reserve, BigInteger _baseLtv);

    void updateLiquidationThreshold(Address _reserve, BigInteger _liquidationThreshold);

    void updateBorrowThreshold(Address _reserve, BigInteger _borrowThreshold);

    void updateLiquidationBonus(Address _reserve, BigInteger _liquidationBonus);

    void updateBorrowingEnabled(Address _reserve, boolean _borrowingEnabled);

    void updateUsageAsCollateralEnabled(Address _reserve, boolean _usageAsCollateralEnabled);

    void enableRewardClaim();

    void disableRewardClaim();

    void addPools(AssetConfig[] _assetConfigs);

    void addPool(AssetConfig _assetConfig);

    void removePool(Address _asset);

    void transferOmmToDaoFund(BigInteger _value);

    void transferOmmFromDaoFund(BigInteger _value, Address _address);

    void transferFundFromFeeProvider(Address _token, BigInteger _value, Address _to);


    Map<String, BigInteger> getVotersCount(int vote_index);

    void setVoteDuration(BigInteger duration);

    BigInteger getVoteDuration();

    void setQuorum(BigInteger quorum);

    BigInteger getQuorum();

    void setVoteDefinitionFee(BigInteger fee);

    BigInteger getVoteDefinitionFee();


    void setVoteDefinitionCriteria(BigInteger percentage);

    BigInteger getBoostedOmmVoteDefinitionCriterion();

    void cancelVote(int vote_index);

    void addAllowedMethods(Address contract, String[] method);

    void removeAllowedMethods(Address contract, String[] method);

    String[] getSupportedMethodsOfContract(Address contract);

    List<Address> getSupportedContracts();

    void tryExecuteTransactions(String transactions);

    void executeTransactions(String transactions);

    BigInteger maxActions();

    int getProposalCount();


    List getProposals(int batch_size, int offset);

    void updateVoteForum(int vote_index, String forum);

    void updateTotalVotingWeight(int vote_index, BigInteger weight);

    void castVote(int vote_index, boolean vote);


    void execute_proposal(int vote_index);


    void setProposalStatus(int vote_index, String _status);


    int getVoteIndex(String _name);

    Map<String, ?> checkVote(int _vote_index);


    Map<String, ?> getVotesOfUser(int vote_index, Address user);


    BigInteger myVotingWeight(Address _address, BigInteger _day);


    void tokenFallback(Address _from, BigInteger _value, byte[] _data);

    void enableHandleActions();

    void disableHandleActions();

    void setAssetWeight(String type, WeightStruct[] weights, @Optional BigInteger timestamp);

    void setTypeWeight(TypeWeightStruct[] weights, @Optional BigInteger timestamp);

    void addType(String key, boolean isPlatformRecipient);

    void addAsset(String type, String name, Address address, @Optional BigInteger poolID);

    void setMinimumLockingAmount(BigInteger value);

    void addContractToWhitelist(Address address);

    void removeContractFromWhitelist(Address address);

}
