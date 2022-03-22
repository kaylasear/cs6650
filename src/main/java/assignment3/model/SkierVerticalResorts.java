package assignment3.model;

/**
 * Class represents total number of skiers at given season.
 */
public class SkierVerticalResorts {
    protected String seasonId;
    protected Integer totalVert;

    public SkierVerticalResorts(String seasonId, Integer totalVert) {
        this.seasonId = seasonId;
        this.totalVert = totalVert;
    }

    public SkierVerticalResorts() {
    }

    public String getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(String seasonId) {
        this.seasonId = seasonId;
    }

    public Integer getTotalVert() {
        return totalVert;
    }

    public void setTotalVert(Integer totalVert) {
        this.totalVert = totalVert;
    }

    @Override
    public String toString() {
        return "SkierVerticalResorts{" +
                "seasonId='" + seasonId + '\'' +
                ", totalVert=" + totalVert +
                '}';
    }
}
