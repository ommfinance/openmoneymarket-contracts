package finance.omm.score.core.lendingpoolcore.reservedata;

import score.Address;
import score.Context;
import score.VarDB;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.Map;

public class ReserveDataObject {

    public Address reserveAddress ;
    public Address oTokenAddress ;
    public Address dTokenAddress ;
    public  BigInteger lastUpdateTimestamp ;
    public BigInteger  liquidityRate ;
    public BigInteger  borrowRate ;
    public BigInteger  borrowThreshold ;
    public BigInteger  liquidityCumulativeIndex ;
    public BigInteger  borrowCumulativeIndex ;
    public BigInteger  baseLTVasCollateral ;
    public BigInteger  liquidationThreshold ;
    public BigInteger  liquidationBonus ;
    public BigInteger  decimals ;
    public  Boolean borrowingEnabled ;
    public  Boolean usageAsCollateralEnabled ;
    public Boolean  isFreezed ;
    public Boolean  isActive ;

    ReserveDataObject(Map<String, Object> reserveData){
    this.reserveAddress = (Address) reserveData.get("reserveAddress");
    this.oTokenAddress = (Address) reserveData.get("oTokenAddress");
    this.dTokenAddress = (Address) reserveData.get("dTokenAddress");
    this.lastUpdateTimestamp = (BigInteger) reserveData.get("lastUpdateTimestamp");
    this.liquidityRate = (BigInteger) reserveData.get("liquidityRate");
    this.borrowRate = (BigInteger) reserveData.get("borrowRate");
    this.liquidityCumulativeIndex = (BigInteger) reserveData.get("liquidityCumulativeIndex");
    this.borrowCumulativeIndex = (BigInteger) reserveData.get("borrowCumulativeIndex");
    this.baseLTVasCollateral = (BigInteger) reserveData.get("baseLTVasCollateral");
    this.liquidationThreshold = (BigInteger) reserveData.get("liquidationThreshold");
    this.liquidationBonus = (BigInteger) reserveData.get("liquidationBonus");
    this.decimals = (BigInteger) reserveData.get("decimals");
    this.borrowingEnabled = (Boolean) reserveData.get("borrowingEnabled");
    this.usageAsCollateralEnabled = (Boolean) reserveData.get("usageAsCollateralEnabled");
    this.isFreezed = (Boolean) reserveData.get("isFreezed");
    this.isActive = (Boolean) reserveData.get("isActive");
    this.borrowThreshold = (BigInteger) reserveData.get("borrowThreshold");

    }

}
