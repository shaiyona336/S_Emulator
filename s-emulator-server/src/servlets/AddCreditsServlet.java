package servlets;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import manager.EngineManager;
import manager.User;
import utils.GsonProvider;

import java.io.BufferedReader;
import java.io.IOException;

@WebServlet(name = "AddCreditsServlet", urlPatterns = "/add-credits")
public class AddCreditsServlet extends HttpServlet {
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

        // Read JSON body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        AddCreditsRequest addCreditsRequest = gson.fromJson(sb.toString(), AddCreditsRequest.class);
        if (addCreditsRequest == null || addCreditsRequest.amount <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Invalid amount")));
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

            user.addCredits(addCreditsRequest.amount);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(new SuccessResponse(
                    "Added " + addCreditsRequest.amount + " credits. New balance: " + user.getCredits()
            )));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ErrorResponse("Error adding credits: " + e.getMessage())));
        }
    }

    private static class AddCreditsRequest {
        int amount;
    }

    private static class SuccessResponse {
        private final String message;
        public SuccessResponse(String message) {
            this.message = message;
        }
    }
}