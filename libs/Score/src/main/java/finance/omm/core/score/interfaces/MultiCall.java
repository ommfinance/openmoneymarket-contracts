package finance.omm.core.score.interfaces;

import foundation.icon.score.client.ScoreInterface;
import score.Address;

import java.util.Map;

@ScoreInterface(suffix = "Client")
public interface MultiCall {

    class Call {
        public Address target;
        public String method;
        public String[] params;
    }

    String name();
    Map<String, Object> aggregate(Call[] calls);

    Map<String, Object> tryAggregate(boolean requireSuccess, Call[] calls);
}
