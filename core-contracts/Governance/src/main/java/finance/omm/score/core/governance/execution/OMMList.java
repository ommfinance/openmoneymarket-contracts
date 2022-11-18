package finance.omm.score.core.governance.execution;

import finance.omm.score.core.governance.exception.GovernanceException;
import java.util.List;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

public class OMMList<T> {

    public List<T> methods;

    public OMMList() {
        this.methods = new ArrayList<>();
    }

    public void setMethods(List<T> methods) {
        this.methods = methods;
    }

    public List<T> getMethods() {
        return this.methods;
    }

    public int size() {
        return this.methods.size();
    }

    public void add(T method) {
        if (this.notIn(method)) {
            this.methods.add(method);
        } else {
            throw GovernanceException.unknown("Already Exists: "+ method);
        }
    }

    public void remove(T method) {
        int size = this.size();
        if (size < 0 || this.notIn(method)) {
            throw GovernanceException.unknown("Error Removing: "+ method);
        }
        this.methods.remove(method);
    }

    public boolean notIn(T method) {
        return this.indexOf(method) < 0;
    }

    public int indexOf(T method) {
        return this.methods.indexOf(method);
    }

    public static OMMList readObject(ObjectReader reader) {
        OMMList<String> obj = new OMMList<String>();
        if (reader.beginNullableList()) {
            List<String> methodList = new ArrayList<>();
            while (reader.hasNext()) {
                methodList.add(reader.readNullable(String.class));
            }
            obj.setMethods(methodList);
            reader.end();
        }
        return obj;
    }

    public static void writeObject(ObjectWriter writer, OMMList obj) {
        obj.writeObject(writer);
    }

    public void writeObject(ObjectWriter writer) {
        List<T> strList = this.getMethods();
        if (strList != null) {
            writer.beginNullableList(strList.size());
            for(T s: strList) {
                writer.writeNullable(s);
            }
            writer.end();
        } else {
            writer.writeNull();
        }
    }
}
