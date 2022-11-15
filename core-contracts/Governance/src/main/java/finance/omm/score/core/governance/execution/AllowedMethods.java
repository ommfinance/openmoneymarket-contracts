package finance.omm.score.core.governance.execution;

import finance.omm.score.core.governance.exception.GovernanceException;
import java.util.List;
import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import scorex.util.ArrayList;

public class AllowedMethods {

    public static final DictDB<Address, OMMList> methodsOfContract = Context.newDictDB("supportedMethodsOfContract",
            OMMList.class);
    public static final ArrayDB<Address> contractList = Context.newArrayDB("contractList", Address.class);

    public static void addAllowedMethods(Address contract, String[] methods) {
        addContract(contract);

        for(String method: methods) {
            addAllowedMethod(contract, method);
        }
    }

    public static void removeAllowedMethods(Address contract, String[] methods) {
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

    public static List<String> allowedMethodsOf(Address contract) {
        OMMList sArr = methodsOfContract.get(contract);
        return sArr.getMethods();
    }

    public static void isValidMethod(Address contract, String method) {
        OMMList sArr = methodsOfContract.get(contract);

        if (sArr == null || sArr.notIn(method)) {
            throw GovernanceException.unknown("Method not allowed to call");
        }
    }

    private static void addAllowedMethod(Address contract, String method) {
        OMMList<String> current = methodsOfContract.get(contract);
        if (current == null) {
            current = new OMMList();
            current.add(method);
        } else {
            current.add(method);
        }
        methodsOfContract.set(contract, current);
    }

    private static void removeAllowedMethod(Address contract, String method) {
        OMMList sArr = methodsOfContract.get(contract);
        if (sArr == null) {
            throw GovernanceException.unknown("Contract not added");
        }
        sArr.remove(method);

        methodsOfContract.set(contract, sArr);
        if (sArr.size() == 0) {
            removeContract(contract);
        }
    }

    private static void addContract(Address contract) {
        if (!arrayContains(contractList, contract)) {
            contractList.add(contract);
        }
    }

    private static void removeContract(Address contract) {
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

    private static boolean arrayContains(ArrayDB<Address> arr, Address value) {
        int size = arr.size();
        for (int i = 0; i < size; i++) {
            if (arr.get(i).equals(value)) {
                return true;
            }
        }
        return false;
    }
}
