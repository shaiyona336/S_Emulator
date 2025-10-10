package servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import manager.EngineManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@WebServlet(name = "FileUploadServlet", urlPatterns = "/upload-file")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 5, maxRequestSize = 1024 * 1024 * 5 * 5)
public class FileUploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");

        Part filePart = request.getPart("programFile"); // "programFile" must match the name in the client's form data
        String xmlContent = readFromInputStream(filePart.getInputStream());

        EngineManager engineManager = EngineManager.getInstance();
        try {
            String programName = engineManager.uploadProgram(xmlContent);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("File processed successfully. Program '" + programName + "' loaded.");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Error processing file: " + e.getMessage());
        }
    }

    private String readFromInputStream(InputStream inputStream) {
        return new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A").next();
    }
}