package http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dtos.DebugStepDetails;
import dtos.ExecutionDetails;
import dtos.ProgramDetails;
import dtos.RunHistoryDetails;
import utils.GsonProvider;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.*;

public class HttpClientUtil {

    private final static String BASE_URL = "http://localhost:8080/s-emulator";
    private final static Gson GSON = GsonProvider.getGson();
    private static Map<String, String> cookies = new HashMap<>();

    // ========== Authentication ==========

    public static void login(String username) throws IOException {
        URL url = new URL(BASE_URL + "/login");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String params = "username=" + URLEncoder.encode(username, "UTF-8");

        try (OutputStream os = connection.getOutputStream()) {
            os.write(params.getBytes("UTF-8"));
        }

        int responseCode = connection.getResponseCode();
        saveCookies(connection);

        if (responseCode != HttpURLConnection.HTTP_OK) {
            String error = readErrorStream(connection);
            throw new IOException("Login failed: " + error);
        }
    }

    // ========== File Upload ==========

    public static String uploadFile(File file) throws IOException {
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        String LINE_FEED = "\r\n";

        URL url = new URL(BASE_URL + "/upload-file");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        addCookiesToConnection(connection);

        try (OutputStream outputStream = connection.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {

            // Add file part
            writer.append("--").append(boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"programFile\"; filename=\"")
                    .append(file.getName()).append("\"").append(LINE_FEED);
            writer.append("Content-Type: application/xml").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            // Write file content
            Files.copy(file.toPath(), outputStream);
            outputStream.flush();

            writer.append(LINE_FEED);
            writer.append("--").append(boundary).append("--").append(LINE_FEED);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String error = readErrorStream(connection);
            throw new IOException("File upload failed: " + error);
        }

        return readResponse(connection);
    }

    // ========== Program Operations ==========

    public static ProgramDetails getProgramDetails() throws IOException {
        String response = sendGetRequest("/program-details");
        return GSON.fromJson(response, ProgramDetails.class);
    }

    public static int getMaxDegree() throws IOException {
        String response = sendGetRequest("/max-degree");
        MaxDegreeResponse resp = GSON.fromJson(response, MaxDegreeResponse.class);
        return resp.maxDegree;
    }

    public static ProgramDetails expandProgram(int degree) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("degree", String.valueOf(degree));

        String response = sendPostFormRequest("/expand-program", params);
        return GSON.fromJson(response, ProgramDetails.class);
    }

    public static List<String> getProgramNames() throws IOException {
        String response = sendGetRequest("/program-names");
        Type listType = new TypeToken<List<String>>(){}.getType();
        return GSON.fromJson(response, listType);
    }

