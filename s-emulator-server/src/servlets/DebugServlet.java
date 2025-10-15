// s-emulator-server/src/servlets/DebugServlet.java
package servlets;

import com.google.gson.Gson;
import components.architecture.Architecture;
import dtos.DebugStepDetails;
import dtos.ExecutionDetails;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet(name = "DebugServlet", urlPatterns = "/debug")
public class DebugServlet extends HttpServlet {
    private final Gson gson = GsonProvider.getGson();

    private static final Map<String, DebugSession> debugSessions = new ConcurrentHashMap<>();

    private static class DebugSession {
        String programName;
        String architecture;
        int architectureCost;

        DebugSession(String programName, String architecture, int architectureCost) {
            this.programName = programName;
            this.architecture = architecture;
            this.architectureCost = architectureCost;
        }
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        String username = (String) request.getSession(false).getAttribute("username");
        if (username == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(new ErrorResponse("User not logged in")));
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Missing action parameter")));
            return;
        }

        EngineManager engineManager = EngineManager.getInstance();
        User user = engineManager.getUser(username);

        try {
            switch (action) {
                case "start" -> {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = request.getReader()) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }

                    }

                    DebugStartRequest debugRequest = gson.fromJson(sb.toString(), DebugStartRequest.class);
                    if (debugRequest == null || debugRequest.architecture == null) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().write(gson.toJson(new ErrorResponse("Invalid request body")));
                        return;
                    }

                    // Validate architecture
                    Architecture arch;
                    try {
                        arch = Architecture.valueOf(debugRequest.architecture);
                    } catch (IllegalArgumentException e) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().write(gson.toJson(new ErrorResponse("Invalid architecture")));
                        return;
                    }

                    // Deduct architecture cost upfront
                    if (!user.deductCredits(arch.getCost())) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().write(gson.toJson(new ErrorResponse(
                                "Insufficient credits for architecture cost: " + arch.getCost()
                        )));
                        return;
                    }

                    // Start debugging with remaining credits
                    DebugStepDetails stepDetails = engineManager.startDebugging(
                            username,
                            debugRequest.degree,
                            debugRequest.inputs,
                            debugRequest.architecture,
                            user.getCredits()
                    );

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(gson.toJson(stepDetails));


                    ProgramDetails programDetails = engineManager.getProgramDetails(username);
                    debugSessions.put(username, new DebugSession(
                            programDetails.name(),
                            debugRequest.architecture,
                            arch.getCost()
                    ));
                }
                case "step" -> {
                    DebugStepDetails stepDetails;
                    try {
                        stepDetails = engineManager.stepOver(username);
                    } catch (RuntimeException e) {
                        if (e.getMessage().contains("Out of credits")) {
                            response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                            response.getWriter().write(gson.toJson(new ErrorResponse(e.getMessage())));
                            return;
                        }
                        throw e;
                    }

                    // Deduct credits from user
                    user.deductCredits(stepDetails.cyclesConsumedThisStep());

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(gson.toJson(stepDetails));
                }
                case "resume" -> {
                    ExecutionDetails executionDetails;
                    DebugSession session = debugSessions.get(username);

                    try {
                        executionDetails = engineManager.resume(username);
                    } catch (RuntimeException e) {
                        if (e.getMessage().contains("Out of credits")) {
                            response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                            response.getWriter().write(gson.toJson(new ErrorResponse(e.getMessage())));
                            debugSessions.remove(username); // Clean up
                            return;
                        }
                        throw e;
                    }

                    // Record the run
                    if (session != null) {
                        EngineManager.ProgramInfo programInfo = engineManager.getProgramInfo(session.programName);
                        if (programInfo != null) {
                            int totalCost = session.architectureCost + executionDetails.cycles();
                            programInfo.recordRun(totalCost);
                        }
                        debugSessions.remove(username); // Clean up
                    }

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(gson.toJson(executionDetails));
                }
                case "stop" -> {
                    engineManager.stopDebugging(username);
                    debugSessions.remove(username); // Clean up
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(gson.toJson(new SuccessResponse("Debug session stopped")));
                }
                default -> {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(gson.toJson(new ErrorResponse("Invalid action: " + action)));
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ErrorResponse("Debug error: " + e.getMessage())));
        }
    }

    private static class DebugStartRequest {
        int degree;
        Long[] inputs;
        String architecture;  // ADDED
    }

    private static class SuccessResponse {
        private final String message;

        public SuccessResponse(String message) {
            this.message = message;
        }
    }
}