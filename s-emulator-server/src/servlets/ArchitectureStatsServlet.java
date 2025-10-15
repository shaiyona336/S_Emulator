// s-emulator-server/src/servlets/ArchitectureStatsServlet.java
package servlets;

import com.google.gson.Gson;
import components.architecture.Architecture;
import components.architecture.ArchitectureAnalyzer;
import dtos.ArchitectureStats;
import dtos.ProgramDetails;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import manager.EngineManager;
import utils.GsonProvider;

import java.io.IOException;

@WebServlet(name = "ArchitectureStatsServlet", urlPatterns = "/architecture-stats")
public class ArchitectureStatsServlet extends HttpServlet {
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

        String archParam = request.getParameter("architecture");
        if (archParam == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Missing architecture parameter")));
            return;
        }

        try {
            Architecture architecture = Architecture.valueOf(archParam);
            EngineManager engineManager = EngineManager.getInstance();
            ProgramDetails details = engineManager.getProgramDetails(username);

            if (details == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(gson.toJson(new ErrorResponse("No program loaded")));
                return;
            }

            ArchitectureStats stats = ArchitectureAnalyzer.analyzeProgram(
                    details.instructions(),
                    architecture
            );

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(stats));

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Invalid architecture")));
        }
    }
}