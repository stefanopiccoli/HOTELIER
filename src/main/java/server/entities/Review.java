package server.entities;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

public class Review {
    private String username;
    private Calendar date;
    private int rate;
    private int cleaning;
    private int position;
    private int services;
    private int quality;

    public Calendar getDate() {
        return this.date;
    }


    public Review(String username, int rate, int cleaning, int position, int services, int quality) {
        this.username = username;
        this.date = Calendar.getInstance();
        this.rate = rate;
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

    public int getRate() {
        return rate;
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

    public void setRate(int rate) {
        this.rate = rate;
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
