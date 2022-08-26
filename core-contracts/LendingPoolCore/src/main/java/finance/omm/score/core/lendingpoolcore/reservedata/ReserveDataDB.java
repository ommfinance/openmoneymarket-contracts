package finance.omm.score.core.lendingpoolcore.reservedata;

import score.Address;
import score.Context;
import score.DictDB;

import java.math.BigInteger;

public class ReserveDataDB {

    public final DictDB<byte[], Address> reserveAddress = Context.newDictDB("id", Address.class);
    public final DictDB<byte[], Address> oTokenAddress = Context.newDictDB("oToken", Address.class);
    public final DictDB<byte[], Address> dTokenAddress = Context.newDictDB("dToken", Address.class);
    public final DictDB<byte[], BigInteger> lastUpdateTimestamp = Context.newDictDB("lastUpdateTimestamp", BigInteger.class);
    public final DictDB<byte[], BigInteger> liquidityRate = Context.newDictDB("liquidityRate", BigInteger.class);
    public final DictDB<byte[], BigInteger> borrowRate = Context.newDictDB("borrowRate", BigInteger.class);
    public final DictDB<byte[], BigInteger> borrowThreshold = Context.newDictDB("borrowThreshold", BigInteger.class);
    public final DictDB<byte[], BigInteger> liquidityCumulativeIndex = Context.newDictDB("liquidityCumulativeIndex", BigInteger.class);
    public final DictDB<byte[], BigInteger> borrowCumulativeIndex = Context.newDictDB("borrowCumulativeIndex", BigInteger.class);
    public final DictDB<byte[], BigInteger> baseLTVasCollateral = Context.newDictDB("baseLTLasCollateral", BigInteger.class);
    public final DictDB<byte[], BigInteger> liquidationThreshold = Context.newDictDB("liquidationThreshold", BigInteger.class);
    public final DictDB<byte[], BigInteger> liquidationBonus = Context.newDictDB("liquidationBonus", BigInteger.class);
    public final DictDB<byte[], Integer> decimals = Context.newDictDB("decimals", Integer.class);
    public final DictDB<byte[], Boolean> borrowingEnabled = Context.newDictDB("borrowingEnabled", Boolean.class);
    public final DictDB<byte[], Boolean> usageAsCollateralEnabled = Context.newDictDB("usageAsCollateralEnabled", Boolean.class);
    public final DictDB<byte[], Boolean> isFreezed = Context.newDictDB("isFreezed", Boolean.class);
    public final DictDB<byte[], Boolean> isActive = Context.newDictDB("Active", Boolean.class);



    public ReserveDataDB() {

    }
}
