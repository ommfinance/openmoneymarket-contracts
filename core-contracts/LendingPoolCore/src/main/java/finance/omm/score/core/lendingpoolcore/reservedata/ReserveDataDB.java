package finance.omm.score.core.lendingpoolcore.reservedata;

import score.Address;
import score.BranchDB;
import score.Context;

import java.math.BigInteger;
import score.VarDB;

public class ReserveDataDB {

    public final BranchDB<String, VarDB<Address>> reserveAddress = Context.newBranchDB("id", Address.class);
    public final BranchDB<String, VarDB<Address>> oTokenAddress = Context.newBranchDB("oToken", Address.class);
    public final BranchDB<String, VarDB<Address>> dTokenAddress = Context.newBranchDB("dToken", Address.class);
    public final BranchDB<String, VarDB<BigInteger>> lastUpdateTimestamp = Context.newBranchDB("lastUpdateTimestamp", BigInteger.class);
    public final BranchDB<String, VarDB<BigInteger>> liquidityRate = Context.newBranchDB("liquidityRate", BigInteger.class);
    public final BranchDB<String, VarDB<BigInteger>> borrowRate = Context.newBranchDB("borrowRate", BigInteger.class);
    public final BranchDB<String, VarDB<BigInteger>> borrowThreshold = Context.newBranchDB("borrowThreshold", BigInteger.class);
    public final BranchDB<String, VarDB<BigInteger>> liquidityCumulativeIndex = Context.newBranchDB("liquidityCumulativeIndex", BigInteger.class);
    public final BranchDB<String, VarDB<BigInteger>> borrowCumulativeIndex = Context.newBranchDB("borrowCumulativeIndex", BigInteger.class);
    public final BranchDB<String, VarDB<BigInteger>> baseLTVasCollateral = Context.newBranchDB("baseLTLasCollateral", BigInteger.class);
    public final BranchDB<String, VarDB<BigInteger>> liquidationThreshold = Context.newBranchDB("liquidationThreshold", BigInteger.class);
    public final BranchDB<String, VarDB<BigInteger>> liquidationBonus = Context.newBranchDB("liquidationBonus", BigInteger.class);
    public final BranchDB<String, VarDB<Integer>> decimals = Context.newBranchDB("decimals", Integer.class);
    public final BranchDB<String, VarDB<Boolean>> borrowingEnabled = Context.newBranchDB("borrowingEnabled", Boolean.class);
    public final BranchDB<String, VarDB<Boolean>> usageAsCollateralEnabled = Context.newBranchDB("usageAsCollateralEnabled", Boolean.class);
    public final BranchDB<String, VarDB<Boolean>> isFreezed = Context.newBranchDB("isFreezed", Boolean.class);
    public final BranchDB<String, VarDB<Boolean>> isActive = Context.newBranchDB("Active", Boolean.class);


    public ReserveDataDB() {

    }
}
