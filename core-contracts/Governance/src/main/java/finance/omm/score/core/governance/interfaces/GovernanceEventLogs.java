package finance.omm.score.core.governance.interfaces;

import java.math.BigInteger;
import score.Address;
import score.annotation.EventLog;

public interface GovernanceEventLogs {

    @EventLog(indexed = 2)
    default void VoteCast(String vote_name, boolean vote, Address voter, BigInteger stake, BigInteger total_for,
            BigInteger total_against) {}


    @EventLog(indexed = 2)
    default void ActionExecuted(BigInteger vote_index, String vote_status) {}


    @EventLog(indexed = 2)
    default void ProposalCreated(BigInteger vote_index, String name, Address proposer) {}
}
