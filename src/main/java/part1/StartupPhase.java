package part1;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

public class StartupPhase implements Callable<ArrayList<Integer>> {
    private final HttpClient httpClient;
    private final int NUM_THREADS;
    private int numThreadsInPhase;
    private int numSkiers;
    private String url;
    private int numLifts;
    private int startSkierId = 1;
    private int endSkierId;
    private int startTime = 1;
    private int endTime = 90;

    private final double POST_VARIABLE = 0.2;
    private int maxCalls;
    private int range;

    private static final int MINERRORCODE = 400;
    private static final int MAXERRORCODE = 599;


    private static String dayId = "5";
    private static String seasonId = "2019";
    private static String resortId = "1";
    private int totalNumOfSuccessfulRequests = 0;
    private int totalFailedRequests = 0;


    public StartupPhase(HttpClient httpClient, int numthreads, int numskiers, String url, int numlifts) {
        this.httpClient = httpClient;
        this.NUM_THREADS = numthreads;

        if (numthreads == 1) {
            this.numThreadsInPhase = numthreads;
        } else {
            this.numThreadsInPhase = Math.round(numthreads/4);
        }

        this.numSkiers = numskiers;
        this.url = url;
        this.numLifts = numlifts;

        range = Math.round(numskiers/(numThreadsInPhase));
    }

    @Override
    synchronized public ArrayList<Integer> call() throws Exception {
        System.out.println("running startup phase....");
        PeakPhase resultPeakPhase = null;
        int multiplier = 1;

        for (int i = 0; i < numThreadsInPhase; i++) {
            endSkierId = range*multiplier;

            maxCalls = (int) ((this.numLifts*POST_VARIABLE) * (range));
            int counter = 1;

            while (counter <= this.maxCalls) {
                try {
                    executePostRequest();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                counter += 1;
                totalNumOfSuccessfulRequests += 1;

                // start phase 2
                if (totalNumOfSuccessfulRequests == Math.round(((maxCalls*numThreadsInPhase)*.20))) {
                    PeakPhase peakPhase = new PeakPhase(httpClient, NUM_THREADS, numSkiers, url, numLifts);
                    resultPeakPhase = peakPhase.call();
                }
            }
            // start new range of skierIds
            multiplier += 1;
            startSkierId = endSkierId+1;
        }
        int totalRequests = this.totalNumOfSuccessfulRequests + resultPeakPhase.getTotalNumOfSuccessfulRequests();
        int failedRequests = this.totalFailedRequests + resultPeakPhase.getTotalFailedRequests();
        ArrayList<Integer> resulList = new ArrayList<>(Arrays.asList(totalRequests, failedRequests));
        return resulList;

    }


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

//        System.out.println(response.statusCode());
//        System.out.println(response.body());
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

    public int getTotalFailedRequests() {
        return totalFailedRequests;
    }

}
