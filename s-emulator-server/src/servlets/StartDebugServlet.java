package servlets;

import com.google.gson.Gson;
import components.architecture.Architecture;
import dtos.DebugStepDetails;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import manager.EngineManager;
import manager.User;
import utils.GsonProvider;

import java.io.BufferedReader;
import java.io.IOException;

@WebServlet(name = "StartDebugServlet", urlPatterns = "/start-debug")
public class StartDebugServlet extends HttpServlet {
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

        DebugRequest debugRequest = gson.fromJson(sb.toString(), DebugRequest.class);
        if (debugRequest == null || debugRequest.inputs == null || debugRequest.architecture == null) {
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
            Architecture architecture = Architecture.fromString(debugRequest.architecture);

            // Validate architecture
            if (!engineManager.getUserEngine(username).validateArchitecture(architecture)) {
                String message = engineManager.getUserEngine(username).getArchitectureValidationMessage(architecture);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(new ErrorResponse(message)));
                return;
            }

            // Check credits for architecture cost
            int architectureCost = architecture.getCost();
            if (user.getCredits() < architectureCost + 10) { // +10 for at least a few steps
                response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                response.getWriter().write(gson.toJson(new ErrorResponse(
                        "Insufficient credits. Required: ~" + (architectureCost + 10) +
                                ", Available: " + user.getCredits()
                )));
                return;
            }

            // Deduct architecture cost
            if (!user.deductCredits(architectureCost)) {
                response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                response.getWriter().write(gson.toJson(new ErrorResponse("Failed to deduct architecture cost")));
                return;
            }

            // Store architecture in session for step-over to use
            request.getSession().setAttribute("debugArchitecture", architecture.name());

            DebugStepDetails stepDetails = engineManager.startDebugging(
                    username,
                    debugRequest.degree,
                    debugRequest.inputs
            );

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(stepDetails));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ErrorResponse("Debug start failed: " + e.getMessage())));
        }
    }

    private static class DebugRequest {
        int degree;
        Long[] inputs;
        String architecture;
    }
}