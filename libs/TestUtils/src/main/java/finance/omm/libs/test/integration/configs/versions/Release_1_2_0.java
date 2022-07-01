package finance.omm.libs.test.integration.configs.versions;

import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import java.math.BigInteger;
import java.util.Map;
import score.Address;

public class Release_1_2_0 extends Release {


    @Override
    public boolean init() {
        System.out.println("----init release v1.2.0----");
        Map<String, Address> contractAddresses = ommClient.getContractAddresses();

        // set bOMM and reward weight controller to address provider
        AddressDetails[] addressDetails = new AddressDetails[] {
          new AddressDetails(Contracts.BOOSTED_OMM.getKey(),
                  contractAddresses.get(Contracts.BOOSTED_OMM.getKey())),
          new AddressDetails(Contracts.REWARD_WEIGHT_CONTROLLER.getKey(),
                  contractAddresses.get(Contracts.REWARD_WEIGHT_CONTROLLER.getKey()))
        };
        ommClient.addressManager.setAddresses(addressDetails);

        ommClient.addressManager.addAddressToScore(Contracts.BOOSTED_OMM.getKey(), new String[]{
                Contracts.DELEGATION.getKey(),
                Contracts.REWARDS.getKey(),
                Contracts.GOVERNANCE.getKey()
        });

        ommClient.addressManager.addAddressToScore(Contracts.REWARD_WEIGHT_CONTROLLER.getKey(), new String[]{
                Contracts.REWARDS.getKey(),
                Contracts.GOVERNANCE.getKey()
        });

        ommClient.addressManager.addAddressToScore(Contracts.REWARDS.getKey(), new String[]{
                Contracts.REWARD_WEIGHT_CONTROLLER.getKey(),
                Contracts.BOOSTED_OMM.getKey(),
        });

        ommClient.addressManager.addAddressToScore(Contracts.GOVERNANCE.getKey(), new String[]{
                Contracts.BOOSTED_OMM.getKey(),
                Contracts.REWARD_WEIGHT_CONTROLLER.getKey(),
        });

        ommClient.addressManager.addAddressToScore(Contracts.DELEGATION.getKey(), new String[]{
                Contracts.BOOSTED_OMM.getKey(),
        });

        ommClient.addressManager.addAddressToScore(Contracts.OMM_TOKEN.getKey(), new String[]{
                Contracts.BOOSTED_OMM.getKey(),
        });

        ommClient.governance.addType("daoFund", true);
        ommClient.governance.addType("workerToken", true);
        ommClient.governance.addType("OMMLocking", false);
        ommClient.governance.addType("reserve", false);
        ommClient.governance.addType("liquidity", false);

        String oICX = Contracts.oICX.getKey();
        String dICX = Contracts.dICX.getKey();
        String oIUSDC = Contracts.oIUSDC.getKey();
        String dIUSDC = Contracts.dIUSDC.getKey();

        ommClient.governance.addAsset("reserve", oICX, contractAddresses.get(oICX), BigInteger.valueOf(-1));
        ommClient.governance.addAsset("reserve", dICX, contractAddresses.get(dICX), BigInteger.valueOf(-1));
        ommClient.governance.addAsset("reserve", oIUSDC, contractAddresses.get(oIUSDC), BigInteger.valueOf(-1));
        ommClient.governance.addAsset("reserve", dIUSDC, contractAddresses.get(dIUSDC), BigInteger.valueOf(-1));

//        ommClient.governance.addAsset("OMMLocking", "bOMM", contractAddresses.get("bOMM"),
//                BigInteger.valueOf(-1));


        return this.next(ommClient);
    }

}
