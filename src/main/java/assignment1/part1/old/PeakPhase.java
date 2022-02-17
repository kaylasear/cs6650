package assignment1.part1.old;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Class represents the Peak Phase, will launch NUMTHREADS to send POST requests
 */
public class PeakPhase implements Callable {
    private HttpClient httpClient;

    private int NUM_THREADS;
    private int numThreadsInPhase;
    private int numSkiers;
    private int numLifts;
    private String url;

    private int startSkierId = 1;
    private int endSkierId;
    private int startTime = 91;
    private int endTime = 360;

    private final double POST_VARIABLE = 0.6;
    private int maxCalls;
    private int range;

    private static final int MINERRORCODE = 400;
    private static final int MAXERRORCODE = 599;

    private static String dayId = "5";
    private static String seasonId = "2019";
    private static String resortId = "1";
    private int totalNumOfSuccessfulRequests = 0;
    private int totalFailedRequests = 0;
    private ExecutorService pool;

    /**
     * Constructs a Peak Phase object with httpclient, num of threads, num of skiers, url and num of lifts
     * Determine the range to assign skierIds
     * @param pool
     * @param httpClient
     * @param num_threads
     * @param numSkiers
     * @param url
     * @param numLifts
     */
    public PeakPhase(ExecutorService pool, HttpClient httpClient, int num_threads, int numSkiers, String url, int numLifts) {
        this.pool = pool;
        this.httpClient = httpClient;
        this.NUM_THREADS = num_threads;
        this.numThreadsInPhase = num_threads;
        this.numSkiers = numSkiers;
        this.numLifts = numLifts;
        this.url = url;

        range = numSkiers/numThreadsInPhase;
    }

    /**
     * Start the call to execute http requests with a start and end range of skierIds
     * @return
     * @throws Exception
     */
    @Override
    synchronized public PeakPhase call() throws Exception {
        System.out.println("running peak phase....");
        Future<CooldownPhase> future = null;
        //CooldownPhase result = null;
        int multiplier = 1;
        maxCalls = (int) ((this.numLifts*POST_VARIABLE) * (range));

        for (int i = 0; i < numThreadsInPhase; i++) {
            endSkierId = range*multiplier;

            int counter = 1;

            while (counter <= this.maxCalls) {
                // Each POST should randomly select:
                // skierId from range of Ids, liftId, time value, waitTime
                try {
                    executePostRequest();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                counter += 1;
                totalNumOfSuccessfulRequests += 1;

                // start phase 3
                if (totalNumOfSuccessfulRequests == Math.round((maxCalls*numThreadsInPhase)*0.2)) {
                    CooldownPhase cooldownPhase = new CooldownPhase(httpClient, NUM_THREADS, numSkiers, url, numLifts);
                    future = pool.submit(cooldownPhase);
                    //result = cooldownPhase.call();
                }
            }
            // start new range of skierIds
            multiplier += 1;
            startSkierId = endSkierId+1;
        }
        // grab total num of requests from cooldown phase and set it to the peak phase object
       CooldownPhase result = future.get();

        this.setTotalNumOfSuccessfulRequests(this.totalNumOfSuccessfulRequests+ result.getTotalNumOfSuccessfulRequests());
        this.setTotalFailedRequests(this.totalFailedRequests + result.getTotalFailedRequests());
        System.out.println("done with peak phase....");
        return this;

    }

    /**
     * Execute a post request to create a lift ride object
     * @throws IOException
     * @throws InterruptedException
     */
    private void executePostRequest() throws IOException, InterruptedException {
        int skierId = generateRandomValue(startSkierId, endSkierId);
        int liftId = generateRandomValue(0, numLifts);
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
            this.totalFailedRequests += 1;
            this.totalNumOfSuccessfulRequests -= 1;
        }
        System.out.println("peak phase" + response.body());
    }

    /**
     * Private method to generate a random value within a range
     * @param startValue start value in range
     * @param endValue end value in range
     * @return a generated random number
     */
    private int generateRandomValue(Integer startValue, Integer endValue) {
        SecureRandom random = new SecureRandom();
        int n = random.nextInt((endValue-startValue)+1)+startValue;
        return n;
    }

    /**
     * Resending response up to 5 times, before counting it as a failed request.
     * @param request
     * @throws IOException
     * @throws InterruptedException
     * @return
     */
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

    /** Getters and Setters **/

    public int getTotalNumOfSuccessfulRequests() {
        return totalNumOfSuccessfulRequests;
    }

    public int getTotalFailedRequests() {
        return totalFailedRequests;
    }

    public void setTotalNumOfSuccessfulRequests(int totalNumOfSuccessfulRequests) {
        this.totalNumOfSuccessfulRequests = totalNumOfSuccessfulRequests;
    }

    public void setTotalFailedRequests(int totalFailedRequests) {
        this.totalFailedRequests = totalFailedRequests;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeakPhase peakPhase = (PeakPhase) o;
        return NUM_THREADS == peakPhase.NUM_THREADS && numThreadsInPhase == peakPhase.numThreadsInPhase && numSkiers == peakPhase.numSkiers && numLifts == peakPhase.numLifts && startSkierId == peakPhase.startSkierId && endSkierId == peakPhase.endSkierId && startTime == peakPhase.startTime && endTime == peakPhase.endTime && Double.compare(peakPhase.POST_VARIABLE, POST_VARIABLE) == 0 && maxCalls == peakPhase.maxCalls && range == peakPhase.range && totalNumOfSuccessfulRequests == peakPhase.totalNumOfSuccessfulRequests && totalFailedRequests == peakPhase.totalFailedRequests && Objects.equals(httpClient, peakPhase.httpClient) && Objects.equals(url, peakPhase.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpClient, NUM_THREADS, numThreadsInPhase, numSkiers, numLifts, url, startSkierId, endSkierId, startTime, endTime, POST_VARIABLE, maxCalls, range, totalNumOfSuccessfulRequests, totalFailedRequests);
    }

    @Override
    public String toString() {
        return "PeakPhase{" +
                "httpClient=" + httpClient +
                ", NUM_THREADS=" + NUM_THREADS +
                ", numThreadsInPhase=" + numThreadsInPhase +
                ", numSkiers=" + numSkiers +
                ", numLifts=" + numLifts +
                ", url='" + url + '\'' +
                ", startSkierId=" + startSkierId +
                ", endSkierId=" + endSkierId +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", POST_VARIABLE=" + POST_VARIABLE +
                ", maxCalls=" + maxCalls +
                ", range=" + range +
                ", totalNumOfSuccessfulRequests=" + totalNumOfSuccessfulRequests +
                ", totalFailedRequests=" + totalFailedRequests +
                '}';
    }
}
