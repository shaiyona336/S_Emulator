// s-emulator-server/src/servlets/DebugServlet.java
package servlets;

import com.google.gson.Gson;
import components.architecture.Architecture;
import dtos.DebugStepDetails;
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

@WebServlet(name = "DebugServlet", urlPatterns = "/debug")
public class DebugServlet extends HttpServlet {
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
                    try {
                        executionDetails = engineManager.resume(username);
                    } catch (RuntimeException e) {
                        if (e.getMessage().contains("Out of credits")) {
                            response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                            response.getWriter().write(gson.toJson(new ErrorResponse(e.getMessage())));
                            return;
                        }
                        throw e;
                    }

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(gson.toJson(executionDetails));
                }
                case "stop" -> {
                    engineManager.stopDebugging(username);
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