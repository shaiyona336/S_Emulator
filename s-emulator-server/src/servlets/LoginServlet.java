package servlets;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import manager.EngineManager;
import java.io.IOException;

@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        EngineManager engineManager = EngineManager.getInstance();

        if (username == null || username.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Username cannot be empty.");
            return;
        }

        // Add user and handle session
        boolean success = engineManager.addUser(username.trim());

        if (success) {
            request.getSession().setAttribute("username", username);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("Login successful.");
        } else {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.getWriter().write("Username already taken.");
        }
    }
}