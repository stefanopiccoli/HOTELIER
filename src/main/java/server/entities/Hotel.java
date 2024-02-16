package server.entities;

import java.util.ArrayList;
import java.util.Map;

public class Hotel {
    private int id;
    private String name;
    private String description;
    private String city;
    private String phone;
    private String[] services;
    private String rate;
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

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
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

        maxLength = Math.max(maxLength, this.rate.length());

        return maxLength;
    }

    public String printInfo() {
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
        result.append(String.format("| %-15s : %-55s |\n", "Rate", this.rate));
        result.append("+").append("-".repeat(lineLength)).append("+\n");
        result.append("| Ratings: ").append(" ".repeat(lineLength - 10)).append("|\n");
        result.append(String.format("|   - %-10s : %-56s |\n", "Cleaning", this.ratings.get("cleaning")));
        result.append(String.format("|   - %-10s : %-56s |\n", "Position", this.ratings.get("position")));
        result.append(String.format("|   - %-10s : %-56s |\n", "Services", this.ratings.get("services")));
        result.append(String.format("|   - %-10s : %-56s |\n", "Quality", this.ratings.get("quality")));
        result.append("+").append("-".repeat(lineLength)).append("+\n");
        result.append("| Services: ").append(" ".repeat(lineLength - 11)).append("|\n");
        for (String service : this.services) {
            result.append(String.format("|   - %-69s |\n", service));
        }
        result.append("+").append("-".repeat(lineLength)).append("+\n");

        return result.toString();
    }
}
