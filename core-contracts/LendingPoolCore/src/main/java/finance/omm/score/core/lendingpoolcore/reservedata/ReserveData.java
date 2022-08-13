package finance.omm.score.core.lendingpoolcore.reservedata;

import score.Address;
import score.Context;
import score.VarDB;

import java.math.BigInteger;

public class ReserveData {

    public final VarDB<Address> reserveAddress = Context.newVarDB("id", Address.class);
    public final VarDB<Address> oTokenAddress = Context.newVarDB("oToken", Address.class);
    public final VarDB<Address> dTokenAddress = Context.newVarDB("dToken", Address.class);
    public final VarDB<BigInteger> lastUpdateTimestamp = Context.newVarDB("lastUpdateTimestamp", BigInteger.class);
    public final VarDB<BigInteger> liquidityRate = Context.newVarDB("liquidityRate", BigInteger.class);
    public final VarDB<BigInteger> borrowRate = Context.newVarDB("borrowRate", BigInteger.class);
    public final VarDB<BigInteger> borrowThreshold = Context.newVarDB("borrowThreshold", BigInteger.class);
    public final VarDB<BigInteger> liquidityCumulativeIndex = Context.newVarDB("liquidityCumulativeIndex", BigInteger.class);
    public final VarDB<BigInteger> borrowCumulativeIndex = Context.newVarDB("borrowCumulativeIndex", BigInteger.class);
    public final VarDB<BigInteger> baseLTVasCollateral = Context.newVarDB("baseLTLasCollateral", BigInteger.class);
    public final VarDB<BigInteger> liquidationThreshold = Context.newVarDB("liquidationThreshold", BigInteger.class);
    public final VarDB<BigInteger> liquidationBonus = Context.newVarDB("liquidationBonus", BigInteger.class);
    public final VarDB<BigInteger> decimals = Context.newVarDB("decimals", BigInteger.class);
    public final VarDB<Boolean> borrowingEnabled = Context.newVarDB("borrowingEnabled", Boolean.class);
    public final VarDB<Boolean> usageAsCollateralEnabled = Context.newVarDB("usageAsCollateralEnabled", Boolean.class);
    public final VarDB<Boolean> isFreezed = Context.newVarDB("isFreezed", Boolean.class);
    public final VarDB<Boolean> isActive = Context.newVarDB("Active", Boolean.class);


    public ReserveData(byte[] prefix) {

    }
}
