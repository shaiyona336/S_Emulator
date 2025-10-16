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
import java.util.stream.Collectors;

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
            targetUsername = username;
        }

        try {
            EngineManager engineManager = EngineManager.getInstance();
            List<RunHistoryDetails> history = engineManager.getUserHistory(targetUsername);

            if (history == null) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(gson.toJson(new ArrayList<>()));
                return;
            }

            // Convert to DTO
            List<HistoryEntryDTO> dtoList = history.stream()
                    .map(h -> new HistoryEntryDTO(
                            h.runNumber(),
                            h.programType(),
                            h.programName(),
                            h.architecture(),
                            h.expansionDegree(),
                            h.yValue(),
                            h.cyclesNumber()
                    ))
                    .collect(Collectors.toList());

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(dtoList));

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ErrorResponse("Error retrieving history: " + e.getMessage())));
        }
    }

    private static class HistoryEntryDTO {
        public final int runNumber;
        public final String type;
        public final String name;
        public final String architecture;
        public final int degree;
        public final long yValue;
        public final int cycles;

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