package server.entities;

import java.util.Calendar;

public class Review {
    private String username;
    private final Calendar date;
    private int globalScore;
    private int cleaning;
    private int position;
    private int services;
    private int quality;

    public Calendar getDate() {
        return this.date;
    }


    public Review(String username, int globalScore, int cleaning, int position, int services, int quality) {
        this.username = username;
        this.date = Calendar.getInstance();
        this.globalScore = globalScore;
        this.cleaning = cleaning;
        this.position = position;
        this.services = services;
        this.quality = quality;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getGlobalScore() {
        return globalScore;
    }

    public int getCleaning() {
        return cleaning;
    }

    public int getPosition() {
        return position;
    }

    public int getServices() {
        return services;
    }

    public int getQuality() {
        return quality;
    }

    public void setGlobalScore(int globalScore) {
        this.globalScore = globalScore;
    }

    public void setCleaning(int cleaning) {
        this.cleaning = cleaning;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setServices(int services) {
        this.services = services;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }
}
