package assignment1.part1;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class represents the StartUp Phase, will launch numThreads/4 threads
 */
public class StartupPhase implements Runnable {
    private final int numRuns;
    private int numThreadsInPhase;
    private int numSkiers;
    private String url;
    private int numLifts;
    private int startSkierId;
    private int endSkierId;
    private int startTime = 1;
    private int endTime = 90;

    private final double POST_VARIABLE = 0.2;
    private int maxCalls;
    private int range;

    private static final int MINERRORCODE = 400;


    private static String dayId = "5";
    private static String seasonId = "2019";
    private static String resortId = "1";
    private int totalNumOfSuccessfulRequests = 0;
    private int totalFailedRequests = 0;


    /**
     * Constructs a StartUp Phase object with Httpclient, num of threads, num of skiers, url and num of lifts
     * Determine the range to assign skierIds
     * @param numthreads
     * @param numskiers
     * @param url
     * @param numlifts
     */
    public StartupPhase(int numthreads, int numskiers, String url, int numlifts, int numRuns) {
        this.numThreadsInPhase = numthreads;

//        if (numthreads == 1) {
//            this.numThreadsInPhase = numthreads;
//        } else {
//            this.numThreadsInPhase = Math.round(numthreads/4);
//        }

        this.numSkiers = numskiers;
        this.url = url;
        this.numLifts = numlifts;
        this.numRuns = numRuns;

        range = Math.round(numskiers/(numThreadsInPhase));
    }


    synchronized public void inc() {
        this.totalNumOfSuccessfulRequests++;
    }

    public int getTotalNumOfSuccessfulRequests() {
        return this.totalNumOfSuccessfulRequests;
    }

    public void run() {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        AtomicInteger counter = new AtomicInteger(1);

        while (counter.get() <= maxCalls) {

            try {
                executePostRequest(httpclient);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //inc();
            counter.getAndIncrement();
        }
        try {
            httpclient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Execute a post request to create a lift ride object
     * @throws IOException
     * @throws InterruptedException
     */
    private void executePostRequest(CloseableHttpClient client) throws IOException, InterruptedException {
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
        CloseableHttpResponse response = client.execute(method);

        try {

            int status = response.getStatusLine().getStatusCode();

            HttpEntity entity = response.getEntity();
            boolean isFailed = false;

            if (MINERRORCODE <= status) {
                isFailed = resendRequest(client, method);
            }
//
            if (isFailed) {
                incFail();
            } else {
                inc();
            }
            if (entity != null) {
                // return it as a String
//                String result = EntityUtils.toString(entity);
//                System.out.println(result);
                EntityUtils.consume(entity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            response.close();
        }
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
    private boolean resendRequest(CloseableHttpClient client, HttpPost request) throws IOException, InterruptedException {
        int count = 5;
        int status;

        for (int i = 0; i <= count; i ++) {
            CloseableHttpResponse response = client.execute(request);
//            int response = httpClient.executeMethod(request);
            status = response.getStatusLine().getStatusCode();
//            status = response.statusCode();

            if (status < MINERRORCODE) {
                return true;
            }
        }
        return false;
    }

    private String buildURL(String localVarPath) {
        String base = url;
        String newUrl = base + localVarPath;
        return newUrl;
    }

    /** Getters and Setters **/

    public synchronized void incFail() {
        totalFailedRequests++;
    }

    public int getTotalFailedRequests() {
        return totalFailedRequests;
    }

}
