package servlets;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import manager.EngineManager;
import utils.GsonProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@WebServlet(name = "FileUploadServlet", urlPatterns = "/upload-file")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 5, maxRequestSize = 1024 * 1024 * 5 * 5)
public class FileUploadServlet extends HttpServlet {
    private final Gson gson = GsonProvider.getGson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        // Get username from session
        String username = (String) request.getSession(false).getAttribute("username");
        if (username == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(new ErrorResponse("User not logged in")));
            return;
        }

        Part filePart = request.getPart("programFile");
        if (filePart == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("No file uploaded")));
            return;
        }

        String xmlContent = readFromInputStream(filePart.getInputStream());

        EngineManager engineManager = EngineManager.getInstance();
        try {
            String programName = engineManager.uploadProgram(username, xmlContent);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(new SuccessResponse(
                    "File processed successfully. Program '" + programName + "' loaded."
            )));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse(
                    "Error processing file: " + e.getMessage()
            )));
        }
    }

    private String readFromInputStream(InputStream inputStream) {
        return new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A").next();
    }

    private static class SuccessResponse {
        private final String message;
        public SuccessResponse(String message) {
            this.message = message;
        }
    }

    private static class ErrorResponse {
        private final String error;
        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}