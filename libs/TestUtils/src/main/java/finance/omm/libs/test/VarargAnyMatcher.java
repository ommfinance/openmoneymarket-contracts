package finance.omm.libs.test;

import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.VarargMatcher;

public class VarargAnyMatcher<T> implements ArgumentMatcher<T>, VarargMatcher {

    @Override
    public boolean matches(T t) {
        return true;
    }
}
