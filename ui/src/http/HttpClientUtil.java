package http;

import com.google.gson.Gson;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HttpClientUtil {

    private final static OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .cookieJar(new SimpleCookieJar()) // This line needs the inner class below
            .build();
    private final static String BASE_URL = "http://localhost:8080/s-emulator"; // Use your application context path
    private final static Gson GSON = new Gson();

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

    public static void uploadFile(File file) throws IOException {
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
            System.out.println("Server response: " + response.body().string());
        }
    }

    //
    // ▼▼▼ NESTED HELPER CLASS ▼▼▼
    //
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
}