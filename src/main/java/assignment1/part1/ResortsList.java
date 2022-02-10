package assignment1.part1;

/**
 * Class represents a list of resorts
 */

import java.util.ArrayList;

public class ResortsList {
    protected ArrayList<Resort> resorts = null;


    public ResortsList(ArrayList resorts) {
        this.resorts = resorts;
    }

    public ResortsList() {
    }

    /**
     * Get the list of resorts
     * @return
     */
    public ArrayList getResorts() {
        return resorts;
    }

    /**
     * Add a Resort object to the list of resorts
     * @param resortName
     * @param resortId
     */
    public ResortsList addResort(String resortName, Integer resortId) {
        try {
            if (this.resorts == null) {
                Resort resort = new Resort(resortName, resortId);
                this.resorts = new ArrayList<>();
                this.resorts.add(resort);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public String toString() {
        return "ResortsList{" +
                "resorts=" + resorts +
                '}';
    }
}
