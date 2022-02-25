package assignment2.servlet;

import assignment1.part1.model.LiftRide;
import assignment1.part1.model.ResponseMsg;
import assignment1.part1.model.SkierVertical;
import assignment1.part1.model.SkierVerticalResorts;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.google.gson.Gson;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;


public class SkierServlet extends HttpServlet {
    private final static int seasonParam = 2;
    private final static int dayParam = 4;
    private final static int skierParam = 6;
    private final static int verticalParam = 2;
    private final static int urlPathVerticalLength = 3;

    private Gson gson = new Gson();

    public SkierServlet() {
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        String urlPath = req.getPathInfo();
        ResponseMsg responseMsg = new ResponseMsg();
        PrintWriter out = res.getWriter();

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            responseMsg.setMessage("Data not found");
            return;
        }

        String[] urlParts = urlPath.split("/");
        // and now validate url path and return the response status code
        // (and maybe also some value if input is valid)


        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMsg.setMessage("Invalid inputs supplied");
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            responseMsg.setMessage("Successful operation - total vertical for the day returned");
            // do any sophisticated processing with urlParts which contains all the url params

            if (urlParts.length == urlPathVerticalLength) {
                Integer skierId = Integer.valueOf(urlParts[seasonParam-1]);
                // call method to return the total vertical for the skier at the specified resort
                getSkierResortTotal(res, req, skierId);

            } else {
                Integer resortId = Integer.valueOf(urlParts[seasonParam-1]);
                String seasonId = urlParts[seasonParam+1];
                String dayId = urlParts[dayParam+1];
                Integer skierId = Integer.valueOf(urlParts[skierParam+1]);
                // return the ski day vertical for a skier for the specified ski day
                getSkierDayVertical(res, req, resortId, seasonId, dayId, skierId);

            }
        }
        out.println(responseMsg);
        out.flush();
    }

    /**
     * Get the ski day vertical for a skier for the specified ski day
     * @param res
     * @param req
     * @param resortId
     * @param seasonId
     * @param dayId
     * @param skierId
     */
    private void getSkierDayVertical(HttpServletResponse res, HttpServletRequest req, Integer resortId, String seasonId, String dayId, Integer skierId) throws IOException {
        SkierVerticalResorts skierVerticalResorts = new SkierVerticalResorts(seasonId, 34507); // dummy data
        Integer totalVert = skierVerticalResorts.getTotalVert();

        String resultString = this.gson.toJson(totalVert);
        PrintWriter out = res.getWriter();
        res.setCharacterEncoding("UTF-8");
        out.println(resultString);
        out.flush();

    }

    /**
     * Get the total vertical for the skier for the specified season at specified resort. If no season,
     * return full list
     * @param res
     * @param req
     * @param id
     */
    private void getSkierResortTotal(HttpServletResponse res, HttpServletRequest req, Integer id) throws IOException {
        //dummy data
        SkierVertical skierVertical = new SkierVertical();
        skierVertical.addVertical("2019", 34507);

        ArrayList<SkierVerticalResorts> result = skierVertical.getResorts();

        String resultString = this.gson.toJson(result);
        PrintWriter out = res.getWriter();
        res.setCharacterEncoding("UTF-8");
        out.println(resultString);
        out.flush();

    }


    private boolean isUrlValid(String[] urlPath) {
        // urlPath = "skiers/{skierId}/vertical
        if (urlPath.length == urlPathVerticalLength) {
            if (urlPath[verticalParam].equals("vertical"))
            return true;
        } else {
            // urlPath  = "/1/seasons/2019/day/1/skier/123"
            // urlParts = [, 1, seasons, 2019, days, 1, skiers, 123]
            if (urlPath[seasonParam].equals("seasons") && urlPath[dayParam].equals("days") && urlPath[skierParam].equals("skiers"))
                return true;
        }
        return false;
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String urlPath = req.getPathInfo();

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String[] urlParts = urlPath.split("/");
        // and now validate url path & JSON payload and return the response status code
        if (!isUrlValid(urlParts) || !validateJson(req)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            // do any sophisticated processing with urlParts which contains all the url params
            // TODO: format incoming data and send it as a payload to queue
            processRequest(req, res, urlParts);

        }
    }

    /**
     * Validate the JSON data from request body according to JSON schema
     * @param req
     * @return true if no errors, false otherwise
     * @throws IOException
     */
    private boolean validateJson(HttpServletRequest req) throws IOException {
        ResponseMsg responseMsg = new ResponseMsg();
        BufferedReader jsonData = req.getReader();

       try {

           StringBuilder sb = new StringBuilder();
           String line;
           while( (line = jsonData.readLine()) != null) {
               sb.append(line);
           }

           JsonNode data = JsonLoader.fromString(String.valueOf(sb));
           // TODO: import schema from swagger?
           String jsonSchema = null;
           JsonNode schema = JsonLoader.fromString(jsonSchema);

           JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
           JsonValidator validator = factory.getValidator();

           ProcessingReport report = validator.validate(schema, data);
           //System.out.println(report);

           if (!report.toString().contains("success")) {
               responseMsg.setMessage("There was a problem");
               return false;
           }
       } catch (IOException | ProcessingException e) {
           e.printStackTrace();
           responseMsg.setMessage("Failed to validate json data");
           System.out.println(responseMsg);
           return false;
       }
        return true;
    }

    /**
     * Process JSON request body. Convert JSON object ot Java object
     * @param request
     * @param response
     * @param urlParts
     * @throws ServletException
     * @throws IOException
     * @return
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response, String[] urlParts)
            throws ServletException, IOException {
        response.setContentType("application/json");
        Gson gson = new Gson();
        ResponseMsg responseMsg = new ResponseMsg();
        PrintWriter out = response.getWriter();

        BufferedReader skier = request.getReader();

        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while( (line = skier.readLine()) != null) {
                sb.append(line);
            }

            LiftRide liftRide = gson.fromJson(sb.toString(), LiftRide.class);

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.setCharacterEncoding("UTF-8");
            out.println(liftRide.toString());
            responseMsg.setMessage("Write successful");
            out.println(responseMsg);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write(e.getMessage());
        }
    }
}
