package assignment4.servlet;



import assignment4.model.*;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;


public class SkierServlet extends HttpServlet {

    private final static int seasonParam = 2;
    private final static int dayParam = 4;
    private final static int skierParam = 6;
    private final static int verticalParam = 2;
    private final static int urlPathVerticalLength = 3;

    private static final String HOST_ADDRESS = "52.13.90.18"; // rabbitmq ec2 instance
    private static final int PORT = 5672;
    private static final int NUM_THREADS = 128;
    private static final String EXCHANGE_NAME = "logs";

    private Gson gson = new Gson();
    private static String QUEUE_NAME = "queue";
    private Connection connection;
    private Channel channel;

    BlockingQueue<Channel> blockingQueue;
    private Logger LOGGER = Logger.getLogger(SkierServlet.class.getName());
    private final static String REDIS_HOST_NAME = "34.219.28.162";
    private static JedisPool pool;

    public SkierServlet() {
    }

    /**
     * Set up connection to RabbitMQ
     */
    @Override
    public void init(){
        blockingQueue = new LinkedBlockingQueue<>(NUM_THREADS);

        ConnectionFactory factory = new ConnectionFactory();

        factory.setUsername("admin");
        factory.setPassword("pass");

        factory.setHost(HOST_ADDRESS);
        factory.setPort(PORT);

        // connect to redis
        connectToDatabase();

        try {
            connection = factory.newConnection();

            // add channels to blocking queue
            for (int i = 0; i < NUM_THREADS; i++) {
                channel = connection.createChannel();
                channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
                //channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                blockingQueue.add(channel);
            }

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
        }
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
     * POST method - validate request and URL, create JSON message, and send message to queue
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
            return;
        }

        String[] urlParts = urlPath.split("/");
        // and now validate url path & JSON payload and return the response status code
        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            res.setStatus(HttpServletResponse.SC_OK);

            Skier skier = processRequest(req, res, urlParts);
            try {
                // send message to queue
                sendMessage(skier.toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Send lift message to RabbitMQ
     * @param message
     * @throws InterruptedException
     */
    public void sendMessage(String message) throws InterruptedException {
        try {
            // get a channel from pool
            Channel channel = blockingQueue.poll(0, TimeUnit.MILLISECONDS);
            channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes(StandardCharsets.UTF_8));
            LOGGER.info(" [x] Sent '" + message + "'");

            // return channel back to pool for reuse
            blockingQueue.add(channel);
        } catch (Exception e) {
            LOGGER.info("Failed to send message to queue");
        }

    }

    public void close() throws IOException {
        connection.close();
    }


    /**
     * Process JSON request body. Convert JSON object ot Java object
     * @param request
     * @param response
     * @param urlParts
     * @throws ServletException
     * @throws IOException
     */
    private Skier processRequest(HttpServletRequest request, HttpServletResponse response, String[] urlParts)
            throws IOException {
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

            // collect variables from url
            int resortId = Integer.parseInt(urlParts[1]);
            String seasonId = urlParts[seasonParam+1];
            String dayId = urlParts[dayParam+1];
            int skierId = Integer.valueOf(urlParts[skierParam+1]);

            // build skier object with liftride info
            Skier skier1 = new Skier(resortId, seasonId, dayId, skierId, liftRide);

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.setCharacterEncoding("UTF-8");
            out.println(skier.toString());
            responseMsg.setMessage("Write successful");
            out.println(responseMsg);
            out.flush();
            return skier1;
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write(e.getMessage());
        }
        return null;
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
     *  urlPath = GET/skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
     * @param res
     * @param req
     * @param resortId
     * @param seasonId
     * @param dayId
     * @param skierId
     */
    private void getSkierDayVertical(HttpServletResponse res, HttpServletRequest req, Integer resortId, String seasonId, String dayId, Integer skierId) throws IOException {
        Jedis jedis = null;

        try {
            jedis = pool.getResource();
            String skierIdStr = String.valueOf(skierId);
            String dayField = "day-"+dayId;
            ResponseMsg responseMsg = null;

            //check if the skierID entry is present for the skierID
            Integer sumTotalVertical = null;
            if(jedis.exists(skierIdStr)) {
                //check if dayID is present
                if (jedis.hexists(skierIdStr, dayField)) {
                    sumTotalVertical = Integer.parseInt(jedis.hget(skierIdStr, dayField));
                    //else return appropriate API message
                } else {
                    responseMsg = new ResponseMsg("Day Id does not exist for the skier");
                }
                //handle case where skierID is not there in DB
            }else{
                responseMsg = new ResponseMsg("Skier ID does not exist");
            }
            PrintWriter out = res.getWriter();
            res.setCharacterEncoding("UTF-8");
            if(sumTotalVertical != null){
                out.println(sumTotalVertical);
            }else{
                out.println(responseMsg);
            }
            out.flush();
        } catch (JedisException e) {
            if (jedis != null) {
                // if error, return it back to pool
                pool.returnBrokenResource(jedis);
                jedis = null;
            }
            e.printStackTrace();
        } finally {
           pool.returnResource(jedis);
        }

    }

    /**
     * Get the total vertical for the skier for the specified season at specified resort. If no season,
     * return full list
     * urlPath = GET/skiers/{skierId}/vertical
     * @param res
     * @param req
     * @param id
     */
    private void getSkierResortTotal(HttpServletResponse res, HttpServletRequest req, Integer id) throws IOException {
        Jedis jedis = pool.getResource();
        ResponseMsg responseMsg = null;
        String resultString = null;
        try {
            String skierId = String.valueOf(id);
            Integer seasonID = 2022;
            if (jedis.exists(skierId)) {
                List<String> verticalTotalsForSkierIDList = jedis.hvals(skierId);
                Integer sumTotal = 0;
                for (String skierTotal : verticalTotalsForSkierIDList) {
                    sumTotal += Integer.parseInt(skierTotal);
                }
                SkierVertical skierVerticalResponse = new SkierVertical();
                skierVerticalResponse.addVertical(seasonID.toString(), sumTotal);
                resultString = this.gson.toJson(skierVerticalResponse);
            } else {
                responseMsg = new ResponseMsg("SkierID does not exist");
            }

            PrintWriter out = res.getWriter();
            res.setCharacterEncoding("UTF-8");
            if(resultString != null){
                out.println(resultString);
            }else{
                out.println(responseMsg);
            }
            out.flush();
        } catch (JedisException e) {
            if (jedis != null) {
                // if error, return it back to pool
                pool.returnBrokenResource(jedis);
                jedis = null;
            }
        } finally {
            pool.returnResource(jedis);
        }

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


}