    public static void setContextProgram(String programName) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("programName", programName);
        sendPostFormRequest("/set-context", params);
    }

    // ========== Execution Operations ==========

    public static ExecutionDetails runProgram(int degree, Long[] inputs) throws IOException {
        RunRequest runRequest = new RunRequest(degree, inputs);
        String response = sendPostJsonRequest("/run-program", runRequest);
        return GSON.fromJson(response, ExecutionDetails.class);
    }

    // ========== Debug Operations ==========

    public static DebugStepDetails startDebugging(int degree, Long[] inputs) throws IOException {
        DebugStartRequest debugRequest = new DebugStartRequest(degree, inputs);
        String response = sendPostJsonRequest("/debug?action=start", debugRequest);
        return GSON.fromJson(response, DebugStepDetails.class);
    }

    public static DebugStepDetails stepOver() throws IOException {
        String response = sendPostJsonRequest("/debug?action=step", "");
        return GSON.fromJson(response, DebugStepDetails.class);
    }

    public static ExecutionDetails resume() throws IOException {
        String response = sendPostJsonRequest("/debug?action=resume", "");
        return GSON.fromJson(response, ExecutionDetails.class);
    }

    public static void stopDebugging() throws IOException {
        sendPostJsonRequest("/debug?action=stop", "");
    }

    // ========== Statistics ==========

    public static List<RunHistoryDetails> getStatistics() throws IOException {
        String response = sendGetRequest("/statistics");
        Type listType = new TypeToken<List<RunHistoryDetails>>(){}.getType();
        return GSON.fromJson(response, listType);
    }

    // ========== Helper Methods ==========

    private static String sendGetRequest(String endpoint) throws IOException {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        addCookiesToConnection(connection);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String error = readErrorStream(connection);
            throw new IOException("Request failed: " + error);
        }

        return readResponse(connection);
    }

    private static String sendPostFormRequest(String endpoint, Map<String, String> params) throws IOException {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        addCookiesToConnection(connection);

        // Build form parameters
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (postData.length() != 0) {
                postData.append('&');
            }
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(param.getValue(), "UTF-8"));
        }

        try (OutputStream os = connection.getOutputStream()) {
            os.write(postData.toString().getBytes("UTF-8"));
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String error = readErrorStream(connection);
            throw new IOException("Request failed: " + error);
        }

        return readResponse(connection);
    }

    private static String sendPostJsonRequest(String endpoint, Object requestBody) throws IOException {
        String jsonBody = (requestBody instanceof String) ? (String) requestBody : GSON.toJson(requestBody);

        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        addCookiesToConnection(connection);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String error = readErrorStream(connection);
            throw new IOException("Request failed: " + error);
        }

        return readResponse(connection);
    }

    private static String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private static String readErrorStream(HttpURLConnection connection) throws IOException {
        InputStream errorStream = connection.getErrorStream();
        if (errorStream == null) {
            return "Unknown error";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(errorStream, "UTF-8"))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private static void saveCookies(HttpURLConnection connection) {
        Map<String, List<String>> headerFields = connection.getHeaderFields();
        List<String> cookiesHeader = headerFields.get("Set-Cookie");

        if (cookiesHeader != null) {
            for (String cookie : cookiesHeader) {
                String[] parts = cookie.split(";")[0].split("=", 2);
                if (parts.length == 2) {
                    cookies.put(parts[0], parts[1]);
                }
            }
        }
    }

    private static void addCookiesToConnection(HttpURLConnection connection) {
        if (!cookies.isEmpty()) {
            StringBuilder cookieHeader = new StringBuilder();
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                if (cookieHeader.length() > 0) {
                    cookieHeader.append("; ");
                }
                cookieHeader.append(entry.getKey()).append("=").append(entry.getValue());
            }
            connection.setRequestProperty("Cookie", cookieHeader.toString());
        }
    }

    // ========== Helper Classes ==========

    private static class RunRequest {
        int degree;
        Long[] inputs;

        RunRequest(int degree, Long[] inputs) {
            this.degree = degree;
            this.inputs = inputs;
        }
    }

    private static class DebugStartRequest {
        int degree;
        Long[] inputs;

        DebugStartRequest(int degree, Long[] inputs) {
            this.degree = degree;
            this.inputs = inputs;
        }
    }

    private static class MaxDegreeResponse {
        int maxDegree;
    }

    public static List<UserInfo> getAllUsers() throws IOException {
        String response = sendGetRequest("/users");
        Type listType = new TypeToken<List<UserInfo>>(){}.getType();
        return GSON.fromJson(response, listType);
    }

    public static List<ProgramInfoDTO> getAllPrograms() throws IOException {
        String response = sendGetRequest("/programs-list");
        Type listType = new TypeToken<List<ProgramInfoDTO>>(){}.getType();
        return GSON.fromJson(response, listType);
    }

    public static List<FunctionInfoDTO> getAllFunctions() throws IOException {
        String response = sendGetRequest("/functions-list");
        Type listType = new TypeToken<List<FunctionInfoDTO>>(){}.getType();
        return GSON.fromJson(response, listType);
    }

    public static void addCredits(int amount) throws IOException {
        AddCreditsRequest request = new AddCreditsRequest(amount);
        String response = sendPostJsonRequest("/add-credits", request);
        // Response is just a success message
    }

    private static class AddCreditsRequest {
        int amount;

        AddCreditsRequest(int amount) {
            this.amount = amount;
        }
    }

    // Add these inner classes at the end of HttpClientUtil
    public static class UserInfo {
        public String username;
        public int programsUploaded;
        public int functionsUploaded;
        public int credits;
        public int usedCredits;
        public int totalRuns;
    }

    public static class ProgramInfoDTO {
        public String name;
        public String owner;
        public int instructionCount;
        public int maxDegree;
        public int runCount;
        public int avgCost;
    }

    public static class FunctionInfoDTO {
        public String name;
        public String programName;
        public String owner;
        public int instructionCount;
        public int maxDegree;
    }
}