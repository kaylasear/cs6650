package assignment1.part1;

import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;


public class Client {

    private static String url = "http://";
    private static final String port = ":8080";
    private static final String webapp = "/assignment1";
    private static String dayId = "5";
    private static String seasonId = "2019";
    private static String resortId = "1";

    private static final int MINLIFTS = 5;
    private static final int MAXLIFTS = 60;
    private static final int MAXMEANLIFTS = 20;
    private static final int MAXTHREADS = 1024;
    private static final int MAXSKIERS = 100000;
    private static final double START_POST_VARIABLE = 0.2;
    private static final double PEAK_POST_VARIABLE = 0.6;
    private static final double COOL_POST_VARIABLE = 0.1;

    private static final int MINERRORCODE = 299;

    private static int NUMTHREADS;
    private static int NUMSKIERS;
    private static int NUMLIFTS = 40;
    private static int NUMRUNS = 10;
    private static String SERVERADDRESS = null;

    private int totalSuccess;
    private int totalFailed = 0;

    private final CountDownLatch startPeak = new CountDownLatch(1);
    private final CountDownLatch startCool = new CountDownLatch(1);

    private CountDownLatch endSignal;
    private CountDownLatch endPeak;
    private CountDownLatch endCool;
    private int peakRequests = 0;
    private Logger LOGGER = Logger.getLogger(Client.class.getName());
    private int expectedRequests;



