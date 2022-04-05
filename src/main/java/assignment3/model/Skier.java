package assignment3.model;

/**
 * Class represents details of a Skier's lift ride.
 */
public class Skier {
    protected Integer resortId;
    protected String seasonId;
    protected String dayId;
    protected Integer skierId;
    protected Integer vertical;
    protected LiftRide liftRide;

    public Skier(Integer resortId, String seasonId, String dayId, Integer skierId, LiftRide liftRide) {
        this.resortId = resortId;
        this.seasonId = seasonId;
        this.dayId = dayId;
        this.skierId = skierId;
        this.liftRide = liftRide;
        this.vertical = liftRide.getLiftId()*10;
    }

    public Skier() {
    }

    public Integer getResortId() {
        return resortId;
    }

    public String getSeasonId() {
        return seasonId;
    }

    public String getDayId() {
        return dayId;
    }

    public Integer getSkierId() {
        return skierId;
    }

    public Integer getVertical() {
        return vertical;
    }

    public void setVertical(Integer liftId) {
        this.vertical = liftId*10;
    }

    public LiftRide getLiftRide() {
        return liftRide;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{")
                .append("\"resortId\":" + resortId + " ,")
                .append("\"seasonId\":" + seasonId + " ,")
                .append("\"dayId\":" + dayId + " ,")
                .append("\"skierId\":" + skierId + " ,")
                .append("\"vertical\":" + vertical + " ,")
                .append("\"liftRide\":" + liftRide)
                .append("}").toString();
    }

    public String toStringData() {
        return "resortId=" + resortId +
                ", seasonId=" + seasonId  +
                ", dayId=" + dayId +
                ", skierId=" + skierId +
                ", vertical=" + vertical +
                ", liftRide=" + liftRide ;
    }
}
