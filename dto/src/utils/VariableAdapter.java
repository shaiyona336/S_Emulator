// Location: Create in a shared utils package or in both dto and ui modules
package utils;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import components.variable.Variable;
import components.variable.VariableFactory;

import java.io.IOException;

public class VariableAdapter extends TypeAdapter<Variable> {

    @Override
    public void write(JsonWriter out, Variable value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.value(value.getStringVariable());
    }

    @Override
    public Variable read(JsonReader in) throws IOException {
        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        String varString = in.nextString();
        return VariableFactory.createVariableFromString(varString);
    }
}