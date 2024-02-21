package server.entities;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
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

    private ArrayList<Double> calculateRate() {
        final double MAX_SIZE_WEIGHT = 1;
        double totalWeightedScore = 0;
        int reviewsSize = this.reviews.size();
        Calendar endDate = Calendar.getInstance();
//        endDate.clear();
//        endDate.set(Calendar.YEAR, 2024);
//        endDate.set(Calendar.MONTH, 12);
//        endDate.set(Calendar.DATE, 31);
//        double nreviews = reviewsSize;
//        double weightedNReviews = nreviews / 20;
//        double rateSum = 0;
//        double dateDiffSum = 0;
//        for (Review r : this.reviews) {
//            rateSum += r.getRate();
//            dateDiffSum += dateDiffSum + (double)(TimeUnit.MILLISECONDS.toDays(Math.abs(r.getDate().getTimeInMillis() - endDate.getTimeInMillis())))/1000;
//            System.out.println(dateDiffSum);
//        }
//        double avgRate = rateSum / nreviews;
//        double weightedAvgDate = Math.pow(Math.E, -0.1 * ( dateDiffSum /nreviews));
//        double rate = (1 * avgRate) + (0.5 * weightedNReviews) + (1 * weightedAvgDate);
        System.out.println(this.getName());
        for (Review r : this.reviews) {
            double deltaTime = TimeUnit.MILLISECONDS.toDays(Math.abs((endDate.getTimeInMillis() - r.getDate().getTimeInMillis())));
            double dataWeight = Math.exp(-0.00009 * deltaTime);
            double weightedScore = r.getRate() * dataWeight;
            totalWeightedScore += weightedScore;
        }
        double sizeWeight = reviewsSize/100.0;
        double rate = (totalWeightedScore / reviewsSize) * 1+sizeWeight ;
        this.rate = Math.round(rate * 100.0) / 100.0;
        System.out.printf("%s - %s - \n", rate, (totalWeightedScore / reviewsSize));

        return new ArrayList<Double>(Arrays.asList(rate));
    }

    public String printInfo() {
        ArrayList<Double> res = this.calculateRate();
        int maxAttributeLength = getMaxAttributeLength();
        int lineLength = Math.max(maxAttributeLength, 71); // Lunghezza massima della riga, puoi adattarla secondo le tue preferenze

        // Aggiungi spazio per la formattazione
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
            result.append(String.format("|    - %-17s:  %-1s  ( C:%-1s | P:%-1s | S:%-1s | Q:%-1s )   -   %-13s |\n", review.getUsername(), review.getRate(), review.getCleaning(), review.getPosition(), review.getServices(), review.getQuality(), new SimpleDateFormat("dd/MM/yyyy").format(review.getDate().getTime())));
        }
        result.append("+").append("-".repeat(lineLength)).append("+\n");
        result.append(String.format("%s", res.get(0)));

        return result.toString();
    }
}
