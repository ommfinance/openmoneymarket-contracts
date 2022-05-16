package finance.omm.score.core.governance.enums;


import java.util.Map;
import scorex.util.HashMap;

public enum ProposalStatus {
    PENDING("Pending"),
    ACTIVE("Active"),
    CANCELLED("Cancelled"),
    DEFEATED("Defeated"),
    SUCCEEDED("Succeeded"),
    NO_QUORUM("No Quorum"),
    EXECUTED("Executed"),
    FAILED_EXECUTION("Failed Execution");


    private static final Map<String, ProposalStatus> lookup = new HashMap<>();

    static {
        for (ProposalStatus d : ProposalStatus.values()) {
            lookup.put(d.getStatus(), d);
        }
    }

    private final String status;

    ProposalStatus(String status) {
        this.status = status;
    }


    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return status;
    }

    public static ProposalStatus get(String status) {
        return lookup.get(status);
    }
}
