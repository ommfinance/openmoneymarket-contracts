package finance.omm.score.core.test;
import finance.omm.core.score.interfaces.RewardWeightController;
import finance.omm.libs.structs.AssetConfig;
import finance.omm.libs.structs.TypeWeightStruct;
import finance.omm.libs.structs.WeightStruct;
import finance.omm.libs.structs.governance.ReserveAttributes;
import finance.omm.libs.structs.governance.ReserveConstant;
import foundation.icon.jsonrpc.Address;

import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.configs.GovernanceConfig;
import finance.omm.score.core.governance.exception.GovernanceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GovernanceIntegrationTest implements ScoreIntegrationTest {

    private static OMMClient ommClient;
    private static OMMClient testClient;

    @BeforeAll
    static void setup() throws Exception {
        OMM omm = new OMM("governance/scores.json");
        System.out.println("a"+omm.getAddresses());
        omm.setupOMM();
        System.out.println("b"+omm.getAddresses());
        Config config = new GovernanceConfig(omm.getAddresses());
        System.out.println("c"+omm.getAddresses());
        omm.runConfig(config);
        System.out.println("d"+omm.getAddresses());
        ommClient = omm.defaultClient();
        testClient = omm.testClient();

    }

    List<score.Address> addresses = new ArrayList<>(){{
        add(Faker.address(Address.Type.EOA));
        add(Faker.address(Address.Type.EOA));
        add(Faker.address(Address.Type.CONTRACT));
    }};

    @Test
    void setReserveActiveStatus(){
//        ommClient.governance.getAddressProvider();
//        testClient.governance.setReserveActiveStatus(Address.fromString("cx1087d7d516916a493463280f97dd1bba6983fd96"),true);

        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.setReserveActiveStatus(addresses.get(0),false), null);


    }
    @Test
    void setReserveFreezeStatus(){
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.setReserveFreezeStatus(addresses.get(0),false), null);

    }
    @Test
    void setReserveConstants(){
        ReserveConstant constant = new ReserveConstant();
        constant.reserve = addresses.get(0);
        constant.optimalUtilizationRate = BigInteger.ONE;
        constant.baseBorrowRate = BigInteger.TWO;
        constant.slopeRate1 = BigInteger.ONE;
        constant.slopeRate2 = BigInteger.TEN;
        ReserveConstant[] constants = new ReserveConstant[]{
                constant
        };

        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.setReserveConstants(constants), null);

    }
    @Test
    void initializeReserve(){
        ReserveAttributes reserve = new ReserveAttributes();
        BigInteger baseltv = BigInteger.TEN;
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.initializeReserve(reserve), null);

    }
    @Test
    void updateBaseLTVasCollateral(){
        Address reserve = (Address) addresses.get(0);
        BigInteger baseltv = BigInteger.TEN;
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.updateBaseLTVasCollateral(reserve,baseltv), null);
    }
    @Test
    void updateLiquidationThreshold(){
        Address reserve = (Address) addresses.get(0);
        BigInteger liquidationThreshold = BigInteger.TEN;
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.updateLiquidationThreshold(reserve,liquidationThreshold), null);
    }
    @Test
    void updateBorrowThreshold(){
        Address reserve = (Address) addresses.get(0);
        BigInteger borrowThreshold = BigInteger.TEN;
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.updateBorrowThreshold(reserve,borrowThreshold), null);
    }
    @Test
    void updateLiquidationBonus(){
        Address reserve = (Address) addresses.get(0);
        BigInteger liquidationBonus = BigInteger.TEN;
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.updateLiquidationBonus(reserve,liquidationBonus), null);
    }
    @Test
    void updateBorrowingEnabled(){
        Address reserve = (Address) addresses.get(0);
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.updateBorrowingEnabled(reserve,true), null);
    }

    @Test
    void updateUsageAsCollateralEnabled(){
        Address reserve = (Address) addresses.get(0);
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.updateUsageAsCollateralEnabled(reserve,true), null);
    }

    @Test
    void enableRewardClaim(){
        Address reserve = (Address) addresses.get(0);
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.enableRewardClaim(), null);
    }

    @Test
    void disableRewardClaim(){
        Address reserve = (Address) addresses.get(0);
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.disableRewardClaim(), null);
    }

    @Test
    void addPools(){
        AssetConfig assetconfig = new AssetConfig();
//        constant.poolID = addresses.get(0);
//        constant.asset = BigInteger.ONE;
//        constant.distPercentage = BigInteger.TWO;
//        constant.assetName = BigInteger.ONE;
//        constant.rewardEntity = BigInteger.TEN;
        AssetConfig[] assetconfigs = new AssetConfig[]{
                assetconfig
        };
        Address reserve = (Address) addresses.get(0);
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.addPools(assetconfigs), null);
    }

    @Test
    void addPool(){
        AssetConfig assetconfig = new AssetConfig();
//        constant.poolID = addresses.get(0);
//        constant.asset = BigInteger.ONE;
//        constant.distPercentage = BigInteger.TWO;
//        constant.assetName = BigInteger.ONE;
//        constant.rewardEntity = BigInteger.TEN;

        Address reserve = (Address) addresses.get(0);
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.addPool(assetconfig), null);
    }

    @Test
    void removePool(){
        Address asset = (Address) addresses.get(2);
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.removePool(asset), null);
    }

    @Test
    void transferOmmToDaoFund(){
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.transferOmmToDaoFund(BigInteger.TEN), null);
    }


    @Test
    void transferOmmFromDaoFund(){
        Address token = (Address) addresses.get(2);
        Address address = (Address) addresses.get(0);
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.transferOmmFromDaoFund(BigInteger.TEN,address), null);
    }

    @Test
    void transferFundFromFeeProvider(){
        Address token = (Address) addresses.get(2);
        Address to = (Address) addresses.get(0);
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.transferFundFromFeeProvider(token,BigInteger.TEN,to), null);
    }

    @Test
    void getVotersCount(){
        int vote_index = 1;
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.getVotersCount(vote_index), null);
    }

    @Test
    void setVoteDuration(){
        BigInteger duration = BigInteger.valueOf(1000);
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.setVoteDuration(duration), null);
    }

    @Test
    void setQuorum(){
        BigInteger quorum = BigInteger.valueOf(1000);
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.setQuorum(quorum), null);
    }

    @Test
    void setVoteDefinitionFee(){
        BigInteger fee = BigInteger.valueOf(1000);
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.setVoteDefinitionFee(fee), null);
    }

    @Test
    void setVoteDefinitionCriteria(){
        BigInteger percentage = BigInteger.TEN;
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.setVoteDefinitionCriteria(percentage), null);
    }

    @Test
    void cancelVote(){
        int vote_index = 0;
        assertUserRevert(GovernanceException.proposalNotFound(vote_index),
                () -> testClient.governance.cancelVote(vote_index), null);
    }

    @Test
    void updateVoteForum(){
        int vote_index = 0;
        String forum = "forum";
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.updateVoteForum(vote_index, forum), null);
        assertUserRevert(GovernanceException.proposalNotFound(vote_index),
                () -> ommClient.governance.updateVoteForum(vote_index,forum), null);

    }

    @Test
    void updateTotalVotingWeight(){
        int vote_index = 0;
        BigInteger weight = BigInteger.TEN;
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.updateTotalVotingWeight(vote_index, weight), null);
        assertUserRevert(GovernanceException.proposalNotFound(vote_index),
                () -> ommClient.governance.updateTotalVotingWeight(vote_index,weight), null);

    }

    @Test
    void castVote(){
        int vote_index = 0;
        assertUserRevert(GovernanceException.proposalNotFound(vote_index),
                () -> testClient.governance.castVote(vote_index,true), null);
    }

    @Test
    void execute_proposal(){
        int vote_index = 10;
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.execute_proposal(vote_index), null);
    }

    @Test
    void setProposalStatus(){
        int vote_index = 0;

        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.setProposalStatus(vote_index,"Pending"), null);

        assertUserRevert(GovernanceException.unknown("invalid proposal status " + null),
                () -> ommClient.governance.setProposalStatus(vote_index,null), null);

        assertUserRevert(GovernanceException.proposalNotFound(vote_index),
                () -> ommClient.governance.setProposalStatus(vote_index,"pending"), null);
    }

    @Test
    void enableHandleActions(){
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.enableHandleActions(), null);
    }

    @Test
    void disableHandleActions(){
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.disableHandleActions(), null);
    }

    @Test
    void setAssetWeight(){
        WeightStruct weight = new WeightStruct(addresses.get(0),BigInteger.TEN);

        WeightStruct[] weights = new WeightStruct[]{
                weight
        };
        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.setAssetWeight("",weights,BigInteger.valueOf(100000)), null);
    }

    @Test
    void addType(){

        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.addType("",true), null);
    }

    @Test
    void addAsset(){

        assertUserRevert(GovernanceException.notOwner(),
                () -> testClient.governance.addAsset("","",addresses.get(0),BigInteger.valueOf(100)), null);
    }
































}
