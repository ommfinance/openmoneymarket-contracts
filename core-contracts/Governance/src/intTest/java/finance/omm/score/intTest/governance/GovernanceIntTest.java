package finance.omm.score.intTest.governance;

import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.scores.LendingPoolScoreClient;
import foundation.icon.jsonrpc.Address;
import foundation.icon.score.client.RevertedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.libs.test.AssertRevertedException.assertReverted;
import static finance.omm.libs.test.AssertRevertedException.assertUserReverted;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GovernanceIntTest extends AbstractGovernanceIntTest{

    @DisplayName("Test name")
    @Test
    @Order(1)
    public void testName() {
        assertEquals("Omm Governance Manager", ownerClient.governance.name());
    }

    @DisplayName("Reserve status")
    @Test
    @Order(1)
    public void setReserveActiveStatus(){
        Address sICX = addressMap.get(Contracts.sICX.getKey());
        Map<String,Object> reserveData =ownerClient.lendingPoolCore.getReserveData(sICX);

        assertEquals("0x1",reserveData.get("isActive"));

        ownerClient.governance.setReserveActiveStatus(sICX,false);

        reserveData =ownerClient.lendingPoolCore.getReserveData(sICX);

        assertEquals("0x0",reserveData.get("isActive"));

        ownerClient.governance.setReserveActiveStatus(sICX,true);
    }

    @DisplayName("Freeze status")
    @Test
    @Order(1)
    public void setReserveFreezeStatus(){
        Address sICX = addressMap.get(Contracts.sICX.getKey());
        Map<String,Object> reserveData =ownerClient.lendingPoolCore.getReserveData(sICX);
        assertEquals("0x0",reserveData.get("isFreezed"));

        ownerClient.governance.setReserveFreezeStatus(sICX,true);

        reserveData =ownerClient.lendingPoolCore.getReserveData(sICX);

        assertEquals("0x1",reserveData.get("isFreezed"));

        ownerClient.governance.setReserveFreezeStatus(sICX,false);
    }

    @DisplayName("handle actions")
    @Test
    @Order(2)
    public void toggleActions(){
        assertUserReverted(41, () -> alice.governance.enableHandleActions(), null);

        depositICX();
        redeem();
        borrow();
        repay();

        ownerClient.governance.disableHandleActions();

        assertReverted(new RevertedException(1,"handle action disabled"),()->depositICX());
        assertReverted(new RevertedException(1,"handle action disabled"),()->redeem());
        assertReverted(new RevertedException(1,"handle action disabled"),()->borrow());
        assertReverted(new RevertedException(1,"handle action disabled"),()->repay());

        ownerClient.governance.enableHandleActions();

    }
    protected void depositICX() {
        ((LendingPoolScoreClient)alice.lendingPool).
                deposit(BigInteger.valueOf(100).multiply(ICX),BigInteger.valueOf(100).multiply(ICX));
    }

    protected void redeem(){
        Address oICX = addressMap.get(Contracts.oICX.getKey());
        alice.lendingPool.redeem(oICX, BigInteger.ONE, false);
    }


    protected void borrow(){
        Address sICX = addressMap.get(Contracts.sICX.getKey());
        alice.lendingPool.borrow(sICX, BigInteger.TEN.multiply(ICX));
    }

    protected void repay(){
        Address lendingPool = addressMap.get(Contracts.LENDING_POOL.getKey());
        BigInteger val = BigInteger.valueOf(5).multiply(ICX);
        alice.sICX.transfer(lendingPool, val,createByteData("repay"));
    }

    private byte[] createByteData(String method) {
        JsonObject internalParameters = new JsonObject();
        JsonObject jsonData = new JsonObject()
                .add("method", method)
                .add("params",internalParameters);
        return jsonData.toString().getBytes();
    }

}
