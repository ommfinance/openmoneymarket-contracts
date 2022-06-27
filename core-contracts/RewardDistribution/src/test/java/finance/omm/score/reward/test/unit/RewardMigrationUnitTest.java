package finance.omm.score.reward.test.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.VarargAnyMatcher;
import java.math.BigInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentMatchers;
import score.Address;


public class RewardMigrationUnitTest extends RewardDistributionAbstractTest {


    @DisplayName("UpdateAssetIndex")
    @Nested
    class TestUpdateAssetIndex {

        @DisplayName("should throw unauthorized access")
        @Test
        void should_throw_unauthorized_access() {
            Executable call = () -> score.invoke(Account.newScoreAccount(501), "updateAssetIndexes");
            expectErrorMessage(call, "require owner access");
        }
    }

    @DisplayName("MigrateUserRewards")
    @Nested
    class TestMigrateUserRewards {

        @DisplayName("should throw unauthorized access")
        @Test
        void should_throw_unauthorized_access() {
            Object[] params = new Object[]{
                    addresses
            };
            Executable call = () -> score.invoke(Account.newScoreAccount(501), "migrateUserRewards", params);
            expectErrorMessage(call, "require owner access");
        }

        @DisplayName("should throw exception if asset index not updated")
        @Test
        void should_throw_if_asset_index_not_updated() {
            Object[] params = new Object[]{
                    addresses
            };
            Executable call = () -> score.invoke(owner, "migrateUserRewards", params);
            expectErrorMessage(call, "Asset indexes are not migrated, Please migrate asset index first");
        }

        @DisplayName("should update index")
        @Test
        void should_update_index() {
            Address[] addressList = new Address[]{
                    sm.createAccount().getAddress(),
                    sm.createAccount().getAddress(),
            };

            VarargAnyMatcher<Object> matcher = new VarargAnyMatcher<>();

            doReturn(addressList).when(scoreSpy).call(eq(Address[].class),eq(Contracts.WORKER_TOKEN),
                    eq("getWallets"));
            doReturn(BigInteger.valueOf(50)).when(scoreSpy).call(eq(BigInteger.class),eq(Contracts.WORKER_TOKEN),
                    eq("balanceOf"), any());
            doReturn(BigInteger.valueOf(100)).when(scoreSpy).call(eq(BigInteger.class),eq(Contracts.WORKER_TOKEN),
                    eq("totalSupply"));

            doNothing().when(scoreSpy)
                    .call(eq(Contracts.OMM_TOKEN), eq("transfer"),
                            ArgumentMatchers.<Object>argThat(matcher));
            doNothing().when(scoreSpy).call(eq(Contracts.OMM_TOKEN), eq("transfer"), any());

            score.invoke(owner, "updateAssetIndexes");
            Object[] params = new Object[]{
                    addresses
            };
            score.invoke(owner, "migrateUserRewards", params);
        }
    }
}
