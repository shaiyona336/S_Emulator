package servlets;

import com.google.gson.Gson;
import dtos.ProgramDetails;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import manager.EngineManager;

import java.io.IOException;

@WebServlet(name = "ExpandProgramServlet", urlPatterns = "/expand-program")
public class ExpandProgramServlet extends HttpServlet {
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

        String degreeParam = request.getParameter("degree");
        if (degreeParam == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Missing degree parameter")));
            return;
        }

        try {
            int degree = Integer.parseInt(degreeParam);
            EngineManager engineManager = EngineManager.getInstance();
            ProgramDetails expandedDetails = engineManager.expandProgram(username, degree);

            if (expandedDetails == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(gson.toJson(new ErrorResponse("No program loaded")));
                return;
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(expandedDetails));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Invalid degree parameter")));
        }
    }
}