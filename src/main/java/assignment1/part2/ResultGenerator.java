package assignment1.part2;

import assignment1.part2.model.SystemStats;

import java.util.ArrayList;
import java.util.Arrays;

public class ResultGenerator {
    private ArrayList<SystemStats> systemStats;
    private double meanResponseTime;
    private double medianResponseTime;
    private int throughput;
    private double p99;
    private double minResponseTime;
    private double maxResponseTime;

    private ArrayList<Double> responseTimes;

    public ResultGenerator(ArrayList<SystemStats> systemStats) {
        this.systemStats = systemStats;
        this.responseTimes = getResponseTimes();
    }

    /**
     * Calculate results from all three phases
     */
    private void generateResults() {
        calculateMean();
        calculateMedian();
        calculateThroughput();
        calculatep99();
        calculateMin();
        calculateMax();
    }

    /**
     * Calculate mean response time
     */
    private void calculateMean() {
        int size = systemStats.size();
        double sum = 0;

        for (SystemStats stat : systemStats) {
            double responseTime = stat.getLatency();
            sum += responseTime;
        }

        this.meanResponseTime = sum/size;

    }

    /**
     * Calculate the median response time
     */
    private void calculateMedian() {
        if (responseTimes.size() % 2 == 0) {
            this.medianResponseTime = responseTimes.get(responseTimes.size()/2) + responseTimes.get((responseTimes.size()/2)-1)/2;
        } else {
            this.medianResponseTime = responseTimes.get(responseTimes.size()/2);
        }

    }

    /**
     * Helper method to get response times from SystemStats objects
     * @return list of response times
     */
    private ArrayList<Double> getResponseTimes() {
        ArrayList<Double> responseTimes = new ArrayList<>();

        for (SystemStats stat : this.systemStats) {
            double responseTime = stat.getLatency();
            responseTimes.add(responseTime);
        }

        Arrays.sort(new ArrayList[]{responseTimes});
        return responseTimes;
    }

    /**
     * Calculate the throughput = total num of requests/wall time
     */
    private void calculateThroughput() {
    }

    /**
     * Calculate the 99th percentile response time
     */
    private void calculatep99() {
    }

    /**
     * Calculate the min response time
     */
    private void calculateMin() {
        this.minResponseTime = this.responseTimes.get(0);
    }

    /**
     * Calculate the max response time
     */
    private void calculateMax() {
        this.maxResponseTime = this.responseTimes.get(-1);
    }


    /** Getters and Setters */
    public ArrayList<SystemStats> getSystemStats() {
        return systemStats;
    }

    public void setSystemStats(ArrayList<SystemStats> systemStats) {
        this.systemStats = systemStats;
    }

    public double getMeanResponseTime() {
        return meanResponseTime;
    }

    public void setMeanResponseTime(double meanResponseTime) {
        this.meanResponseTime = meanResponseTime;
    }

    public double getMedianResponseTime() {
        return medianResponseTime;
    }

    public void setMedianResponseTime(double medianResponseTime) {
        this.medianResponseTime = medianResponseTime;
    }

    public int getThroughput() {
        return throughput;
    }

    public void setThroughput(int throughput) {
        this.throughput = throughput;
    }

    public double getP99() {
        return p99;
    }

    public void setP99(double p99) {
        this.p99 = p99;
    }

    public double getMinResponseTime() {
        return minResponseTime;
    }

    public void setMinResponseTime(double minResponseTime) {
        this.minResponseTime = minResponseTime;
    }

    public double getMaxResponseTime() {
        return maxResponseTime;
    }

    public void setMaxResponseTime(double maxResponseTime) {
        this.maxResponseTime = maxResponseTime;
    }


}
