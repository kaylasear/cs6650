package assignment4;



import org.apache.commons.lang3.concurrent.EventCountCircuitBreaker;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;


public class Client {

    private static String url = "http://";
    private static final String port = ":8080";
    private static final String webapp = "/assignment1";
    private static String seasonId = "2022";
    private static String dayId="1";
    private static Integer resortId = 10;

    private static final int MINLIFTS = 5;
    private static final int MAXLIFTS = 60;
    private static final int MAXMEANLIFTS = 20;
    private static final int MAXTHREADS = 1024;
    private static final int MAXSKIERS = 100000;
    private static final double START_POST_VARIABLE = 0.2;
    private static final double PEAK_POST_VARIABLE = 0.6;
    private static final double COOL_POST_VARIABLE = 0.1;

    private static int NUMTHREADS;
    private static int NUMSKIERS;
    private static int NUMLIFTS = 40;
    private static int NUMRUNS = 10;
    private static String SERVERADDRESS = null;

    private int totalSuccess;
    private int expectedRequests;

    private int peakRequests = 0;
    private Logger LOGGER = Logger.getLogger(Client.class.getName());
    private EventCountCircuitBreaker breaker = new EventCountCircuitBreaker(1800, 1, TimeUnit.SECONDS, 500);

    public static void main(String[] args) throws InterruptedException, IOException, TimeoutException {
        final Client rmw = new Client();
        validateArguments(args);

        url = url + SERVERADDRESS + port + webapp;

        long start = System.currentTimeMillis();
        rmw.executePeakPhase();
        long finish = System.currentTimeMillis();
        long wallTime = ((finish - start) / 1000);

        System.out.println("Threads: " + NUMTHREADS);
        System.out.println("---------------------------------------");
        System.out.println("Total successful requests: " + rmw.getSuccess());
        System.out.println("Total failed requests: " + (rmw.expectedRequests - rmw.getSuccess()));
        System.out.println("Wall Time in seconds: " + wallTime);

        double throughout = (double) rmw.getSuccess() / wallTime;
        System.out.println("Total throughput (req/sec): " + throughout);
    }

    public synchronized void inc() {
        totalSuccess++;
    }

    public synchronized int getSuccess() {
        return totalSuccess;
    }

    private synchronized void incCurrent() {
        peakRequests++;
    }

    private synchronized int getCurrent() {
        return peakRequests;
    }

