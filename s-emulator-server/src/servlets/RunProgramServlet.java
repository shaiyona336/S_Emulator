package servlets;

import com.google.gson.Gson;
import dtos.ExecutionDetails;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import manager.EngineManager;

import java.io.BufferedReader;
import java.io.IOException;

@WebServlet(name = "RunProgramServlet", urlPatterns = "/run-program")
public class RunProgramServlet extends HttpServlet {
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

        // Read JSON body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        RunRequest runRequest = gson.fromJson(sb.toString(), RunRequest.class);
        if (runRequest == null || runRequest.inputs == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Invalid request body")));
            return;
        }

        try {
            EngineManager engineManager = EngineManager.getInstance();
            ExecutionDetails executionDetails = engineManager.runProgram(
                    username,
                    runRequest.degree,
                    runRequest.inputs
            );

            if (executionDetails == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(gson.toJson(new ErrorResponse("No program loaded")));
                return;
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(executionDetails));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ErrorResponse("Execution error: " + e.getMessage())));
        }
    }

    private static class RunRequest {
        int degree;
        Long[] inputs;
    }
}