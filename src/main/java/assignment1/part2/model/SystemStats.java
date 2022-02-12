package assignment1.part2.model;

import java.util.Objects;

/**
 * Represents statistics of Http requests with startTime, request Types, latency and response codes.
 */
public class SystemStats {
    private double startTime;
    private String requestType;
    private double latency;
    private Integer responseCode;

    /**
     * Constructs a System Stats object with start time, request type, latency and response code.
     * @param startTime
     * @param requestType
     * @param latency
     * @param responseCode
     */
    public SystemStats(double startTime, String requestType, double latency, Integer responseCode) {
        this.startTime = startTime;
        this.requestType = requestType;
        this.latency = latency;
        this.responseCode = responseCode;
    }

    /** Getters **/
    public double getStartTime() {
        return startTime;
    }

    public String getRequestType() {
        return requestType;
    }

    public double getLatency() {
        return latency;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemStats that = (SystemStats) o;
        return Double.compare(that.startTime, startTime) == 0 && Double.compare(that.latency, latency) == 0 && Objects.equals(requestType, that.requestType) && Objects.equals(responseCode, that.responseCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, requestType, latency, responseCode);
    }

    @Override
    public String toString() {
        return "SystemStats{" +
                "startTime=" + startTime +
                ", requestType='" + requestType + '\'' +
                ", latency=" + latency +
                ", responseCode=" + responseCode +
                '}';
    }
}
