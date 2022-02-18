package finance.omm.score.reward.test.unit;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.score.core.reward.distribution.RewardController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

public class RewardDistributionUnitTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private Account owner;
    private Score score;
    private RewardController scoreSpy;

    @BeforeEach
    void setup() throws Exception {
        owner = sm.createAccount(100);

        score = sm.deploy(owner, RewardController.class);

        scoreSpy = (RewardController) spy(score.getInstance());
        score.setInstance(scoreSpy);
    }

    @DisplayName("Unauthorized call")
    @Test
    void testAssetName() {
        Address asset = sm.createAccount().getAddress();
        Executable call = () -> score.invoke(sm.createAccount(100), "setAssetName", asset, "_temp");
        String expectedErrorMessage = "require owner access";
        expectErrorMessage(call, expectedErrorMessage);
    }

    @DisplayName("asset not found")
    @Test
    void testSetAssetName() {
        Address asset = sm.createAccount().getAddress();
        Executable call = () -> score.invoke(owner, "setAssetName", asset, "_temp");
        String expectedErrorMessage = "Asset not found " + asset.toString();
        expectErrorMessage(call, expectedErrorMessage);
    }


    public void expectErrorMessage(Executable contractCall, String errorMessage) {
        AssertionError e = Assertions.assertThrows(AssertionError.class, contractCall);
        assertEquals(errorMessage, e.getMessage());
    }

}
