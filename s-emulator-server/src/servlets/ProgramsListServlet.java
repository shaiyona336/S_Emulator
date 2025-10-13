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

@WebServlet(name = "ProgramsListServlet", urlPatterns = "/programs-list")
public class ProgramsListServlet extends HttpServlet {
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
        List<ProgramInfo> programInfos = new ArrayList<>();

        for (EngineManager.ProgramInfo prog : engineManager.getAllPrograms()) {
            programInfos.add(new ProgramInfo(
                    prog.getName(),
                    prog.getOwner(),
                    prog.getInstructionCount(),
                    prog.getMaxDegree(),
                    prog.getRunCount(),
                    prog.getAvgCost()
            ));
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(programInfos));
    }

    private static class ProgramInfo {
        private final String name;
        private final String owner;
        private final int instructionCount;
        private final int maxDegree;
        private final int runCount;
        private final int avgCost;

        public ProgramInfo(String name, String owner, int instructionCount,
                           int maxDegree, int runCount, int avgCost) {
            this.name = name;
            this.owner = owner;
            this.instructionCount = instructionCount;
            this.maxDegree = maxDegree;
            this.runCount = runCount;
            this.avgCost = avgCost;
        }
    }
}