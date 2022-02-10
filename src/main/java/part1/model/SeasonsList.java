package part1.model;

import java.util.ArrayList;

/**
 * Class represents a list of seasons.
 */
public class SeasonsList {
    protected ArrayList<String> seasons = null;

    public SeasonsList(ArrayList<String> seasons) {
        this.seasons = seasons;
    }

    public SeasonsList(String seasonToAdd) {
        addSeason(seasonToAdd);
    }

    public SeasonsList() {
    }

    public ArrayList<String> getSeasons() {
        return seasons;
    }

    public SeasonsList addSeason(String season) {
        try {
            if (this.seasons == null) {
                this.seasons = new ArrayList<>();
            }
            this.seasons.add(season);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public String toString() {
        return "SeasonsList{" +
                "seasons=" + seasons +
                '}';
    }
}
