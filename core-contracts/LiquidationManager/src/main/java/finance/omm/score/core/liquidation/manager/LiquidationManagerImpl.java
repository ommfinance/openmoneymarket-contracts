package finance.omm.score.core.liquidation.manager;

import finance.omm.core.score.interfaces.LiquidationManager;
import finance.omm.libs.address.AddressProvider;
import score.Address;

public class LiquidationManagerImpl extends AddressProvider implements LiquidationManager {

    public LiquidationManagerImpl(Address addressProvider) {
        super(addressProvider, false);
    }
}