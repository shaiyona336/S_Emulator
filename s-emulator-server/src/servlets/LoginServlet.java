package servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import manager.EngineManager;

import java.io.IOException;

@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");

        String username = request.getParameter("username");
        if (username == null || username.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Error: Username is required");
            return;
        }

        EngineManager engineManager = EngineManager.getInstance();
        boolean added = engineManager.addUser(username);

        if (!added) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.getWriter().println("Error: Username '" + username + "' is already taken");
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("username", username);

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("Login successful.");
    }
}