    public static void main(String[] args) throws InterruptedException, IOException {
        final Client rmw = new Client();
        validateArguments(args);

        url = url + SERVERADDRESS + port + webapp;

        rmw.endSignal = new CountDownLatch((NUMTHREADS/4));
        rmw.endPeak = new CountDownLatch(NUMTHREADS);
        rmw.endCool = new CountDownLatch((int) Math.round(NUMTHREADS * 0.10));

        long start = System.currentTimeMillis();

        rmw.executeStartupPhase();
        rmw.executePeakPhase();
        rmw.executeCooldownPhase();

        rmw.endSignal.await();
        rmw.endPeak.await();
        rmw.endCool.await();
        long finish = System.currentTimeMillis();
        long wallTime = ((finish - start) / 1000);

        System.out.println("Total successful requests: " + (rmw.getSuccess()));
        System.out.println("Total failed requests: " + (rmw.expectedRequests - rmw.getSuccess()));
        System.out.println("Wall Time in seconds: " + wallTime);

        double throughout = (double) rmw.totalSuccess / wallTime;
        System.out.println("Total throughput: " + throughout);

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

    private synchronized void incFail() {
        totalFailed++;
    }

    private int getFail() {
        return totalFailed;
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

    public void executeStartupPhase() throws IOException {
        int startSkierId = 1;
        int start = 1;
        int end = 90;
        int startupThreads = NUMTHREADS / 4;

        if (startupThreads == 0) {
            startupThreads = 1;
        }
        int range = Math.round(NUMSKIERS / startupThreads);
        int maxCalls = (int) ((NUMRUNS * START_POST_VARIABLE) * (range));
        int multiplier = 1;
        expectedRequests = expectedRequests + (maxCalls*startupThreads);

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        //increase max total connections to num threads
        cm.setMaxTotal(startupThreads);

        // set max connections per route to num threads
        cm.setDefaultMaxPerRoute(startupThreads);


        for (int i = 0; i < startupThreads; i++) {

            int endSkierId = range * multiplier;
            AtomicInteger counter = new AtomicInteger(1);

            int finalStartSkierId = startSkierId;

            int finalStartupThreads = startupThreads;
            Runnable thread = () -> {
                try {
                    CloseableHttpClient httpClient = HttpClients.custom().setRetryHandler(new HttpRequestRetryHandler() {
                        @Override
                        public boolean retryRequest(IOException e, int i, HttpContext httpContext) {
                            if (i > 3) {
                                LOGGER.warning("Maximum tries reached for client http pool ");
                                return false;
                            }
                            if (e instanceof NoHttpResponseException) {
                                LOGGER.warning("No response from server on " + i + " call");
                                return true;
                            }
                            return false;
                        }
                    }).build();
//                    CloseableHttpClient httpclient = HttpClients.createDefault();
                    while (counter.get() <= maxCalls) {
                        // execute the POST requests
                        executePost(httpClient, finalStartSkierId, endSkierId, start, end);

                        int current = getSuccess();
                        // 20% requests done, signal the Peak threads to start
                        if (current == (Math.round(maxCalls * finalStartupThreads * 0.2))) {
                            startPeak.countDown();
                        }
                        counter.getAndIncrement();
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    // we've finished - let the main thread know
                    System.out.println("shutting start");
                    endSignal.countDown();
                }
            };
            new Thread(thread).start();
            multiplier += 1;
            startSkierId = endSkierId + 1;

        }
    }

    private void executePeakPhase() throws IOException {
        int startSkierId = 1;
        int start = 91;
        int end = 360;
        int range = (NUMSKIERS / NUMTHREADS);
        int maxCalls = (int) ((NUMRUNS * PEAK_POST_VARIABLE) * (range));
        int multiplier = 1;
        expectedRequests = expectedRequests + (maxCalls*NUMTHREADS);

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        //increase max total connections to num threads
        cm.setMaxTotal(NUMTHREADS);

        // set max connections per route to num threads
        cm.setDefaultMaxPerRoute(NUMTHREADS);

        for (int i = 0; i < NUMTHREADS; i++) {

            int endSkierId = range * multiplier;
            AtomicInteger counter = new AtomicInteger(1);
            int finalStartSkierId = startSkierId;

            int finalI = i;
            Runnable thread = () -> {
                try {
                    CloseableHttpClient httpClient = HttpClients.custom().setRetryHandler(new HttpRequestRetryHandler() {
                        @Override
                        public boolean retryRequest(IOException e, int i, HttpContext httpContext) {
                            if (i > 3) {
                                LOGGER.warning("Maximum tries reached for client http pool ");
                                return false;
                            }
                            if (e instanceof NoHttpResponseException) {
                                LOGGER.warning("No response from server on " + i + " call");
                                return true;
                            }
                            return false;
                        }
                    }).build();
                    // wait for the start up phase to signal us
                    startPeak.await();
                    System.out.println("starting peak");
                    while (counter.get() <= maxCalls) {
                        // execute the POST requests
                        executePost(httpClient, finalStartSkierId, endSkierId, start, end);
                        incCurrent();

                        // 20% requests done, signal the Peak threads to start
                        if (getCurrent() == Math.round(maxCalls * NUMTHREADS * 0.2)) {
                            System.out.println("starting cool");
                            startCool.countDown();
                        }
                        counter.getAndIncrement();
                    }
                } catch (InterruptedException | IOException e) {
                } finally {
                    // we've finished - let the main thread know
                    System.out.println("shutting peak");
                    endPeak.countDown();
                }
            };
            new Thread(thread).start();
            multiplier += 1;
            startSkierId = endSkierId + 1;

        }

    }

    private void executeCooldownPhase() throws IOException {
        int startSkierId = 1;
        int start = 361;
        int end = 420;
        int range = (int) (NUMSKIERS / (NUMTHREADS * 0.10));
        int maxCalls = (int) ((NUMRUNS * COOL_POST_VARIABLE));
        int multiplier = 1;
        expectedRequests = (int) (expectedRequests + (maxCalls*Math.round(NUMTHREADS*0.10)));


        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

        //increase max total connections to num threads
        cm.setMaxTotal((int) (NUMTHREADS * 0.10));

        // set max connections per route to num threads
        cm.setDefaultMaxPerRoute((int) (NUMTHREADS * 0.10));


        for (int i = 0; i < Math.round(NUMTHREADS * 0.10); i++) {

            int endSkierId = range * multiplier;
            AtomicInteger counter = new AtomicInteger(1);

            int finalStartSkierId = startSkierId;
            Runnable thread = () -> {
                try {
                    CloseableHttpClient httpClient = HttpClients.custom().setRetryHandler(new HttpRequestRetryHandler() {
                        @Override
                        public boolean retryRequest(IOException e, int i, HttpContext httpContext) {
                            if (i > 3) {
                                LOGGER.warning("Maximum tries reached for client http pool ");
                                return false;
                            }
                            if (e instanceof NoHttpResponseException) {
                                LOGGER.warning("No response from server on " + i + " call");
                                return true;
                            }
                            return false;
                        }
                    }).build();
                    // wait for the peak thread to tell us to start
                    startCool.await();
                    while (counter.get() <= maxCalls) {

                        executePost(null, finalStartSkierId, endSkierId, start, end);
                        counter.getAndIncrement();

                    }
                } catch (InterruptedException | IOException e) {
                } finally {
                    // we've finished - let the main thread know
                    System.out.println("shutting cool");
                    endCool.countDown();
                }
            };
            new Thread(thread).start();
            multiplier += 1;
            startSkierId = endSkierId + 1;

        }

    }


    public void executePost(CloseableHttpClient client, int startSkierId, int endSkierId, int startTime, int endTime) throws IOException, InterruptedException {
        // generate random values for skierId, liftId, time, waitTime
        int skierId = generateRandomValue(startSkierId, endSkierId);
        int liftId = generateRandomValue(0, NUMLIFTS);
        int time = generateRandomValue(startTime, endTime);
        int waitTime = generateRandomValue(0, 10);

        // create path and map variables
        String localVarPath = "/skiers/{resortId}/seasons/{seasonId}/days/{dayId}/skiers/{skierId}"
                .replaceAll("\\{resortId\\}", resortId)
                .replaceAll("\\{seasonId\\}", seasonId)
                .replaceAll("\\{dayId\\}", dayId)
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
        CloseableHttpResponse response = client.execute(method);

        try {

            int status = response.getStatusLine().getStatusCode();

            HttpEntity entity = response.getEntity();
            boolean isFailed = false;

            // resend request if 4XX or 5XX
            if (MINERRORCODE <= status) {
                isFailed = resendRequest(client, method);
            }
            if (isFailed) {
                incFail();
            } else {
                inc();
            }
            // print response body
            if (entity != null) {
                // return it as a String
//                String result = EntityUtils.toString(entity);
//                System.out.println(result);
            }
            EntityUtils.consume(entity);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        } finally {
//            response.close();
//            method.releaseConnection();
//        }
    }

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
     * Resend the request up to 5 times if status code 4XX - 5XX
     * @param httpClient
     * @param request
     * @return true if OK, false otherwise
     * @throws IOException
     * @throws InterruptedException
     */
    private boolean resendRequest(CloseableHttpClient httpClient, HttpPost request) throws IOException, InterruptedException {
        int count = 5;
        int status;

        for (int i = 0; i <= count; i ++) {
            CloseableHttpResponse response = httpClient.execute(request);
            status = response.getStatusLine().getStatusCode();

            if (status < MINERRORCODE) {
                return true;
            }
        }
        return false;
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