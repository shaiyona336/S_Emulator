package http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dtos.DebugStepDetails;
import dtos.ExecutionDetails;
import dtos.ProgramDetails;
import dtos.RunHistoryDetails;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HttpClientUtil {

    private final static OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .cookieJar(new SimpleCookieJar())
            .build();

    private final static String BASE_URL = "http://localhost:8080/s_emulator_server_war_exploded";
    private final static Gson GSON = new Gson();

    // ========== Authentication ==========

    public static void login(String username) throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "/login")
                .post(formBody)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Login failed: " + response.body().string());
            }
        }
    }

    // ========== File Upload ==========

    public static String uploadFile(File file) throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("programFile", file.getName(),
                        RequestBody.create(file, MediaType.parse("application/xml")))
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "/upload-file")
                .post(requestBody)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("File upload failed: " + response.body().string());
            }
            return response.body().string();
        }
    }

    // ========== Program Operations ==========

    public static ProgramDetails getProgramDetails() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/program-details")
                .get()
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get program details: " + response.body().string());
            }
            return GSON.fromJson(response.body().string(), ProgramDetails.class);
        }
    }

    public static int getMaxDegree() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/max-degree")
                .get()
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get max degree: " + response.body().string());
            }
            MaxDegreeResponse resp = GSON.fromJson(response.body().string(), MaxDegreeResponse.class);
            return resp.maxDegree;
        }
    }

    public static ProgramDetails expandProgram(int degree) throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("degree", String.valueOf(degree))
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "/expand-program")
                .post(formBody)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to expand program: " + response.body().string());
            }
            return GSON.fromJson(response.body().string(), ProgramDetails.class);
        }
    }

    public static List<String> getProgramNames() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/program-names")
                .get()
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get program names: " + response.body().string());
            }
            Type listType = new TypeToken<List<String>>(){}.getType();
            return GSON.fromJson(response.body().string(), listType);
        }
    }

    public static void setContextProgram(String programName) throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("programName", programName)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "/set-context")
                .post(formBody)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to set context: " + response.body().string());
            }
        }
    }

    // ========== Execution Operations ==========

    public static ExecutionDetails runProgram(int degree, Long[] inputs) throws IOException {
        RunRequest runRequest = new RunRequest(degree, inputs);
        String jsonBody = GSON.toJson(runRequest);

        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "/run-program")
                .post(body)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to run program: " + response.body().string());
            }
            return GSON.fromJson(response.body().string(), ExecutionDetails.class);
        }
    }

    // ========== Debug Operations ==========

    public static DebugStepDetails startDebugging(int degree, Long[] inputs) throws IOException {
        DebugStartRequest debugRequest = new DebugStartRequest(degree, inputs);
        String jsonBody = GSON.toJson(debugRequest);

        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "/debug?action=start")
                .post(body)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to start debugging: " + response.body().string());
            }
            return GSON.fromJson(response.body().string(), DebugStepDetails.class);
        }
    }

    public static DebugStepDetails stepOver() throws IOException {
        RequestBody body = RequestBody.create("", MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(BASE_URL + "/debug?action=step")
                .post(body)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to step over: " + response.body().string());
            }
            return GSON.fromJson(response.body().string(), DebugStepDetails.class);
        }
    }

    public static ExecutionDetails resume() throws IOException {
        RequestBody body = RequestBody.create("", MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(BASE_URL + "/debug?action=resume")
                .post(body)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to resume: " + response.body().string());
            }
            return GSON.fromJson(response.body().string(), ExecutionDetails.class);
        }
    }

    public static void stopDebugging() throws IOException {
        RequestBody body = RequestBody.create("", MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(BASE_URL + "/debug?action=stop")
                .post(body)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to stop debugging: " + response.body().string());
            }
        }
    }

    // ========== Statistics ==========

    public static List<RunHistoryDetails> getStatistics() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/statistics")
                .get()
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get statistics: " + response.body().string());
            }
            Type listType = new TypeToken<List<RunHistoryDetails>>(){}.getType();
            return GSON.fromJson(response.body().string(), listType);
        }
    }

    // ========== Helper Classes ==========

    private static class SimpleCookieJar implements CookieJar {
        private final List<Cookie> allCookies = new ArrayList<>();

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            allCookies.addAll(cookies);
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            return allCookies;
        }
    }

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
}