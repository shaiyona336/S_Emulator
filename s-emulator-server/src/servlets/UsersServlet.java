package servlets;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import manager.EngineManager;
import manager.User;
import utils.GsonProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "UsersServlet", urlPatterns = "/users")
public class UsersServlet extends HttpServlet {
    private final Gson gson = GsonProvider.getGson();

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
        List<UserInfo> userInfos = new ArrayList<>();

        for (User user : engineManager.getAllUsers()) {
            userInfos.add(new UserInfo(
                    user.getUsername(),
                    user.getProgramsUploaded(),
                    user.getFunctionsUploaded(),
                    user.getCredits(),
                    user.getUsedCredits(),
                    user.getTotalRuns()
            ));
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(userInfos));
    }

    private static class UserInfo {
        private final String username;
        private final int programsUploaded;
        private final int functionsUploaded;
        private final int credits;
        private final int usedCredits;
        private final int totalRuns;

        public UserInfo(String username, int programsUploaded, int functionsUploaded,
                        int credits, int usedCredits, int totalRuns) {
            this.username = username;
            this.programsUploaded = programsUploaded;
            this.functionsUploaded = functionsUploaded;
            this.credits = credits;
            this.usedCredits = usedCredits;
            this.totalRuns = totalRuns;
        }
    }
}