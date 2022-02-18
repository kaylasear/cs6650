package assignment1.part2;

import assignment1.part2.model.SystemStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

/**
 * Class represents a generator that calculates system's statistics
 */
public class ResultGenerator {
    private ArrayList<SystemStats> systemStats;
    private double meanResponseTime;
    private double medianResponseTime;
    private double throughput;
    private double p99;
    private double minResponseTime;
    private double maxResponseTime;

    private ArrayList<Double> responseTimes = new ArrayList<>();

    public ResultGenerator(ArrayList<SystemStats> systemStats) {
        this.systemStats = systemStats;
    }

    /**
     * Calculate results from all three phases
     */
    public void generateResults() {
        this.responseTimes = getResponseTimes();
        calculateMean();
        calculateMedian();
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
            this.medianResponseTime = (responseTimes.get(responseTimes.size()/2) + responseTimes.get((responseTimes.size()/2)-1)) / 2;
        } else {
            this.medianResponseTime = responseTimes.get((responseTimes.size()/2)-1);
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

        Collections.sort(responseTimes);
        return responseTimes;
    }


    /**
     * Calculate the wall time in seconds by adding up all the response times
     * @return the wall time
     */
    private double getWallTime() {
        double wallTime = 0;

        for (Double response : this.responseTimes) {
            wallTime += response;
        }
        return wallTime/1000;
    }

    /**
     * Calculate the 99th percentile response time
     */
    private void calculatep99() {
        int i = (int)Math.ceil(0.99 * this.responseTimes.size());
        this.p99 = this.responseTimes.get(i-1);
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
        this.maxResponseTime = this.responseTimes.get(this.responseTimes.size()-1);
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

    public double getThroughput() {
        return throughput;
    }

    public void setThroughput(double throughput) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResultGenerator that = (ResultGenerator) o;
        return Double.compare(that.meanResponseTime, meanResponseTime) == 0 && Double.compare(that.medianResponseTime, medianResponseTime) == 0 && Double.compare(that.throughput, throughput) == 0 && Double.compare(that.p99, p99) == 0 && Double.compare(that.minResponseTime, minResponseTime) == 0 && Double.compare(that.maxResponseTime, maxResponseTime) == 0 && Objects.equals(systemStats, that.systemStats) && Objects.equals(responseTimes, that.responseTimes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(systemStats, meanResponseTime, medianResponseTime, throughput, p99, minResponseTime, maxResponseTime, responseTimes);
    }

    @Override
    public String toString() {
        return "ResultGenerator{" +
                "  meanResponseTime=" + meanResponseTime +
                ", medianResponseTime=" + medianResponseTime +
                ", throughput=" + throughput +
                ", p99=" + p99 +
                ", minResponseTime=" + minResponseTime +
                ", maxResponseTime=" + maxResponseTime +
                '}';
    }
}
