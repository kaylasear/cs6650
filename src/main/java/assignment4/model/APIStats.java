package assignment4.model;

import java.util.ArrayList;

/**
 * Class represents API stats that contains information of endpoint stats.
 */
public class APIStats {
    protected ArrayList<APIEndpointStats> endpointStats = null;

    public APIStats(ArrayList<APIEndpointStats> endpointStats) {
        this.endpointStats = endpointStats;
    }

    public APIStats() {
    }

    public ArrayList<APIEndpointStats> getEndpointStats() {
        return endpointStats;
    }

    /**
     * Add an EnpointStat object to the list of stats
     * @param url
     * @param operation
     * @param mean
     * @param max
     */
    public APIStats addStatistic(String url, String operation, Integer mean, Integer max) {
        try {
            APIEndpointStats stat = new APIEndpointStats(url, operation, mean, max);
            if (this.endpointStats == null) {
                this.endpointStats = new ArrayList<>();
            }
            this.endpointStats.add(stat);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return this;
    }
}
