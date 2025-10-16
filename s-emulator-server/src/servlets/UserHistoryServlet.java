package servlets;

import com.google.gson.Gson;
import dtos.RunHistoryDetails;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import manager.EngineManager;
import utils.GsonProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "UserHistoryServlet", urlPatterns = "/user-history")
public class UserHistoryServlet extends HttpServlet {
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

        String targetUsername = request.getParameter("targetUser");
        if (targetUsername == null) {
            targetUsername = username; // Default to current user
        }

        try {
            EngineManager engineManager = EngineManager.getInstance();

            // Get the history for the target user
            List<RunHistoryDetails> history = engineManager.getUserHistory(targetUsername);

            if (history == null) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(gson.toJson(new ArrayList<>()));
                return;
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(history));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ErrorResponse("Error retrieving history: " + e.getMessage())));
        }
    }


    private static class HistoryEntryDTO {
        private final int runNumber;
        private final String type;        // "PROGRAM" or "FUNCTION"
        private final String name;
        private final String architecture;
        private final int degree;
        private final long yValue;
        private final int cycles;

        public HistoryEntryDTO(int runNumber, String type, String name,
                               String architecture, int degree, long yValue, int cycles) {
            this.runNumber = runNumber;
            this.type = type;
            this.name = name;
            this.architecture = architecture;
            this.degree = degree;
            this.yValue = yValue;
            this.cycles = cycles;
        }
    }
}