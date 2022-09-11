package finance.omm.score.core.finance.omm.score.core.lendingpoolDataProvider.test.unit;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.core.score.interfaces.LendingPoolDataProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.score.core.lendingpoolDataProvider.LendingPoolDataProviderImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static finance.omm.score.core.lendingpoolDataProvider.AbstractLendingPoolDataProvider.TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LendingPoolDataProviderUnitTest extends TestBase {


    private static ServiceManager sm = getServiceManager();
    private static Account owner = sm.createAccount();

    private Score lendingPoolDataProvider;
    private Account accountAddressProvider = sm.createAccount();

    @BeforeEach
    public void setup() throws Exception {
        lendingPoolDataProvider = sm.deploy(owner, LendingPoolDataProviderImpl.class, accountAddressProvider.getAddress());
    }

    @Test
    void testName() throws Exception {
        assertEquals("Omm " + TAG, lendingPoolDataProvider.call("name") );
    }

}
