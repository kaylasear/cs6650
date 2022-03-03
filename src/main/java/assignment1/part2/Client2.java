package assignment1.part2;


import assignment1.part2.model.SystemStats;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.util.EntityUtils;

public class Client2 {

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
    private ArrayList<SystemStats> systemStatsArrayList= new ArrayList<>();


    public static void main(String[] args) throws InterruptedException {
        final Client2 rmw = new Client2();
        rmw.validateArguments(args); // check and assign arguments

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
        System.out.println("Total failed requests: " + rmw.getFail());
        System.out.println("Wall Time in seconds: " + wallTime);

        double throughput = (double) rmw.totalSuccess / wallTime;
        System.out.println("Total throughput: " + throughput);

        CsvWriter csvWriter = new CsvWriter();
        csvWriter.writeToCsvFile(rmw.systemStatsArrayList, "systemstats_64T.csv");

        // calculate response time stats
        ResultGenerator resultGenerator = new ResultGenerator(rmw.systemStatsArrayList);
        resultGenerator.setThroughput(throughput);
        resultGenerator.generateResults();
        System.out.println(resultGenerator);

    }

    private synchronized void incFail() {
        totalFailed++;
    }

    private int getFail() {
        return totalFailed;
    }

    private void validateArguments(String[] args) {
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

    public void executeStartupPhase() {
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

        for (int i = 0; i < startupThreads; i++) {
            int endSkierId = range * multiplier;
            AtomicInteger counter = new AtomicInteger(1);
            CloseableHttpClient httpclient = HttpClients.createDefault();

            int finalStartSkierId = startSkierId;

            int finalStartupThreads = startupThreads;
            Runnable thread = () -> {
                try {

                    while (counter.get() <= maxCalls) {

                        double startTime = (double)System.currentTimeMillis();
                        CloseableHttpResponse response = executePost(httpclient, finalStartSkierId, endSkierId, start, end);
                        response.close();
                        double endTime = (double)System.currentTimeMillis();

                        addStats(response, startTime, endTime);
                        int current = getSuccess();

                        if (current == (Math.round(maxCalls * finalStartupThreads * 0.2))) {
                            startPeak.countDown();
                        }
                        counter.getAndIncrement();
                    }
                    httpclient.close();
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

    private void executePeakPhase() {
        int startSkierId = 1;
        int start = 91;
        int end = 360;
        int range = (NUMSKIERS / NUMTHREADS);
        int maxCalls = (int) ((NUMRUNS * PEAK_POST_VARIABLE) * (range));
        int multiplier = 1;

        for (int i = 0; i < NUMTHREADS; i++) {
            int endSkierId = range * multiplier;
            AtomicInteger counter = new AtomicInteger(1);
            int finalStartSkierId = startSkierId;
            CloseableHttpClient httpclient = HttpClients.createDefault();

            int finalI = i;
            Runnable thread = () -> {
                try {
                    startPeak.await();
                    System.out.println("starting peak");
                    while (counter.get() <= maxCalls) {

                        double startTime = (double)System.currentTimeMillis();
                        CloseableHttpResponse response = executePost(httpclient, finalStartSkierId, endSkierId, start, end);
                        double endTime = (double)System.currentTimeMillis();
                        incCurrent();

                        addStats(response, startTime, endTime);

                        int current = getCurrent();
                        if (current == Math.round(maxCalls * NUMTHREADS * 0.2)) {
                            System.out.println("starting cool");
                            startCool.countDown();
                        }
                        counter.getAndIncrement();
                    }
                    httpclient.close();
                } catch (InterruptedException | IOException e) {
                } finally {
                    // we've finished - let the main thread know
                    System.out.println("shutting peak " + finalI);
                    endPeak.countDown();
                }
            };
            new Thread(thread).start();
            multiplier += 1;
            startSkierId = endSkierId + 1;
        }

    }

    private void executeCooldownPhase() {
        int startSkierId = 1;
        int start = 361;
        int end = 420;
        int range = (int) (NUMSKIERS / (NUMTHREADS * 0.10));
        int maxCalls = (int) ((NUMRUNS * COOL_POST_VARIABLE));
        int multiplier = 1;

        for (int i = 0; i < NUMTHREADS * 0.10; i++) {
            int endSkierId = range * multiplier;
            AtomicInteger counter = new AtomicInteger(1);
            CloseableHttpClient httpclient = HttpClients.createDefault();

            int finalStartSkierId = startSkierId;
            Runnable thread = () -> {
                try {
                    // wait for the peak thread to tell us to start
                    startCool.await();
                    while (counter.get() <= maxCalls) {

                        double startTime = (double)System.currentTimeMillis();
                        CloseableHttpResponse response = executePost(httpclient, finalStartSkierId, endSkierId, start, end);
                        double endTime = (double)System.currentTimeMillis();
                        counter.getAndIncrement();

                        addStats(response, startTime, endTime);

                    }
                    httpclient.close();
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

    /**
     * Build stats object and return it
     * @param response
     * @param start
     * @param end
     */
    private synchronized void addStats(CloseableHttpResponse response, double start, double end) {
        double latency = (end - start);
        String method = "POST";
        int responseCode = response.getStatusLine().getStatusCode();

        SystemStats stat = new SystemStats(start,method, latency, responseCode);
        systemStatsArrayList.add(stat);
    }

    public CloseableHttpResponse executePost(CloseableHttpClient client, int startSkierId, int endSkierId, int startTime, int endTime) throws IOException, InterruptedException {
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
        method.setEntity(new StringEntity(json.toString()));
        CloseableHttpResponse response = client.execute(method);

        try {

            int status = response.getStatusLine().getStatusCode();

            HttpEntity entity = response.getEntity();
            boolean isFailed = false;

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
                String result = EntityUtils.toString(entity);
                System.out.println(result);
                EntityUtils.consume(entity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        method.releaseConnection();
        return response;
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
