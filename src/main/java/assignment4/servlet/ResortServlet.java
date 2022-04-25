package assignment4.servlet;

import assignment1.part1.model.*;
import com.google.gson.Gson;
import com.rabbitmq.client.ConnectionFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;


public class ResortServlet extends HttpServlet {
    private final static int resortParam = 1;
    private final static int seasonParam = 2;
    private final static int dayParam = 4;
    private final static int skierParam = 6;
    private final static int urlPathResortsLength = 3;
    private static final int NUM_THREADS = 128;


    private Gson gson = new Gson();
    private final static String REDIS_HOST_NAME = "34.210.142.4";
    private static JedisPool pool;


    @Override
    public void init(){
        connectToDatabase();
    }

    /**
     * Connect and configure to the database. Thread-safe configurations from
     * https://www.baeldung.com/jedis-java-redis-client-library
     */
    private static void connectToDatabase() {
        pool = new JedisPool(REDIS_HOST_NAME , 6379);

        pool.setMaxTotal(NUM_THREADS);
        pool.setBlockWhenExhausted(true);
        pool.setMaxIdle(NUM_THREADS);
        pool.setMinIdle(16);
        pool.setMinEvictableIdle(Duration.ofMillis(60000));
        pool.setTimeBetweenEvictionRuns(Duration.ofMillis(30000));
        pool.setNumTestsPerEvictionRun(3);
        pool.setTestOnBorrow(true);
        pool.setTestOnReturn(true);
        pool.setTestWhileIdle(true);

        System.out.println("Connection successful");
    }

    /**
     * Get a list of ski resorts in the database. Get number of
     * unique skiers are resort/season/day. Get list of seasons for the specified resort
     * @param req
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        String urlPath = req.getPathInfo();
        ResponseMsg responseMsg = new ResponseMsg();
        PrintWriter out = res.getWriter();

        if (urlPath != null) {
            String[] urlParts = urlPath.split("/");

            // and now validate url path and return the response status code
            // (and maybe also some value if input is valid)
            if (!isUrlValid(urlParts)) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                responseMsg.setMessage("Resort not found");

            } else {
                res.setStatus(HttpServletResponse.SC_OK);

                // do any sophisticated processing with urlParts which contains all the url params
                responseMsg.setMessage("Success!");

                Integer resortId = Integer.valueOf(urlParts[resortParam]);

                // /resorts/{resortId}/seasons
                // call method to return seasons list of specified resort
                if (urlParts.length == urlPathResortsLength) {
                    System.out.println(resortId);
                    getSeasonsByResortId(res, req, resortId);
                } else {
                    // resorts/{resortId}/seasons/{seasonID}/day/{dayId}/skiers
                    // call method to get total # of skiers at resort/season/day
                    String seasonId = urlParts[seasonParam +1];
                    String dayId = urlParts[dayParam+1];
                    System.out.println(dayId);
                    getTotalSkiersAtResort(res, req, resortId, seasonId, dayId);
                }
            }
        } else {  // url path is empty, return all resorts
            res.setStatus(HttpServletResponse.SC_OK);
            responseMsg.setMessage("Success!");

            ResortsList resortsList = new ResortsList();
            // dummy data
            resortsList.addResort("Mission Ridge", 1);
            resortsList.addResort("Snohomish", 2);
            ArrayList<Resort> list = resortsList.getResorts();

            String listJsonString = this.gson.toJson(list);

            res.setCharacterEncoding("UTF-8");
            out.println(listJsonString);
        }
        out.println(responseMsg);
        out.flush();
    }

    /**
     * Get total number of skiers at specified resort, season, day
     * urlPath = GET/resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
     * @param res
     * @param req
     * @param resortId
     * @param seasonId
     * @param dayId
     */
    public void getTotalSkiersAtResort(HttpServletResponse res, HttpServletRequest req, Integer resortId, String seasonId, String dayId) throws IOException {
        Jedis jedis = null;
        try {
           jedis = pool.getResource();
            String resortIDStr = String.valueOf(resortId);
            ResponseMsg responseMsg = null;
            String key = "resort-"+resortIDStr+"-day-"+dayId;
            Set<String> skierDataSet = new HashSet<>();
            Integer size = 0;
            ResortSkier resortSkier = null;

            //check set/entry for resortID/dayID exists
            if(jedis.exists(key)){
                skierDataSet = jedis.smembers(key);
                size = skierDataSet.size();
                resortSkier = new ResortSkier("Mission Ridge",size);
                //else return appropriate API message
            } else {
                responseMsg = new ResponseMsg("Resort ID/Day ID entry does not exist");
            }

            PrintWriter out = res.getWriter();
            res.setCharacterEncoding("UTF-8");
            if(size != 0){
                out.println(resortSkier);
            }else{
                out.println(responseMsg);
            }
            out.flush();
        } catch (JedisException e) {
            e.printStackTrace();
        } finally {
            pool.returnResource(jedis);
        }
    }

    public SeasonsList getSeasonsByResortId(HttpServletResponse res, HttpServletRequest req, Integer resortId) throws IOException {
        SeasonsList seasonsList = new SeasonsList();
        // dummy data
        seasonsList.addSeason("2018");
        seasonsList.addSeason("2019");
        ArrayList<String> seasons = seasonsList.getSeasons();

        String seasonsJsonString = this.gson.toJson(seasons);
        PrintWriter out = res.getWriter();
        res.setCharacterEncoding("UTF-8");
        out.print(seasonsJsonString);
        out.flush();
        return seasonsList;
    }

    private boolean isUrlValid(String[] urlPath) {
        if (urlPath.length == urlPathResortsLength) {
            if (urlPath[seasonParam].equals("seasons"))
                return true;
        }
        if (urlPath[seasonParam].equals("seasons") && urlPath[dayParam].equals("day") && urlPath[skierParam].equals("skiers"))
            return true;
        return false;
    }

    /**
     * Add a new season for a resort
     * @param req
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String urlPath = req.getPathInfo();

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing parameters");
            return;
        }

        String[] urlParts = urlPath.split("/");
        // and now validate url path and return the response status code
        // (and maybe also some value if input is valid)

        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            // do any sophisticated processing with urlParts which contains all the url params
            // TODO: process url params in `urlParts`
            processRequest(req, res);
        }
    }

    /**
     * Process JSON request body. Convert JSON object ot Java object
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response)
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

            String s = sb.toString().replace("}", "");
            String[] strings = s.split(" ");

            ArrayList<String> seasonList = new ArrayList<String>(Arrays.asList(strings));
            SeasonsList season = new SeasonsList();
            season.addSeason(seasonList.get(seasonList.size()-1));
//            SeasonsList season = (SeasonsList) gson.fromJson(sb.toString(), SeasonsList.class);

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.setCharacterEncoding("UTF-8");
            out.println(season);
            responseMsg.setMessage("New season created");
            out.println(responseMsg);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write(e.getMessage());
        }

    }
}