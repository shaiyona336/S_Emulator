// engine/src/components/engine/Engine.java
package components.engine;

import dtos.DebugStepDetails;
import dtos.ExecutionDetails;
import dtos.ProgramDetails;
import dtos.RunHistoryDetails;

import java.io.File;
import java.io.Serializable;
import java.util.List;

public interface Engine extends Serializable {
    void loadProgramFromFile(File file);
    boolean isProgramLoaded();
    ProgramDetails getProgramDetails();
    int getProgramMaxDegree();
    ProgramDetails expandProgram(int expansionDegree);
    ExecutionDetails runProgram(int expansionDegree, Long... input);
    List<RunHistoryDetails> getStatistics();
    boolean isRunning();

    // Updated for debugging with credits
    DebugStepDetails startDebugging(int degree, Long[] inputs, String architecture, int initialCredits);  // UPDATED
    DebugStepDetails stepOver();
    ExecutionDetails resume();
    void stop();
    void addRunToHistory(RunHistoryDetails details);

    List<String> getDisplayableProgramNames();
    void setContextProgram(String displayName);
}