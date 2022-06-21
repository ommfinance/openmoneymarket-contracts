package finance.omm.libs.test.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import foundation.icon.jsonrpc.Address;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Score {

    private String name;
    private String contract;
    private String path;
    private Map<String, Object> params;
    private Map<String, String> addressParams;
    private Address address;
    private float order;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public float getOrder() {
        return order;
    }

    public void setOrder(float order) {
        this.order = order;
    }

    public String getContract() {
        return contract == null ? name : contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public Map<String, String> getAddressParams() {
        if (addressParams == null) {
            return Map.of();
        }
        return addressParams;
    }

    public void setAddressParams(Map<String, String> addressParams) {
        this.addressParams = addressParams;
    }

    public void addParams(String key, Object value) {
        this.params.put(key, value);
    }
}
