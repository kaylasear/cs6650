package assignment1.part2.model;

/**
 * Class represents a resort that has a name and total number of skiers.
 */
public class ResortSkier {
    protected String resortName;
    protected Integer numSkiers;

    public ResortSkier(String resortName, Integer numSkiers) {
        this.resortName = resortName;
        this.numSkiers = numSkiers;
    }

    /**
     * Getters and Setters
     * @return
     */
    public String getResortName() {
        return resortName;
    }

    public void setResortName(String resortName) {
        this.resortName = resortName;
    }

    public Integer getNumSkiers() {
        return numSkiers;
    }

    public void setNumSkiers(Integer numSkiers) {
        this.numSkiers = numSkiers;
    }

    @Override
    public String toString() {
        return "ResortSkier{" +
                "resortName='" + resortName + '\'' +
                ", numSkiers=" + numSkiers +
                '}';
    }
}
