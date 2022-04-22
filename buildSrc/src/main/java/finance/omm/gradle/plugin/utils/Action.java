package finance.omm.gradle.plugin.utils;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public class Action {

    private String contract;
    private String method;
    private JsonNode __args__;
    private Map<String, Object> params;
    private float order;


    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public float getOrder() {
        return order;
    }

    public void setOrder(float order) {
        this.order = order;
    }

    public JsonNode getArgs() {
        return __args__;
    }

    public void set__args__(JsonNode args) {
        this.__args__ = args;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
