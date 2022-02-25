package assignment2.model;

import java.util.ArrayList;

/**
 * Class represents a list of total skiers at a given season.
 */
public class SkierVertical {
    protected ArrayList<SkierVerticalResorts> resorts = null;

    public SkierVertical(ArrayList<SkierVerticalResorts> resorts) {
        this.resorts = resorts;
    }

    public SkierVertical() {
    }

    public ArrayList<SkierVerticalResorts> getResorts() {
        return resorts;
    }

    /**
     * Add a vertical object to the list
     * @param seasonId
     * @param vert
     */
    public SkierVertical addVertical(String seasonId, Integer vert) {
        try {
            SkierVerticalResorts skierVerticalResorts = new SkierVerticalResorts(seasonId, vert);
            if (this.resorts == null) {
                this.resorts = new ArrayList<>();
            }
            this.resorts.add(skierVerticalResorts);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public String toString() {
        return "SkierVertical{" +
                "resorts=" + resorts +
                '}';
    }
}
