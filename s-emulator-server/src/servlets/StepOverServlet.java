package servlets;

import com.google.gson.Gson;
import dtos.DebugStepDetails;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import manager.EngineManager;
import manager.User;
import utils.GsonProvider;

import java.io.IOException;

@WebServlet(name = "StepOverServlet", urlPatterns = "/step-over")
public class StepOverServlet extends HttpServlet {
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

        try {
            EngineManager engineManager = EngineManager.getInstance();
            User user = engineManager.getUser(username);

            if (user == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(gson.toJson(new ErrorResponse("User not found")));
                return;
            }

            // Check if user has credits for at least one cycle
            if (user.getCredits() < 1) {
                // Stop debugging
                engineManager.stopDebugging(username);
                response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                response.getWriter().write(gson.toJson(new ErrorResponse("Insufficient credits to continue debugging")));
                return;
            }

            // Perform step
            DebugStepDetails stepDetails = engineManager.stepOver(username);

            // Deduct credits for the step (1 cycle = 1 credit)
            if (stepDetails != null && stepDetails.cyclesConsumed() > 0) {
                user.deductCredits(stepDetails.cyclesConsumed());
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(stepDetails));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ErrorResponse("Step over failed: " + e.getMessage())));
        }
    }
}