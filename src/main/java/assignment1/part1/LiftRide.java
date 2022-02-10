package assignment1.part1;

/**
 * Class represents a lift ride with time, liftId, and wait time.
 */
public class LiftRide {
    protected Integer time;
    protected Integer liftId;
    protected Integer waitTime;

    public LiftRide(Integer time, Integer liftId, Integer waitTime) {
        this.time = time;
        this.liftId = liftId;
        this.waitTime = waitTime;
    }

    /**
     * Getters and Setters
     * @return
     */
    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public Integer getLiftId() {
        return liftId;
    }

    public void setLiftId(Integer liftId) {
        this.liftId = liftId;
    }

    public Integer getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(Integer waitTime) {
        this.waitTime = waitTime;
    }

    @Override
    public String toString() {
        return "LiftRide{" +
                "time=" + time +
                ", liftId=" + liftId +
                ", waitTime=" + waitTime +
                '}';
    }
}
