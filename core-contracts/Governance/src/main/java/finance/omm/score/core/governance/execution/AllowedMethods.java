package finance.omm.score.core.governance.execution;

import finance.omm.score.core.governance.exception.GovernanceException;
import java.util.List;
import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import scorex.util.ArrayList;

public class AllowedMethods {

    public static final DictDB<Address, StringArray> methodsOfContract = Context.newDictDB("methodsOfContract",
            StringArray.class);
    public static final ArrayDB<Address> contractList = Context.newArrayDB("contractList", Address.class);

    public void addAllowedMethods(Address contract, String[] methods) {
        addContract(contract);

        for(String method: methods) {
            addAllowedMethod(contract, method);
        }
    }

    public void removeAllowedMethods(Address contract, String[] methods) {
        for(String method: methods) {
            removeAllowedMethod(contract, method);
        }
    }

    public List<Address> getContractList() {
        List<Address> contracts = new ArrayList<>();
        int size = contractList.size();
        for (int i = 0; i < size; i++) {
            contracts.add(contractList.get(i));
        }
        return contracts;
    }

    public String[] allowedMethodsOf(Address contract) {
        StringArray sArr = methodsOfContract.get(contract);
        return sArr.getMethod();
    }

    public static void isValidMethod(Address contract, String method) {
        StringArray sArr = methodsOfContract.get(contract);

        if (sArr == null || sArr.notIn(method)) {
            throw GovernanceException.unauthorized("Method not allowed to call");
        }
    }

    private void addAllowedMethod(Address contract, String method) {
        StringArray current = methodsOfContract.get(contract);
        if (current == null) {
            current = new StringArray(new String[]{method});
        } else {
            current.add(method);
        }

        methodsOfContract.set(contract, current);
    }

    private void removeAllowedMethod(Address contract, String method) {
        StringArray sArr = methodsOfContract.get(contract);
        sArr.remove(method);

        methodsOfContract.set(contract, sArr);
        if (sArr.size() == 0) {
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

    private boolean arrayContains(ArrayDB<Address> arr, Address value) {
        int size = arr.size();
        for (int i = 0; i < size; i++) {
            if (arr.get(i).equals(value)) {
                return true;
            }
        }
        return false;
    }
}
