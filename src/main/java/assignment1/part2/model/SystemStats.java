package assignment1.part2.model;

public class SystemStats {
    private double startTime;
    private String requestType;
    private double latency;
    private Integer responseCode;

    public SystemStats(double startTime, String requestType, double latency, Integer responseCode) {
        this.startTime = startTime;
        this.requestType = requestType;
        this.latency = latency;
        this.responseCode = responseCode;
    }

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
}
