package assignment1.part2;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author Ian Gorton
 * Multiple threads increment a value concurrently. Method is synchronized so behaves correctly
 * Demonstrates us of a count down latch to implement barrier synchronization
 *
 */
public class HttpApiClient {
    private static HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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

    private static final int MINERRORCODE = 400;
    private static final int MAXERRORCODE = 599;

    private static int NUMTHREADS;
    private static int NUMSKIERS;
    private static int NUMLIFTS = 40;
    private static int MEANNUMLIFTSPERDAY = 10;
    private static String SERVERADDRESS = null;

    private int totalSuccess;
    private int totalFailed = 0;
    private final CountDownLatch startSignal = new CountDownLatch(1);
    private final CountDownLatch startPeak = new CountDownLatch(1);
    private final CountDownLatch startCool = new CountDownLatch(1);

    private CountDownLatch endSignal;
    private CountDownLatch endPeak;
    private CountDownLatch endCool;

    public synchronized void inc() {
        totalSuccess++;
    }

    public int getSuccess() {
        return totalSuccess;
    }


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

        rmw.endSignal.await();
        rmw.endPeak.await();
        rmw.endCool.await();
        long finish = System.currentTimeMillis();
        long wallTime = ((finish - start)/1000);

        CsvWriter csvWriter = new CsvWriter();
        //csvWriter.writeToCsvFile(rmw, "systemstats.csv");

        System.out.println("Total successful requests: " + rmw.totalSuccess);
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
            if (MEANNUMLIFTSPERDAY <= MAXMEANLIFTS) {
                MEANNUMLIFTSPERDAY = meanLifts;
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
        int maxCalls = (int) ((NUMLIFTS*COOL_POST_VARIABLE));
        int multiplier = 1;

        for (int i = 0; i < Math.round(NUMTHREADS*0.10); i++) {
            System.out.println("running cool");
            int endSkierId = range*multiplier;
            AtomicInteger counter = new AtomicInteger(1);

                int finalStartSkierId = startSkierId;
                Runnable thread =  () -> {
                    try {
                        while (counter.get() <= maxCalls) {
                            // wait for the main thread to tell us to start
                            rmw.startCool.await();
                            rmw.executePost(finalStartSkierId, endSkierId, start, end);
                            rmw.inc();

                            counter.getAndIncrement();
                        }

                    } catch (InterruptedException | IOException e) {
                    } finally {
                        // we've finished - let the main thread know
                        System.out.println("shutting cool");
                        rmw.endCool.countDown();
                        return;
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
        int maxCalls = (int) ((NUMLIFTS*PEAK_POST_VARIABLE) * (range));
        int multiplier = 1;

        for (int i = 0; i < NUMTHREADS; i++) {
            System.out.println("running peak");
            int endSkierId = range*multiplier;
            AtomicInteger counter = new AtomicInteger(1);
            AtomicInteger currentRequests = new AtomicInteger(1);

                int finalStartSkierId = startSkierId;

                Runnable thread =  () -> {
                    try {
                        while (counter.get() <= maxCalls) {
                            // wait for the main thread to tell us to start
                            rmw.startPeak.await();
                            rmw.executePost(finalStartSkierId, endSkierId, start, end);
                            rmw.inc();

                            if (currentRequests.get() == Math.round(maxCalls*NUMTHREADS*0.2)) {
                                System.out.println("starting cool");
                                rmw.startCool.countDown();
                            }
                            counter.getAndIncrement();
                            currentRequests.getAndIncrement();
                        }
                    } catch (InterruptedException | IOException e) {
                    } finally {
                        // we've finished - let the main thread know
                        System.out.println("shutting peak");
                        rmw.endPeak.countDown();
                        return;
                    }
                };
                new Thread(thread).start();
                multiplier += 1;
                startSkierId = endSkierId+1;
            }

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
        int maxCalls = (int) ((NUMLIFTS*START_POST_VARIABLE) * (range));
        int multiplier = 1;

        for (int i = 0; i < startupThreads; i++) {
            int endSkierId = range*multiplier;
            AtomicInteger counter = new AtomicInteger(1);

                int finalStartSkierId = startSkierId;
                int finalI = i;

            int finalStartupThreads = startupThreads;
            Runnable thread = () -> {
                    try {

                        while (counter.get() <= maxCalls) {
                            // wait for the main thread to tell us to start
                            rmw.startSignal.await();
                            rmw.executePost(finalStartSkierId, endSkierId, start, end);
                            rmw.inc();

                        if (rmw.getSuccess() == Math.round(maxCalls*finalStartupThreads*0.2)) {
                            rmw.startPeak.countDown();
                       }
                            counter.getAndIncrement();
                        }
                    } catch (InterruptedException | IOException e) {
                    } finally {
                        // we've finished - let the main thread know
                        System.out.println("shutting start");
                        rmw.endSignal.countDown();
                        return;
                    }
                };
                new Thread(thread).start();
                multiplier += 1;
                startSkierId = endSkierId+1;
        }
    }

    public void executePost(int startSkierId, int endSkierId, int startTime, int endTime) throws IOException, InterruptedException {
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
        url = url + localVarPath;

        // json formatted data
        String json = new StringBuilder()
                .append("{")
                .append("\"time\":" + time + " ,")
                .append("\"liftId\":" + liftId + " ,")
                .append("\"waitTime\":" + waitTime)
                .append("}").toString();

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .uri(URI.create(url))
                .setHeader("User-Agent", "Java 17 HttpClient Bot")
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();

        boolean isFailed = false;

        if (MINERRORCODE <= status && status <= MAXERRORCODE) {
            isFailed = resendRequest(request);
        }

        if (isFailed) {
            totalFailed ++;
            totalSuccess --;
        }
        //System.out.println(Thread.currentThread() + response.body());
    }

    private boolean resendRequest(HttpRequest request) throws IOException, InterruptedException {
        int count = 5;
        int status;

        for (int i = 0; i <= count; i ++) {
            HttpResponse<String> response= httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            status = response.statusCode();

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
