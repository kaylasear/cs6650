package assignment1.part1.old;

import assignment1.part1.StartupPhase;

import java.io.IOException;

import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * Class represents a HttpClient that sends requests to API server
 */
public class HttpClientSynchronous extends Thread {

    private static String url = "http://";
    private static final String webapp = "/assignment1";

    private static final int MINLIFTS = 5;
    private static final int MAXLIFTS = 60;
    private static final int MAXMEANLIFTS = 20;
    private static final int MAXTHREADS = 1024;
    private static final int MAXSKIERS = 100000;

    private static int NUMTHREADS;
    private static int NUMSKIERS;
    private static int NUMLIFTS = 40;
    private static int MEANNUMLIFTSPERDAY = 10;
    private static String SERVERADDRESS = null;


    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        // check args = [numThreads, numSkiers, numLifts, meanNumOfLiftsPerDay, serverAddress]
//        validateArguments(args);
//
//        // start thread pool
//        ExecutorService pool = Executors.newFixedThreadPool(NUMTHREADS);
//        url = url + SERVERADDRESS + webapp;
//
//        long start = System.currentTimeMillis();
//        // three phases - each one sending a large number of lift rides to the server API
//        final Future<ArrayList<Integer>> future = pool.submit(new StartupPhase(pool, NUMTHREADS, NUMSKIERS, url, NUMLIFTS, MEANNUMLIFTSPERDAY));
//
//        int success = future.get().get(0);
//        int failures = future.get().get(1);
//        pool.shutdown();
//
//        try {
//            pool.awaitTermination(1000, TimeUnit.SECONDS);
//            long finish = System.currentTimeMillis();
//            long wallTime = (finish - start);
//
//            System.out.println("Total successful requests: " + success);
//            System.out.println("Total failed requests: " + failures);
//            System.out.println("Wall Time in seconds: " + wallTime/1000);
//
//            double throughout = (double) success/(wallTime/1000);
//            System.out.println("Total throughput: " + throughout);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

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
