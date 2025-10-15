package servlets;

import com.google.gson.Gson;
import components.architecture.Architecture;
import dtos.ExecutionDetails;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import manager.EngineManager;
import manager.User;
import utils.GsonProvider;

import java.io.BufferedReader;
import java.io.IOException;

@WebServlet(name = "RunProgramServlet", urlPatterns = "/run-program")
public class RunProgramServlet extends HttpServlet {
    private final Gson gson = GsonProvider.getGson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        String username = (String) request.getSession(false).getAttribute("username");
        if (username == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(new ErrorResponse("User not logged in")));
            return;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        RunRequest runRequest = gson.fromJson(sb.toString(), RunRequest.class);
        if (runRequest == null || runRequest.inputs == null || runRequest.architecture == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Invalid request")));
            return;
        }

        try {
            EngineManager engineManager = EngineManager.getInstance();
            User user = engineManager.getUser(username);

            if (user == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(gson.toJson(new ErrorResponse("User not found")));
                return;
            }

            // Parse architecture
            Architecture architecture = Architecture.fromString(runRequest.architecture);

            // Validate architecture supports all instructions
            if (!engineManager.getUserEngine(username).validateArchitecture(architecture)) {
                String message = engineManager.getUserEngine(username).getArchitectureValidationMessage(architecture);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(new ErrorResponse(message)));
                return;
            }

            // Get average cost estimate
            int architectureCost = architecture.getCost();
            int estimatedCycles = estimateAverageCycles(username, runRequest.degree);
            int totalEstimatedCost = architectureCost + estimatedCycles;

            // Check if user has enough credits
            if (user.getCredits() < totalEstimatedCost) {
                response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                response.getWriter().write(gson.toJson(new ErrorResponse(
                        "Insufficient credits. Required: ~" + totalEstimatedCost +
                                ", Available: " + user.getCredits()
                )));
                return;
            }

            // Deduct architecture cost upfront
            if (!user.deductCredits(architectureCost)) {
                response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                response.getWriter().write(gson.toJson(new ErrorResponse("Failed to deduct architecture cost")));
                return;
            }

            // Run the program
            ExecutionDetails executionDetails = engineManager.runProgram(
                    username,
                    runRequest.degree,
                    runRequest.inputs
            );

            // Deduct cycles cost
            int cyclesCost = executionDetails.totalCycles();
            if (!user.deductCredits(cyclesCost)) {
                response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                response.getWriter().write(gson.toJson(new ErrorResponse(
                        "Ran out of credits during execution. Credits deducted: " + (architectureCost + cyclesCost)
                )));
                return;
            }

            // Increment run count
            user.incrementRun();

            // Record program run statistics
            String programName = engineManager.getUserEngine(username).getContextProgramName();
            EngineManager.ProgramInfo programInfo = engineManager.getProgramInfo(programName);
            if (programInfo != null) {
                programInfo.recordRun(architectureCost + cyclesCost);
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(executionDetails));

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Invalid architecture: " + e.getMessage())));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ErrorResponse("Execution failed: " + e.getMessage())));
        }
    }

    private int estimateAverageCycles(String username, int degree) {
        // Simple estimation: base cycles * degree factor
        // You can improve this by looking at historical data
        return 100 * (degree + 1);
    }

    private static class RunRequest {
        int degree;
        Long[] inputs;
        String architecture;
    }
}