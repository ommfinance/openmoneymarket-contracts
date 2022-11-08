package finance.omm.score.core.governance.execution;

import finance.omm.score.core.governance.exception.GovernanceException;
import java.util.List;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

public class StringArray {

    public String[] method;

    public StringArray() {}

    public StringArray(String[] method) {
        this.method = method;
    }

    public int size() {
        return method.length;
    }

    public void setMethod(String[] method) {
        this.method = method;
    }

    public String[] getMethod() {
        return this.method;
    }

    public void add(String method) {
        if (this.notIn(method)) {
            this.append(method);
        } else {
            throw GovernanceException.unknown("Already Exists: "+ method);
        }
    }

    public void remove(String method) {
        int newSize = this.size() - 1;
        if (newSize < 0 || this.notIn(method)) {
            throw GovernanceException.unknown("Error Removing: "+ method);
        }

        String[] arr = new String[newSize];
        for (int i = 0, k = 0; i <= newSize; i++) {
            if (method.equals(this.method[i])) {
                continue;
            }
            arr[k++] = this.method[i];
        }
        this.setMethod(arr);
    }

    private void append(String method) {
        int size = size();
        String[] arr = new String[size + 1];
        if (size >= 0)
            System.arraycopy(this.method, 0, arr, 0, size);

        arr[size] = method;
        this.setMethod(arr);
    }

    public boolean notIn(String method) {
        int idx = this.indexOf(method);
        return idx < 0;
    }

    public boolean contains(String method) {
        return !notIn(method);
    }

    public int indexOf(String method) {
        int size = this.size();
        if (size == 0) {
            return -1;
        }
        for (int i = 0; i < size; i++) {
            if (method.equals(this.method[i])) {
                return i;
            }
        }
        return -1;
    }

    public static StringArray readObject(ObjectReader reader) {
        StringArray obj = new StringArray();
        reader.beginList();
        if (reader.beginNullableList()) {
            String[] methods;
            List<String> methodList = new ArrayList<>();
            while (reader.hasNext()) {
                methodList.add(reader.readNullable(String.class));
            }
            methods = new String[methodList.size()];
            for (int i = 0; i < methods.length; i++) {
                methods[i] = methodList.get(i);
            }
            obj.setMethod(methods);
            reader.end();
        }
        reader.end();
        return obj;
    }

    public static void writeObject(ObjectWriter writer, StringArray obj) {
        obj.writeObject(writer);
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(1);
        String[] addresses = this.getMethod();
        if (addresses != null) {
            writer.beginNullableList(addresses.length);
            for (String s : addresses) {
                writer.writeNullable(s);
            }
            writer.end();
        } else {
            writer.writeNull();
        }
        writer.end();
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        StringArray.writeObject(writer, this);
        return writer.toByteArray();
    }
}
