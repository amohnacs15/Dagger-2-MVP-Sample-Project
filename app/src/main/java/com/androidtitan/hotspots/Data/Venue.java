package com.androidtitan.hotspots.Data;

public class Venue {

    public long id;
    private String name;
    private String city;
    private String category;
    private String venueId;
    private float rating;

    public Venue() {
        this.name = "";
        this.city = "";
        this.setCategory("");
        this.venueId = "";
    }

    public String getCity() {
        if (city.length() > 0) {
            return city;
        }
        return city;
    }

    public void setCity(String city) {
        if (city != null) {
            this.city = city.replaceAll("\\(", "").replaceAll("\\)", "");
            ;
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getVenueId() {
        return venueId;
    }

    public void setVenueId(String venueId) {
        this.venueId = venueId;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }
}
