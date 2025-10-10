package servlets;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import manager.EngineManager;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "ProgramNamesServlet", urlPatterns = "/program-names")
public class ProgramNamesServlet extends HttpServlet {
    private final Gson gson = new Gson();

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
        List<String> programNames = engineManager.getDisplayableProgramNames(username);

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(programNames));
    }
}