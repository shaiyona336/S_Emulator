package utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import components.label.FixedLabel;
import components.label.Label;
import components.label.LabelFactory;

import java.io.IOException;

public class LabelAdapter extends TypeAdapter<Label> {

    @Override
    public void write(JsonWriter out, Label value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        // Write the string representation
        if (value == FixedLabel.EMPTY) {
            out.value("");  // or out.value("EMPTY") - be consistent
        } else {
            out.value(value.getStringLabel());
        }
    }

    @Override
    public Label read(JsonReader in) throws IOException {
        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        String labelString = in.nextString();

        // Handle special cases first
        if (labelString == null || labelString.trim().isEmpty() || "EMPTY".equals(labelString)) {
            return FixedLabel.EMPTY;
        }
        if ("EXIT".equalsIgnoreCase(labelString)) {
            return FixedLabel.EXIT;
        }

        // Use factory for standard labels (L1, L2, etc.)
        return LabelFactory.createLabelFromString(labelString);
    }
}