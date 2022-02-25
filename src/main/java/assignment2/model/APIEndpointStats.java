package assignment2.model;

/**
 * Class represents an endpoint statistic containing a URL, operation (GET/POST), mean, and max.
 */
public class APIEndpointStats {
    protected String URL;
    protected String operation;
    protected Integer mean;
    protected Integer max;

    public APIEndpointStats(String URL, String operation, Integer mean, Integer max) {
        this.URL = URL;
        this.operation = operation;
        this.mean = mean;
        this.max = max;
    }

    /**
     * Getters and Setters
     * @return
     */
    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Integer getMean() {
        return mean;
    }

    public void setMean(Integer mean) {
        this.mean = mean;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    @Override
    public String toString() {
        return "APIEndpointStats{" +
                "URL='" + URL + '\'' +
                ", operation='" + operation + '\'' +
                ", mean=" + mean +
                ", max=" + max +
                '}';
    }
}
