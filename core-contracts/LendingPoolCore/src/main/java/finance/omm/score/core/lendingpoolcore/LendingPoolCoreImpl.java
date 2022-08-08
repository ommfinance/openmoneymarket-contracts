package finance.omm.score.core.lendingpoolcore;


import finance.omm.libs.address.Contracts;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import finance.omm.score.core.lendingpoolcore.exception.LendingPoolCoreException;
import score.Address;

import score.annotation.External;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import static finance.omm.utils.math.MathUtils.*;

public class LendingPoolCoreImpl extends AbstractLendingPoolCore {

    public LendingPoolCoreImpl(Address addressProvider) {
        super(addressProvider, false);
    }

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
    }
}
