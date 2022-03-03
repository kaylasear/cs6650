package assignment2.servlet;



import assignment2.ThreadObjectFactory;
import assignment2.model.LiftRide;
import assignment2.model.ResponseMsg;
import assignment2.model.SkierVertical;
import assignment2.model.SkierVerticalResorts;
import com.google.gson.Gson;

import com.rabbitmq.client.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;


import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Logger;


public class SkierServlet extends HttpServlet {

    private final static int seasonParam = 2;
    private final static int dayParam = 4;
    private final static int skierParam = 6;
    private final static int verticalParam = 2;
    private final static int urlPathVerticalLength = 3;
    private static final String HOST_ADDRESS = "localhost";
    private static final int PORT = 5672;
    private static int threadNumber = 128;

    private Gson gson = new Gson();
    private static String QUEUE_NAME = "queue";
    private Connection connection;
    private Channel channel;

    private ObjectPool pool;
    private Logger LOGGER = Logger.getLogger(SkierServlet.class.getName());

    public SkierServlet() {
    }

    public void init(){
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(1);

        ConnectionFactory factory = new ConnectionFactory();

//        factory.setUsername("admin");
//        factory.setPassword("pass");

        factory.setHost(HOST_ADDRESS);
        factory.setPort(PORT);

        pool = new GenericObjectPool<Channel>(new ThreadObjectFactory(), poolConfig);

        try {
            connection = factory.newConnection();

            channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
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
        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            // do any sophisticated processing with urlParts which contains all the url params
            // TODO: format incoming data and send it as a payload to queue
//            LiftRide liftRide = processRequest(req, res, urlParts);
            LiftRide liftRide = processRequest(req, res, urlParts);
            try {
                sendMessage(liftRide.toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) throws IOException, InterruptedException {
        try {
            // get a channel from pool
            Channel channel = (Channel) pool.borrowObject();
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            LOGGER.info(" [x] Sent '" + message + "'");

            // return channel back to pool for reuse
            pool.returnObject(channel);
        } catch (Exception e) {
            LOGGER.info("Failed to send message to queue");
        }

    }

    public void close() throws IOException {
        connection.close();
    }

//    /**
//     * Send Json object to queue
//     * @param message - lift ride object
//     * @return
//     */
//    public boolean sendMessageToQueue(LiftRide message) throws IOException {
//        ResponseMsg responseMsg = new ResponseMsg();
//
//        try {
//            Channel channel = pool.borrowObject();
//            channel.basicPublish(message.toString(), QUEUE_NAME, null, message.toString().getBytes(StandardCharsets.UTF_8));
//            pool.returnObject(channel);
//            responseMsg.setMessage("Successfully sent message to RabbitMQ");
//            System.out.println(responseMsg);
//            return true;
//        } catch (Exception e) {
//            responseMsg.setMessage("Failed to send message to RabbitMQ");
//            System.out.println(responseMsg);
//            return false;
//        }
//    }

    /**
     * Process JSON request body. Convert JSON object ot Java object
     * @param request
     * @param response
     * @param urlParts
     * @throws ServletException
     * @throws IOException
     */
    private LiftRide processRequest(HttpServletRequest request, HttpServletResponse response, String[] urlParts)
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
            return liftRide;
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


}
