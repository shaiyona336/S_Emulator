package utils;

import com.google.gson.*;
import components.executor.Context;
import components.executor.StandardContext;
import components.variable.Variable;

import java.lang.reflect.Type;
import java.util.Map;

public class ContextAdapter implements JsonSerializer<Context>, JsonDeserializer<Context> {

    @Override
    public JsonElement serialize(Context src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();

        // Serialize the variables map
        JsonObject variablesJson = new JsonObject();
        for (Map.Entry<Variable, Long> entry : src.getVariables().entrySet()) {
            variablesJson.addProperty(entry.getKey().getStringVariable(), entry.getValue());
        }
        result.add("variables", variablesJson);
        result.addProperty("totalCycles", src.getTotalCycles());

        return result;
    }

    @Override
    public Context deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        StandardContext standardContext = new StandardContext();

        // Deserialize variables
        JsonObject variablesJson = jsonObject.getAsJsonObject("variables");
        for (Map.Entry<String, JsonElement> entry : variablesJson.entrySet()) {
            String varName = entry.getKey();
            long value = entry.getValue().getAsLong();
            Variable variable = components.variable.VariableFactory.createVariableFromString(varName);
            standardContext.updateVariableValue(variable, value);
        }

        // Deserialize total cycles
        if (jsonObject.has("totalCycles")) {
            standardContext.setTotalCycles(jsonObject.get("totalCycles").getAsInt());
        }

        return standardContext;
    }
}