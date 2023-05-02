package finance.omm.libs.test.integration.configs.versions;

import static finance.omm.utils.math.MathUtils.ICX;

import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.libs.structs.TypeWeightStruct;
import finance.omm.libs.structs.WeightStruct;
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

        ommClient.addressManager.addAddressToScore(Contracts.FEE_DISTRIBUTION.getKey(),new String[]{
                Contracts.sICX.getKey(),
                Contracts.STAKING.getKey(),
                Contracts.LENDING_POOL_CORE.getKey(),
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
        String daoFund = Contracts.DAO_FUND.getKey();

        ommClient.governance.addAsset("reserve", oICX, contractAddresses.get(oICX), BigInteger.valueOf(-1));
        ommClient.governance.addAsset("reserve", dICX, contractAddresses.get(dICX), BigInteger.valueOf(-1));
        ommClient.governance.addAsset("reserve", oIUSDC, contractAddresses.get(oIUSDC), BigInteger.valueOf(-1));
        ommClient.governance.addAsset("reserve", dIUSDC, contractAddresses.get(dIUSDC), BigInteger.valueOf(-1));

        ommClient.governance.addAsset("OMMLocking", "bOMM", contractAddresses.get("bOMM"),
                BigInteger.valueOf(-1));
        BigInteger systemTime = BigInteger.valueOf(System.currentTimeMillis() / 1000);
        BigInteger time = systemTime.add(BigInteger.valueOf(1));

        ommClient.governance.setTypeWeight(new TypeWeightStruct[]{
                new TypeWeightStruct("reserve", ICX.divide(BigInteger.TWO)),
                new TypeWeightStruct("daoFund", ICX.divide(BigInteger.TWO))
        }, time);


        WeightStruct[] weightStructs = new WeightStruct[]{
                new WeightStruct(contractAddresses.get(oICX),ICX.divide(BigInteger.valueOf(4))),
                new WeightStruct(contractAddresses.get(dICX),ICX.divide(BigInteger.valueOf(4))),
                new WeightStruct(contractAddresses.get(oIUSDC),ICX.divide(BigInteger.valueOf(4))),
                new WeightStruct(contractAddresses.get(dIUSDC),ICX.divide(BigInteger.valueOf(4)))

        };

        WeightStruct[] daoWeightStructs = new WeightStruct[]{
                new WeightStruct(contractAddresses.get(daoFund),ICX)
        };

        ommClient.governance.setAssetWeight("reserve",weightStructs,time);
        ommClient.governance.setAssetWeight("daoFund",daoWeightStructs,time);

        return this.next(ommClient);
    }

}
