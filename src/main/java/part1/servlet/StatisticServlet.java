package part1.servlet;

import com.google.gson.Gson;
import part1.model.APIEndpointStats;
import part1.model.APIStats;
import part1.model.ResponseMsg;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;


public class StatisticServlet extends HttpServlet {
    private Gson gson = new Gson();

    /**
     * Get the API performance stats
     * @param req
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        ResponseMsg responseMsg = new ResponseMsg();
        res.setStatus(HttpServletResponse.SC_OK);
        responseMsg.setMessage("Successful operation");

        APIEndpointStats apiEndpointStats = new APIEndpointStats("/resorts", "GET", 11, 198);
        ArrayList<APIEndpointStats> list = new ArrayList<>(Arrays.asList(apiEndpointStats));
        APIStats apiStats = new APIStats(list);
        ArrayList<APIEndpointStats> result = apiStats.getEndpointStats();

        String apistatsJsonString = this.gson.toJson(result);

        PrintWriter out = res.getWriter();
        res.setCharacterEncoding("UTF-8");
        out.print(apistatsJsonString);
        out.print(responseMsg);
        out.flush();

    }

}
