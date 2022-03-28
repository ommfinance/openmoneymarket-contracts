package finance.omm.score.reward.test.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.score.core.reward.distribution.RewardDistributionImpl;
import java.math.BigInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;

public class RewardDistributionUnitTest extends TestBase {

    private static final ServiceManager sm = getServiceManager();
    private Account owner;
    private Score score;
    private RewardDistributionImpl scoreSpy;

    BigInteger startTimestamp = BigInteger.valueOf(1629954000000000L);

    @BeforeEach
    void setup() throws Exception {
        owner = sm.createAccount(100);

        score = sm.deploy(owner, RewardDistributionImpl.class, Account.newScoreAccount(1).getAddress(),
                startTimestamp);

        scoreSpy = (RewardDistributionImpl) spy(score.getInstance());
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
