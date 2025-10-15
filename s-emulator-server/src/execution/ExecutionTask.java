package execution;

import components.architecture.Architecture;
import components.engine.Engine;
import dtos.ExecutionDetails;
import manager.EngineManager;
import manager.User;

import java.util.UUID;
import java.util.concurrent.Callable;

public class ExecutionTask implements Callable<ExecutionDetails> {
    private final String taskId;
    private final String username;
    private final int degree;
    private final Long[] inputs;
    private final Architecture architecture;
    private ExecutionStatus status;

    public ExecutionTask(String username, int degree, Long[] inputs, Architecture architecture) {
        this.taskId = UUID.randomUUID().toString();
        this.username = username;
        this.degree = degree;
        this.inputs = inputs;
        this.architecture = architecture;
        this.status = ExecutionStatus.PENDING;
    }

    @Override
    public ExecutionDetails call() {
        try {
            status = ExecutionStatus.RUNNING;

            EngineManager engineManager = EngineManager.getInstance();
            User user = engineManager.getUser(username);
            Engine engine = engineManager.getUserEngine(username);

            if (user == null || engine == null) {
                status = ExecutionStatus.FAILED;
                throw new RuntimeException("User or engine not found");
            }

            // Charge architecture cost upfront
            if (!user.deductCredits(architecture.getCost())) {
                status = ExecutionStatus.FAILED;
                throw new RuntimeException("Insufficient credits for architecture cost: " + architecture.getCost());
            }

            // Run the program
            ExecutionDetails result = engine.runProgram(degree, inputs);

            // Charge for cycles (1 cycle = 1 credit)
            int cyclesCost = result.totalCycles();
            if (!user.deductCredits(cyclesCost)) {
                status = ExecutionStatus.FAILED;
                throw new RuntimeException("Ran out of credits during execution");
            }

            // Increment run count
            user.incrementRun();

            // Record program run statistics
            String programName = engine.getContextProgramName();
            EngineManager.ProgramInfo programInfo = engineManager.getProgramInfo(programName);
            if (programInfo != null) {
                programInfo.recordRun(architecture.getCost() + cyclesCost);
            }

            status = ExecutionStatus.COMPLETED;
            return result;

        } catch (Exception e) {
            status = ExecutionStatus.FAILED;
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String getTaskId() {
        return taskId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public enum ExecutionStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }
}