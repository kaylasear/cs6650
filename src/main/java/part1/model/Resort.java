package part1.model;

/**
 * Class represents a Resort with a resort name and resort ID.
 */
public class Resort {
    protected String resortName;
    protected Integer resortId;

    public Resort(String resortName, Integer resortId) {
        this.resortName = resortName;
        this.resortId = resortId;

    }

    /**
     * Getters and setters
     * @return
     */
    public String getResortName() {
        return resortName;
    }

    public void setResortName(String resortName) {
        this.resortName = resortName;
    }

    public Integer getResortId() {
        return resortId;
    }

    public void setResortId(Integer resortId) {
        this.resortId = resortId;
    }

    @Override
    public String toString() {
        return "Resort{" +
                "resortName='" + resortName + '\'' +
                ", resortId=" + resortId +
                '}';
    }
}