    private static void validateArguments(String[] args) {
        Integer threads = Integer.parseInt(args[0]);
        Integer skiers = Integer.parseInt(args[1]);
        Integer lifts = Integer.parseInt(args[2]);
        Integer meanLifts = Integer.parseInt(args[3]);

        try {
            if (threads <= MAXTHREADS) {
                NUMTHREADS = threads;
            }
            if (skiers <= MAXSKIERS) {
                NUMSKIERS = skiers;
            }
            if (MINLIFTS <= lifts && lifts <= MAXLIFTS) {
                NUMLIFTS = lifts;
            }
            if (meanLifts <= MAXMEANLIFTS) {
                NUMRUNS = meanLifts;
            }
            SERVERADDRESS = args[4];
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void executeStartupPhase() throws IOException, InterruptedException {
        int startSkierId = 1;
        int start = 1;
        int end = 90;
        int startupThreads = NUMTHREADS/4;
        CountDownLatch completed = new CountDownLatch((NUMTHREADS/4));

        if (startupThreads == 0) {
            startupThreads = 1;
        }
        double range = ((double)NUMSKIERS/(double)startupThreads);
        double maxCalls = (((double)NUMRUNS * START_POST_VARIABLE) * (range));
        int multiplier = 1;
        expectedRequests += (maxCalls*startupThreads);

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(NUMTHREADS/2);

        // set max connections per route to num threads
        cm.setDefaultMaxPerRoute(NUMTHREADS/2);
        CloseableHttpClient httpClient = HttpClients.custom().setRetryHandler(requestRetryHandler).setConnectionManager(cm).build();

        for (int i = 0; i < startupThreads; i++) {
            int endSkierId = (int)range * multiplier;
            AtomicInteger counter = new AtomicInteger(1);

            int finalStartSkierId = startSkierId;

            int finalStartupThreads = startupThreads;
            Runnable thread = () -> {
                try {

                    while (counter.get() <= maxCalls) {
                        // execute the POST requests
                        executePost(httpClient, finalStartSkierId, endSkierId, start, end);

                        int current = getSuccess();

                        // 20% requests done, signal the Peak threads to start
                        if (current == (Math.round(maxCalls * finalStartupThreads * 0.2))) {
                            executePeakPhase();
                        }
                        counter.getAndIncrement();
                    }
                } catch (IOException | InterruptedException | PeakLoadException e) {
                    e.printStackTrace();
                } finally {
                    // we've finished - let the main thread know
                    System.out.println("shutting start");
                    completed.countDown();
                }
            };
            new Thread(thread).start();
            multiplier += 1;
            startSkierId = endSkierId + 1;
        }
        completed.await();
    }

    private void executePeakPhase() throws IOException, InterruptedException {
        int startSkierId = 1;
        int start = 91;
        int end = 360;
        double range = ((double)NUMSKIERS/(double)NUMTHREADS);
        double maxCalls = (((double)NUMRUNS * PEAK_POST_VARIABLE) * (range));
        int multiplier = 1;
        expectedRequests += ((maxCalls*NUMTHREADS));
        CountDownLatch completed = new CountDownLatch(NUMTHREADS);

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        //increase max total connections to num threads
        cm.setMaxTotal(NUMTHREADS*2);

        // set max connections per route to num threads
        cm.setDefaultMaxPerRoute(NUMTHREADS*2);
        CloseableHttpClient httpClient = HttpClients.custom().setRetryHandler(requestRetryHandler).setConnectionManager(cm).build();

        for (int i = 0; i < NUMTHREADS; i++) {
            int endSkierId = (int)range * multiplier;
            AtomicInteger counter = new AtomicInteger(1);
            int finalStartSkierId = startSkierId;

            int finalI = i;
            Runnable thread = () -> {
                try {
                    // wait for the start up phase to signal us
                    //startPeak.await();
                    //System.out.println("starting peak");
                    while (counter.get() <= maxCalls) {
                        // execute the POST request
//                        executePost(httpClient, finalStartSkierId, endSkierId, start, end);
                        executeGet(httpClient);
                        incCurrent();
//                        if (getCurrent() == Math.round(maxCalls * NUMTHREADS * 0.2)) {
//                            // System.out.println("starting cool");
//                            executeCooldownPhase();
//                        }
                        counter.getAndIncrement();
                    }

                } catch (IOException e) {
                } finally {
                    // we've finished - let the main thread know
                    System.out.println("shutting peak");
                    completed.countDown();
                }
            };
            new Thread(thread).start();
            multiplier += 1;
            startSkierId = endSkierId + 1;
        }
        completed.await();
    }

    private void executeGet(CloseableHttpClient httpClient) throws IOException {
        String localVarPath = "/resorts/10/seasons/2022/day/1/skiers";
        String url = buildURL(localVarPath);

//        HttpGet method = new HttpGet(localVarPath);
//        HttpContext context = new BasicHttpContext();

        try
        {
            HttpGet httpget = new HttpGet(url);
            BasicResponseHandler responseHandler = new BasicResponseHandler();//Here is the change
            String responseBody = httpClient.execute(httpget, responseHandler);
            System.out.println(responseBody);
        }
        catch (ClientProtocolException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }


    private void executeCooldownPhase() throws IOException, InterruptedException {
        int startSkierId = 1;
        int start = 361;
        int end = 420;
        int range = (int) (NUMSKIERS / (NUMTHREADS * 0.10));
        double maxCalls = (((double)NUMRUNS * COOL_POST_VARIABLE));
        int multiplier = 1;
        expectedRequests += (maxCalls*Math.round(NUMTHREADS*0.10));
        CountDownLatch completed = new CountDownLatch((int) (NUMTHREADS*0.10));

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

        //increase max total connections to num threads
        cm.setMaxTotal((int)(NUMTHREADS/2));

        // set max connections per route to num threads
        cm.setDefaultMaxPerRoute(((int)(NUMTHREADS/2)));
        CloseableHttpClient httpClient = HttpClients.custom().setRetryHandler(requestRetryHandler).setConnectionManager(cm).build();

        for (int i = 0; i < (NUMTHREADS * 0.10); i++) {
            int endSkierId = range * multiplier;
            AtomicInteger counter = new AtomicInteger(1);

            int finalStartSkierId = startSkierId;
            Runnable thread = () -> {
                try {

                    // wait for the peak thread to tell us to start
                   // startCool.await();
                    while (counter.get() <= maxCalls) {

                        executePost(httpClient, finalStartSkierId, endSkierId, start, end);
                        counter.getAndIncrement();
                    }
                } catch (InterruptedException | IOException | PeakLoadException e) {
                } finally {
                    // we've finished - let the main thread know
                    System.out.println("shutting cool");
                    completed.countDown();
                }
            };
            new Thread(thread).start();
            multiplier += 1;
            startSkierId = endSkierId + 1;
        }
        completed.await();
    }


    public void executePost(CloseableHttpClient client, int startSkierId, int endSkierId, int startTime, int endTime) throws IOException, InterruptedException, PeakLoadException {
        // generate random values for skierId, liftId, time, waitTime
        int skierId = generateRandomValue(startSkierId, endSkierId);
        int liftId = generateRandomValue(0, NUMLIFTS);
        int time = generateRandomValue(startTime, endTime);
        int waitTime = generateRandomValue(0, 10);
        //int dayId = generateRandomValue(0, 360);
        //int resortId = generateRandomValue(0, 10);

        // create path and map variables
        String localVarPath = "/skiers/{resortId}/seasons/{seasonId}/days/{dayId}/skiers/{skierId}"
                .replaceAll("\\{resortId\\}", String.valueOf(resortId))
                .replaceAll("\\{seasonId\\}", seasonId)
                .replaceAll("\\{dayId\\}", String.valueOf(dayId))
                .replaceAll("\\{" + "skierId" + "\\}", String.valueOf(skierId));

        // build url
        String newUrl = buildURL(localVarPath);

        // json formatted data
        String json = new StringBuilder()
                .append("{")
                .append("\"time\":" + time + " ,")
                .append("\"liftId\":" + liftId + " ,")
                .append("\"waitTime\":" + waitTime)
                .append("}").toString();

        // execute POST method
        HttpPost method = new HttpPost(newUrl);
        method.setEntity(new StringEntity(json));

        // check circuit breaker state
        if (breaker.incrementAndCheckState()) {
            CloseableHttpResponse response = client.execute(method);

            try {
                int status = response.getStatusLine().getStatusCode();

                HttpEntity entity = response.getEntity();

                // print response body
                if (entity != null) {
                    inc();
                    // return it as a String
                    String result = EntityUtils.toString(entity);
                    System.out.println(status);
                }
                EntityUtils.consume(entity);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                response.close();
                method.releaseConnection();
            }
        } else { // throw error code if peak load hit
            throw new PeakLoadException("peak load, cannot send request");
        }
    }

    HttpRequestRetryHandler requestRetryHandler = new HttpRequestRetryHandler() {
        @Override
        public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
            return executionCount < 5;
        }
    };

    /**
     * Helper method to build URL path
     * @param localVarPath
     * @return new url
     */
    private String buildURL(String localVarPath) {
        String base = url;
        String newUrl = base + localVarPath;
        return newUrl;
    }


    /**
     * Helper method to generate random value
     * @param startValue
     * @param endValue
     * @return a random value
     */
    private int generateRandomValue(int startValue, int endValue) {
        SecureRandom random = new SecureRandom();
        int n = random.nextInt((endValue-startValue)+1)+startValue;
        return n;
    }

}