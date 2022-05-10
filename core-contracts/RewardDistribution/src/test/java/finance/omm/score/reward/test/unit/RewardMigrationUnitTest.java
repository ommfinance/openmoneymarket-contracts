package finance.omm.score.reward.test.unit;

import com.iconloop.score.test.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;


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
            score.invoke(owner, "updateAssetIndexes");
            Object[] params = new Object[]{
                    addresses
            };
            score.invoke(owner, "migrateUserRewards", params);
        }
    }
}
