package assignment3.model;

/**
 * Resort with season
 */
public class ResortIDSeasons {
    protected String year;

    public ResortIDSeasons(String year) {
        this.year = year;
    }

    public ResortIDSeasons() {
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return "ResortIDSeasons{" +
                "year='" + year + '\'' +
                '}';
    }
}
