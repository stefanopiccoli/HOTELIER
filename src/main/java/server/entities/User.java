package server.entities;

import java.util.ArrayList;

public class User {
    private String username;
    private String password;
    transient private boolean isLogged;

    public User() {
        this.isLogged = false;
    }
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }


    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isLogged() {
        return isLogged;
    }

    public void setLogged(boolean logged) {
        isLogged = logged;
    }

    public static boolean checkUsername(String username){
        return (!username.isBlank() && username.length()>=8);
    }
    public static boolean checkPassword(String password){
        return (!password.isBlank() && password.length()>=8);
    }

    public Badge showMyBadges(ArrayList<Hotel> hotels){
        int reviewCount = 0;
        for (Hotel h: hotels){
            for (Review r: h.getReviews()){
                if (r.getUsername().equals(this.username)){
                    reviewCount++;
                }
            }
        }

        for (Badge.BadgeType type : Badge.BadgeType.values()) {
            if (reviewCount >= type.getBoundFrom() && reviewCount <= type.getBoundTo()) {
                return new Badge(type);
            }
        }
        return null;
    }
}
