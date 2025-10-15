package execution;

import dtos.ExecutionDetails;

import java.util.Map;
import java.util.concurrent.*;

public class ExecutionManager {
    private static final ExecutionManager instance = new ExecutionManager();
    private final ExecutorService executorService;
    private final Map<String, Future<ExecutionDetails>> activeTasks;

    private ExecutionManager() {
        // Thread pool with 10 threads
        this.executorService = Executors.newFixedThreadPool(10);
        this.activeTasks = new ConcurrentHashMap<>();
    }

    public static ExecutionManager getInstance() {
        return instance;
    }

    public String submitExecution(ExecutionTask task) {
        Future<ExecutionDetails> future = executorService.submit(task);
        activeTasks.put(task.getTaskId(), future);
        return task.getTaskId();
    }

    public ExecutionTask.ExecutionStatus getTaskStatus(String taskId) {
        Future<ExecutionDetails> future = activeTasks.get(taskId);
        if (future == null) {
            return null;
        }

        if (future.isDone()) {
            return ExecutionTask.ExecutionStatus.COMPLETED;
        } else if (future.isCancelled()) {
            return ExecutionTask.ExecutionStatus.FAILED;
        } else {
            return ExecutionTask.ExecutionStatus.RUNNING;
        }
    }

    public ExecutionDetails getResult(String taskId) throws ExecutionException, InterruptedException {
        Future<ExecutionDetails> future = activeTasks.get(taskId);
        if (future == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        return future.get();
    }

    public void shutdown() {
        executorService.shutdown();
    }
}