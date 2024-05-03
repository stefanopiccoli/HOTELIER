package server.entities;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Hotel {
    private int id;
    private String name;
    private String description;
    private String city;
    private String phone;
    private String[] services;
    private double rate;
    private Map<String, Integer> ratings;
    private ArrayList<Review> reviews;
    private int localRank;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String[] getServices() {
        return services;
    }

    public void setServices(String[] services) {
        this.services = services;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public Map<String, Integer> getRatings() {
        return ratings;
    }

    public void setRatings(Map<String, Integer> ratings) {
        this.ratings = ratings;
    }

    public ArrayList<Review> getReviews() {
        return reviews;
    }

    public void addReview(Review review) {
        if (this.reviews != null)
            this.reviews.add(review);
        else {
            this.reviews = new ArrayList<>();
            this.reviews.add(review);
        }
    }

    private int getMaxAttributeLength() {
        int maxLength = 0;
        maxLength = Math.max(maxLength, this.name.length());
        maxLength = Math.max(maxLength, this.description.length());
        maxLength = Math.max(maxLength, this.city.length());
        maxLength = Math.max(maxLength, this.phone.length());

        for (String service : this.services) {
            maxLength = Math.max(maxLength, service.length());
        }

        return maxLength;
    }

    public void calculateRate() {
        final double TIME_WEIGHT = -0.0005;
        final double MAX_REVIEW = 100;
        double totalWeightedScore = 0;
        double cleaning = 0.0;
        double position = 0.0;
        double services = 0.0;
        double quality = 0.0;
        int reviewsSize = this.reviews.size();
        //Inizializzazione alla data attuale
        Calendar endDate = Calendar.getInstance();

        for (Review r : this.reviews) {
            double deltaTime = TimeUnit.MILLISECONDS.toDays(Math.abs((endDate.getTimeInMillis() - r.getDate().getTimeInMillis()))); //Differenza in giorni dalla giornata attuale alla data della recensione
            double weightedDelta = Math.exp(TIME_WEIGHT * deltaTime); //Data pesata, (1 >= weightedDelta >= 0)
            double weightedScore = r.getGlobalScore() * weightedDelta; //Voto pesato per attualità, (1<= weightedScore <= 5)
            totalWeightedScore += weightedScore; //Somma di tutte le recensioni pesate per attualità
            cleaning+=r.getCleaning();
            position+=r.getPosition();
            services+=r.getServices();
            quality+=r.getQuality();
        }
        double sizeWeight = reviewsSize / MAX_REVIEW;
        double rate = (totalWeightedScore / reviewsSize) + sizeWeight; //Media dei voti sommata al premio delle quantità di recensioni
        if (rate>5.00) rate = 5.00; //Nel caso viene superato il punteggio massimo di 5 verrà mostrato 5, anche se il teorico sará più elevato e solido (più difficile da abbassare rispetto a un 5 reale)

        this.rate = Math.round(rate * 100.0) / 100.0; //Arrotondamento alla seconda cifra dopo la virgola
        this.ratings.put("cleaning",(int)Math.round(cleaning/reviewsSize));
        this.ratings.put("position",(int)Math.round(position/reviewsSize));
        this.ratings.put("services",(int)Math.round(services/reviewsSize));
        this.ratings.put("quality",(int)Math.round(quality/reviewsSize));
    }

    public String printInfo(Map<String, Badge> userBadges) {
        this.calculateRate();
        int maxAttributeLength = getMaxAttributeLength();
        int lineLength = Math.max(maxAttributeLength, 71); // Lunghezza massima della riga


        //Spazio per la formattazione
        lineLength += 4;

        StringBuilder result = new StringBuilder();
        result.append("+").append("-".repeat(lineLength)).append("+\n");
        result.append(String.format("| %-15s : %-55s |\n", "Name", this.name));
        result.append(String.format("| %-15s : %-55s |\n", "Description", this.description));
        result.append(String.format("| %-15s : %-55s |\n", "City", this.city));
        result.append(String.format("| %-15s : %-55s |\n", "Phone", this.phone));
        result.append("+").append("-".repeat(lineLength)).append("+\n");
        result.append(String.format("| %-15s : %-55s |\n", "Rate", new DecimalFormat("0.00").format(this.rate)));
        result.append("+").append("-".repeat(lineLength)).append("+\n");
        result.append("| Ratings: ").append(" ".repeat(lineLength - 10)).append("|\n");
        result.append(String.format("|   - %-11s : %-55s |\n", "Cleaning", this.ratings.get("cleaning")));
        result.append(String.format("|   - %-11s : %-55s |\n", "Position", this.ratings.get("position")));
        result.append(String.format("|   - %-11s : %-55s |\n", "Services", this.ratings.get("services")));
        result.append(String.format("|   - %-11s : %-55s |\n", "Quality", this.ratings.get("quality")));
        result.append("+").append("-".repeat(lineLength)).append("+\n");
        result.append("| Services: ").append(" ".repeat(lineLength - 11)).append("|\n");
        for (String service : this.services) {
            result.append(String.format("|   - %-69s |\n", service));
        }
        result.append("+").append("-".repeat(lineLength)).append("+\n");
        result.append("| Reviews: ").append(" ".repeat(lineLength - 10)).append("|\n");
        for (Review review : this.reviews) {
            Badge badge = new Badge(0); //Inizializzazione a un badge vuoto in caso di nuovi utenti
            if (userBadges.containsKey(review.getUsername()))
                badge = userBadges.get(review.getUsername()); //Badge già esistente
            result.append(String.format("| (%-2s) - %-15s:  %-1s  ( C:%-1s | P:%-1s | S:%-1s | Q:%-1s )   -   %-13s |\n",badge.getInitials(), review.getUsername(), review.getGlobalScore(), review.getCleaning(), review.getPosition(), review.getServices(), review.getQuality(), new SimpleDateFormat("dd/MM/yyyy").format(review.getDate().getTime())));
        }
        result.append("+").append("-".repeat(lineLength)).append("+\n");

        return result.toString();
    }
}
