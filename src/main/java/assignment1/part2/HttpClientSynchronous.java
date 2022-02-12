package assignment1.part2;

import assignment1.part2.model.SystemStats;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.*;

public class HttpClientSynchronous extends Thread {
    private static HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static String url = "http://";
    private static final String webapp = "/assignment1";

    private static final int MINLIFTS = 5;
    private static final int MAXLIFTS = 60;
    private static final int MAXMEANLIFTS = 20;
    private static final int MAXTHREADS = 1024;
    private static final int MAXSKIERS = 100000;

    private static int NUMTHREADS = 0;
    private static int NUMSKIERS = 0;
    private static int NUMLIFTS = 40;
    private static int MEANNUMLIFTSPERDAY = 10;
    private static String SERVERADDRESS = null;


    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        // check args = [numThreads, numSkiers, numLifts, meanNumOfLiftsPerDay, serverAddress]
        validateArguments(args);

        // start thread pool
        ExecutorService pool = Executors.newFixedThreadPool(NUMTHREADS);
        url = url + SERVERADDRESS + webapp;

        // three phases - each one sending a large number of lift rides to the server API
        final Future<ArrayList<SystemStats>> future = pool.submit(new StartupPhase(httpClient, NUMTHREADS, NUMSKIERS, url, NUMLIFTS));

        ArrayList<SystemStats> systemStats = future.get();
        pool.shutdown();

        try {
            pool.awaitTermination(1000, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        buildRecord(systemStats);
        ResultGenerator resultGenerator = new ResultGenerator(systemStats);
    }



    /**
     * Write out a record containing system stats into a CVS
     * @param systemStats
     */
    private static void buildRecord(ArrayList<SystemStats> systemStats) {
    }

    /**
     * Check and assign arguments
     * @param args
     */
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
}
