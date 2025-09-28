package components.executor;

import components.program.Program;
import java.util.Map;

public interface Executor {

    Long run(Long... input);

    Context getVariablesContext();
}