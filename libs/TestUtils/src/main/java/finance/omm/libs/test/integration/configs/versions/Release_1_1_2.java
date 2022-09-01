package finance.omm.libs.test.integration.configs.versions;

import finance.omm.libs.structs.ReserveAddressDetails;

public class Release_1_1_2 extends Release {


    @Override
    public boolean init() {
        System.out.println("----init release v1.1.2----");
        ReserveAddressDetails sicx = new ReserveAddressDetails();
        sicx.dTokenAddress = addressMap.get("dICX");
        sicx.oTokenAddress = addressMap.get("oICX");
        sicx.oTokenName = "oICX";
        sicx.dTokenName = "dICX";
        sicx.reserveAddress = addressMap.get("sICX");
        sicx.reserveName = "sICX";
        ommClient.addressManager.addReserveAddress(sicx, true);

        ReserveAddressDetails iusdc = new ReserveAddressDetails();
        iusdc.dTokenAddress = addressMap.get("dIUSDC");
        iusdc.oTokenAddress = addressMap.get("oIUSDC");
        iusdc.oTokenName = "oUSDC";
        iusdc.dTokenName = "dUSDC";
        iusdc.reserveAddress = addressMap.get("IUSDC");
        iusdc.reserveName = "iUSDC";
        ommClient.addressManager.addReserveAddress(iusdc, true);

        return this.next(ommClient);
    }

}
