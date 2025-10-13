package servlets;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import manager.EngineManager;
import utils.GsonProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "FunctionsListServlet", urlPatterns = "/functions-list")
public class FunctionsListServlet extends HttpServlet {
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
        List<FunctionInfo> functionInfos = new ArrayList<>();

        for (EngineManager.FunctionInfo func : engineManager.getAllFunctions()) {
            functionInfos.add(new FunctionInfo(
                    func.getUserString(),
                    func.getProgramName(),
                    func.getOwner(),
                    func.getInstructionCount(),
                    func.getMaxDegree()
            ));
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(functionInfos));
    }

    private static class FunctionInfo {
        private final String name;
        private final String programName;
        private final String owner;
        private final int instructionCount;
        private final int maxDegree;

        public FunctionInfo(String name, String programName, String owner,
                            int instructionCount, int maxDegree) {
            this.name = name;
            this.programName = programName;
            this.owner = owner;
            this.instructionCount = instructionCount;
            this.maxDegree = maxDegree;
        }
    }
}