package finance.omm.gradle.plugin.utils;

import foundation.icon.jsonrpc.Address;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddressUtils {


    public static Map<String, Object> getParamsForSetAddresses(Map<String, Address> addresses) {
        List<Map<String, Object>> details = new ArrayList<>();
        for (Map.Entry<String, Address> entry : addresses.entrySet()) {
            if (!entry.getKey().equals("addressProvider") && !entry.getKey().equals("owner")) {
                details.add(Map.of(
                        "name", entry.getKey(), "address", entry.getValue()
                ));
            }
        }
        return Map.of("_addressDetails", details);
    }
}
