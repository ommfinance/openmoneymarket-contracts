package finance.omm.score.core.governance.utils;

import finance.omm.score.core.governance.exception.GovernanceException;
import java.util.List;
import score.Address;
import score.ArrayDB;
import score.BranchDB;
import score.Context;
import score.DictDB;
import scorex.util.ArrayList;

public class AllowedMethods {

    public static final BranchDB<Address, DictDB<String, String>> allowedMethods = Context.newBranchDB("allowedMethods",
            String.class);
    public static final DictDB<Address, ArrayDB> methodsOfContract = Context.newDictDB("methodsOfContract",
            ArrayDB.class);
    public static final ArrayDB<Address> contractList = Context.newArrayDB("supportedContracts",
            Address.class);

    public void addAllowedMethod(Address contract, String method, String parameters) {
        addContract(contract);
        addAllowedMethod(contract, method);
        allowedMethods.at(contract).set(method, parameters);
    }

    public List<String> getAllowedMethodsOfContract(Address contract) {
        ArrayDB arr = methodsOfContract.get(contract);
        List<String> allowedMethods = new ArrayList<>();
        int size = arr.size();
        for (int i = 0; i < size; i++) {
            allowedMethods.add((String) arr.get(i));
        }
        return allowedMethods;
    }

    public void removeAllowedMethod(Address contract, String method) {
        removeAllowedMethodInternal(contract, method);
        allowedMethods.at(contract).set(method, null);
    }

    public String getMethodParameters(Address contract, String method) {
        return allowedMethods.at(contract).getOrDefault(method, "");
    }

    public static void isValidMethod(Address contract, String method) {
        if (allowedMethods.at(contract).get(method) == null) {
            throw GovernanceException.unknown("Method not allowed to call.");
        }
    }

    private void addAllowedMethod(Address contract, String method) {
        ArrayDB arr = methodsOfContract.get(contract);
        if (!arrayContains(arr, method)) {
            arr.add(method);
        }
    }

    private void removeAllowedMethodInternal(Address contract, String method) {
        ArrayDB arr = methodsOfContract.get(contract);
        if (arrayContains(arr, method)) {
            String methodName = (String) arr.pop();
            int size = arr.size();
            if (!method.equals(methodName)) {
                for (int i = 0; i < size; i++) {
                    if (arr.get(i).equals(method)) {
                        arr.set(i, methodName);
                    }
                }
            }
        }

        if (arr.size() == 0) {
            removeContract(contract);
        }
    }

    private void addContract(Address contract) {
        if (!arrayContains(contractList, contract)) {
            contractList.add(contract);
        }
    }

    private void removeContract(Address contract) {
        Address methodName = contractList.pop();
        int size = contractList.size();
        if (!contract.equals(methodName)) {
            for (int i = 0; i < size; i++) {
                if (contractList.get(i).equals(contract)) {
                    contractList.set(i, methodName);
                }
            }
        }
    }

    public boolean arrayContains(ArrayDB arr, String value) {
        int size = arr.size();
        for (int i = 0; i < size; i++) {
            if (arr.get(i).equals(value)) {
                return true;
            }
        }
        return false;
    }

    public boolean arrayContains(ArrayDB arr, Address value) {
        int size = arr.size();
        for (int i = 0; i < size; i++) {
            if (arr.get(i).equals(value)) {
                return true;
            }
        }
        return false;
    }
}
