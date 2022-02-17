package assignment1.part1;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;



public class HttpApiClient {
//    private static HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
//            .connectTimeout(Duration.ofSeconds(10))
//            .build();

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
    private final CountDownLatch startSignal = new CountDownLatch(1);
    private final CountDownLatch startPeak = new CountDownLatch(1);
    private final CountDownLatch startCool = new CountDownLatch(1);

    private CountDownLatch endSignal;
    private CountDownLatch endPeak;
    private CountDownLatch endCool;
    private int peakRequests = 0;
    private int startUpRequests = 0;
    private int coolRequests = 0;

    public static void main(String[] args) throws InterruptedException {
        final HttpApiClient rmw = new HttpApiClient();
        validateArguments(args);

        url = url + SERVERADDRESS + port + webapp;
        rmw.endSignal = new CountDownLatch((NUMTHREADS/4));
        rmw.endPeak = new CountDownLatch(NUMTHREADS);
        rmw.endCool = new CountDownLatch((int)Math.round(NUMTHREADS*0.10));

        executeStartupPhase(rmw);
        executePeakPhase(rmw);
        executeCooldownPhase(rmw);

        long start = System.currentTimeMillis();
        rmw.startSignal.countDown();
        //rmw.startPeak.countDown();

        rmw.endSignal.await();
        rmw.endPeak.await();
        rmw.endCool.await();
        long finish = System.currentTimeMillis();
        long wallTime = ((finish - start)/1000);


        System.out.println("Total successful requests: " + (rmw.getSuccess()));
        System.out.println("Total failed requests: " + rmw.totalFailed);
        System.out.println("Wall Time in seconds: " + wallTime);

        double throughout = (double) rmw.totalSuccess/wallTime;
        System.out.println("Total throughput: " + throughout);

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

    private static void executeCooldownPhase(HttpApiClient rmw) {
        int startSkierId = 1;
        int start = 361;
        int end = 420;
        int range = (int) (NUMSKIERS/(NUMTHREADS*0.10));
        int maxCalls = (int) ((NUMRUNS*COOL_POST_VARIABLE));
        int multiplier = 1;

        for (int i = 0; i < NUMTHREADS*0.10; i++) {
            int endSkierId = range*multiplier;
            AtomicInteger counter = new AtomicInteger(1);

                int finalStartSkierId = startSkierId;
                Runnable thread =  () -> {
                    try {
                        rmw.startCool.await();
//                        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
//                                .connectTimeout(Duration.ofSeconds(10))
//                                .build();
                        RequestConfig requestConfig = RequestConfig.custom()
                                .setConnectionRequestTimeout(5000)
                                .setConnectTimeout(5000)
                                .setSocketTimeout(5000)
                                .build();

                        CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
                        while (counter.get() <= maxCalls) {

                            // wait for the peak thread to tell us to start
                            System.out.println("running cool");
                            rmw.executePost(httpclient, finalStartSkierId, endSkierId, start, end);
                            rmw.inc();
                            counter.getAndIncrement();
                        }

                    } catch (InterruptedException | IOException e) {
                    } finally {
                        // we've finished - let the main thread know
                        System.out.println("shutting cool");
                        rmw.endCool.countDown();
                        //return;
                    }
                };
                new Thread(thread).start();
                multiplier += 1;
                startSkierId = endSkierId+1;
            }

    }

    private static void executePeakPhase(HttpApiClient rmw) {
        int startSkierId = 1;
        int start = 91;
        int end = 360;
        int range = (NUMSKIERS/NUMTHREADS);
        int maxCalls = (int) ((NUMRUNS*PEAK_POST_VARIABLE) * (range));
        int multiplier = 1;

        for (int i = 0; i < NUMTHREADS; i++) {
            int endSkierId = range*multiplier;
            AtomicInteger counter = new AtomicInteger(1);

                int finalStartSkierId = startSkierId;

                Runnable thread =  () -> {
                    try {
                        rmw.startPeak.await();
                        RequestConfig requestConfig = RequestConfig.custom()
                                .setConnectionRequestTimeout(5000)
                                .setConnectTimeout(5000)
                                .setSocketTimeout(5000)
                                .build();

                        CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
                        while (counter.get() <= maxCalls) {

                            // wait for the main thread to tell us to start
                            //System.out.println("running peak");
                            rmw.executePost(httpclient, finalStartSkierId, endSkierId, start, end);
                            //rmw.peakRequests++;
                           rmw.incCurrent();
                            rmw.inc();

                            if (rmw.getCurrent() == Math.round(maxCalls*NUMTHREADS*0.2)) {
                                System.out.println("starting cool");
                                rmw.startCool.countDown();
                            }
                            counter.getAndIncrement();
                        }
                    } catch (InterruptedException | IOException e) {
                    } finally {
                        // we've finished - let the main thread know
                        System.out.println("shutting peak");
                        rmw.endPeak.countDown();
                        //return;
                    }
                };
                new Thread(thread).start();
                multiplier += 1;
                startSkierId = endSkierId+1;
            }

    }

    public synchronized void inc() {
        totalSuccess++;
    }

    public int getSuccess() {
        return totalSuccess;
    }

    private void incCurrent() {
        peakRequests++;
    }

    private int getCurrent() {
        return peakRequests;
    }

    private int getCoolRequests() {
        return coolRequests;
    }

    private int getStartUpRequests(){
        return startUpRequests;
    }


    public static void executeStartupPhase(HttpApiClient rmw) {
        int startSkierId = 1;
        int start = 1;
        int end = 90;
        int startupThreads = NUMTHREADS/4;

        if (startupThreads == 0) {
            startupThreads = 1;
        }
        int range = Math.round(NUMSKIERS/startupThreads);
        int maxCalls = (int) ((NUMRUNS*START_POST_VARIABLE) * (range));
        int multiplier = 1;

        for (int i = 0; i < startupThreads; i++) {
            int endSkierId = range*multiplier;
            AtomicInteger counter = new AtomicInteger(1);

            int finalStartSkierId = startSkierId;

            int finalStartupThreads = startupThreads;
            Runnable thread = () -> {
                    try {
                        rmw.startSignal.await();
//                        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
//                                .connectTimeout(Duration.ofSeconds(10))
//                                .build();
                        RequestConfig requestConfig = RequestConfig.custom()
                                .setConnectionRequestTimeout(5000)
                                .setConnectTimeout(5000)
                                .setSocketTimeout(5000)
                                .build();
                        CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
                        while (counter.get() <= maxCalls) {
                            // wait for the main thread to tell us to start
                            rmw.executePost(httpclient, finalStartSkierId, endSkierId, start, end);
                            rmw.inc();

                        if (rmw.startUpRequests == (Math.round(maxCalls*finalStartupThreads*0.2))) {
                            System.out.println("starting peak");
                            rmw.startPeak.countDown();
                       }
                            counter.getAndIncrement();
                        }
                    } catch (InterruptedException | IOException e) {
                    } finally {
                        // we've finished - let the main thread know
                        System.out.println("shutting start");
                        rmw.endSignal.countDown();
                        //return;
                    }
                };
                new Thread(thread).start();
                multiplier += 1;
                startSkierId = endSkierId+1;
        }
    }

    public void executePost(CloseableHttpClient httpClient, int startSkierId, int endSkierId, int startTime, int endTime) throws IOException, InterruptedException {
        int skierId = generateRandomValue(startSkierId, endSkierId);
        int liftId = generateRandomValue(0, NUMLIFTS);
        // time value from range of minutes passed to each thread (between start and end time)
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

        HttpPost method = new HttpPost(newUrl);

        method.setEntity(new StringEntity(json.toString()));

        CloseableHttpResponse response = httpClient.execute(method);

        try {

            int status = response.getStatusLine().getStatusCode();

            HttpEntity entity = response.getEntity();
            boolean isFailed = false;

            if (MINERRORCODE <= status) {
                isFailed = resendRequest(httpClient, method);
            }

            if (isFailed) {
                totalFailed++;
                totalSuccess--;
            }
//            if (entity != null) {
//                // return it as a String
//                String result = EntityUtils.toString(entity);
//                System.out.println(result);
//            }
            } finally {
            response.close();
        }


//        // create a request
//        HttpRequest request = HttpRequest.newBuilder(
//                        URI.create(newUrl))
//                .header("accept", "application/json")
//                .POST(HttpRequest.BodyPublishers.ofString(json))
//                .build();
//
//        try {
//            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//
//            int status = response.statusCode();
//
//            boolean isFailed = false;
//
//            if (MINERRORCODE <= status) {
//                isFailed = resendRequest(httpClient, request);
//            }
//            inc();
//            if (isFailed) {
//                totalFailed ++;
//                totalSuccess --;
//            }
//        } catch (IOException e) {
//            System.out.println(e);
//            e.printStackTrace();
//        }
//        System.out.println(Thread.currentThread() + response.body());
    }

    private String buildURL(String localVarPath) {
        String base = url;
        String newUrl = base + localVarPath;
        return newUrl;
    }

    private boolean resendRequest(CloseableHttpClient httpClient, HttpPost request) throws IOException, InterruptedException {
        int count = 5;
        int status;

        for (int i = 0; i <= count; i ++) {
            org.apache.http.HttpResponse response = httpClient.execute(request);
            status = response.getStatusLine().getStatusCode();

            if (status < MINERRORCODE) {
                return true;
            }
        }
        return false;
    }

    private int generateRandomValue(int startValue, int endValue) {
        SecureRandom random = new SecureRandom();
        int n = random.nextInt((endValue-startValue)+1)+startValue;
        return n;
    }


}
