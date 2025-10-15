package servlets;

import com.google.gson.Gson;
import components.architecture.Architecture;
import components.architecture.ArchitectureAnalyzer;
import dtos.ArchitectureStats;
import dtos.ExecutionDetails;
import dtos.ExecutionRequest;
import dtos.ProgramDetails;
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

        ExecutionRequest runRequest = gson.fromJson(sb.toString(), ExecutionRequest.class);
        if (runRequest == null || runRequest.inputs() == null || runRequest.architecture() == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Invalid request body")));
            return;
        }

        try {
            EngineManager engineManager = EngineManager.getInstance();
            User user = engineManager.getUser(username);

            Architecture arch = Architecture.valueOf(runRequest.architecture());

            // Check if user has enough credits
            ProgramDetails details = engineManager.getProgramDetails(username);
            if (details == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(gson.toJson(new ErrorResponse("No program loaded")));
                return;
            }

            // Expand to target degree
            ProgramDetails expandedDetails = engineManager.expandProgram(username, runRequest.degree());

            // Analyze if program can run on this architecture
            ArchitectureStats stats = ArchitectureAnalyzer.analyzeProgram(
                    expandedDetails.instructions(),
                    arch
            );

            if (!stats.canRunOnArchitecture()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(new ErrorResponse(
                        "Program requires " + stats.minimumRequiredArchitecture() +
                                " but selected " + arch.name()
                )));
                return;
            }

            // Calculate required credits
            int avgCost = ArchitectureAnalyzer.calculateAverageCost(expandedDetails.instructions(), arch);

            if (user.getCredits() < avgCost) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(new ErrorResponse(
                        "Insufficient credits. Required: " + avgCost + ", Available: " + user.getCredits()
                )));
                return;
            }

            // Deduct architecture cost upfront
            if (!user.deductCredits(arch.getCost())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(new ErrorResponse("Failed to deduct architecture cost")));
                return;
            }

            ExecutionDetails executionDetails = engineManager.runProgram(
                    username,
                    runRequest.degree(),
                    runRequest.inputs()
            );

            // Deduct cycle costs
            int cycleCost = executionDetails.cycles();
            user.deductCredits(cycleCost);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(executionDetails));

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Invalid architecture: " + e.getMessage())));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ErrorResponse("Execution error: " + e.getMessage())));
        }
    }

    private static class RunRequest {
        int degree;
        Long[] inputs;
    }
}