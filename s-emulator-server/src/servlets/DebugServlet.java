package servlets;

import com.google.gson.Gson;
import dtos.DebugStepDetails;
import dtos.ExecutionDetails;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import manager.EngineManager;

import java.io.BufferedReader;
import java.io.IOException;

@WebServlet(name = "DebugServlet", urlPatterns = "/debug")
public class DebugServlet extends HttpServlet {
    private final Gson gson = new Gson();

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
                    if (debugRequest == null) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().write(gson.toJson(new ErrorResponse("Invalid request body")));
                        return;
                    }

                    DebugStepDetails stepDetails = engineManager.startDebugging(
                            username,
                            debugRequest.degree,
                            debugRequest.inputs
                    );

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(gson.toJson(stepDetails));
                }
                case "step" -> {
                    DebugStepDetails stepDetails = engineManager.stepOver(username);
                    if (stepDetails == null) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().write(gson.toJson(new ErrorResponse("Not in debug mode")));
                        return;
                    }

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(gson.toJson(stepDetails));
                }
                case "resume" -> {
                    ExecutionDetails executionDetails = engineManager.resume(username);
                    if (executionDetails == null) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().write(gson.toJson(new ErrorResponse("Not in debug mode")));
                        return;
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
    }

    private static class SuccessResponse {
        private final String message;

        public SuccessResponse(String message) {
            this.message = message;
        }
    }
}