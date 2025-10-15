package servlets;

import com.google.gson.Gson;
import dtos.ProgramDetails;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import manager.EngineManager;
import utils.GsonProvider;

import java.io.IOException;

@WebServlet(name = "ProgramDetailsServlet", urlPatterns = "/program-details2")
public class ProgramDetailsServlet extends HttpServlet {
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

        EngineManager engineManager = EngineManager.getInstance();
        ProgramDetails details = engineManager.getProgramDetails(username);

        if (details == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(gson.toJson(new ErrorResponse("No program loaded")));
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(details));
    }
}
