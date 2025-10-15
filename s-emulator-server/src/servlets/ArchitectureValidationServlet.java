package servlets;

import com.google.gson.Gson;
import components.architecture.Architecture;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import manager.EngineManager;
import utils.GsonProvider;

import java.io.IOException;

@WebServlet(name = "ArchitectureValidationServlet", urlPatterns = "/validate-architecture")
public class ArchitectureValidationServlet extends HttpServlet {
    private final Gson gson = GsonProvider.getGson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        String username = (String) request.getSession(false).getAttribute("username");
        if (username == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(new ErrorResponse("User not logged in")));
            return;
        }

        String architectureParam = request.getParameter("architecture");
        if (architectureParam == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Architecture parameter required")));
            return;
        }

        try {
            Architecture architecture = Architecture.fromString(architectureParam);
            EngineManager engineManager = EngineManager.getInstance();

            boolean isValid = engineManager.getUserEngine(username).validateArchitecture(architecture);
            String message = engineManager.getUserEngine(username).getArchitectureValidationMessage(architecture);

            ValidationResponse validationResponse = new ValidationResponse(isValid, message, architecture.getCost());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(validationResponse));

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Invalid architecture: " + e.getMessage())));
        }
    }

    private static class ValidationResponse {
        boolean valid;
        String message;
        int architectureCost;

        ValidationResponse(boolean valid, String message, int architectureCost) {
            this.valid = valid;
            this.message = message;
            this.architectureCost = architectureCost;
        }
    }
}