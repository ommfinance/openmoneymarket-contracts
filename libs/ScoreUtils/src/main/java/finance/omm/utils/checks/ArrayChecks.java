package finance.omm.utils.checks;

import java.util.HashSet;

public class ArrayChecks {

    public static <T> boolean containsDuplicate(T[] arr) {
        HashSet<T> set = new HashSet<T>();

        for (T element : arr) {
            if (!set.add(element)) {
                return true;
            }
        }

        return false;
    }
}
