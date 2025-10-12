package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import components.executor.Context;
import components.instruction.Instruction;
import components.label.Label;
import components.variable.Variable;

public class GsonProvider {
    private static final Gson GSON_INSTANCE;

    static {
        GSON_INSTANCE = new GsonBuilder()
                .registerTypeAdapter(Variable.class, new VariableAdapter())
                .registerTypeAdapter(Label.class, new LabelAdapter())
                .registerTypeAdapter(Instruction.class, new InstructionAdapter())
                .registerTypeAdapter(Context.class, new ContextAdapter())  // ADD THIS
                .create();
    }

    public static Gson getGson() {
        return GSON_INSTANCE;
    }
}