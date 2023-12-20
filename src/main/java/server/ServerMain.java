package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import server.entities.Hotel;
import server.entities.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class ServerMain implements Runnable {
    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private ExecutorService pool;
    private ArrayList<Hotel> hotels;
    private ArrayList<User> users;


    public static void main(String[] args) {
        ServerMain server = new ServerMain();
        server.run();
    }

    public ServerMain() {
        connections = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            loadFiles();
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (true) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            //TODO: handle
            e.printStackTrace();
        }
    }


    public class ConnectionHandler implements Runnable {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String choice;
        private User user;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            user = new User();
            try {
                do {
                    out = new PrintWriter(client.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    if (user.isLogged()) {
                        out.println(user.getUsername() + ", welcome to Hotelier!");
                        out.println("3. Logout");
                    } else {
                        out.println("Welcome to Hotelier!");
                        out.println("1. Register");
                        out.println("2. Login");
                        out.println("0. Close");
                    }
                    out.println("4. Search hotel");
                    out.println("5. Search all hotels");

                    choice = in.readLine();
                    switch (choice) {
                        case "1":
                            boolean done = false;
                            while (!done) {
                                out.println("REGISTER");
                                out.println("Choose your username:");
                                String username = in.readLine();
                                out.println("Choose your password:");
                                String password = in.readLine();
                                done = register(username, password);
                            }
                            break;
                        case "2":
                            done = false;
                            while (!done) {
                                out.println("LOGIN");
                                out.println("Choose your username:");
                                String username = in.readLine();
                                out.println("Choose your password:");
                                String password = in.readLine();
                                done = login(username, password);
                            }
                            break;
                        case "3":
                            logout();
                            break;
                        case "4":
                            out.println("SEARCH HOTEL");
                            out.println("Search hotel name:");
                            String hotelName = in.readLine();
                            out.println("Search hotel city:");
                            String hotelCity = in.readLine();
                            try {
                                Hotel found = searchHotel(hotelName, hotelCity);
                                out.println(found.printInfo());
                            } catch (NullPointerException e) {
                                out.println(e.getMessage());
                            }
                            break;
                        case "5":
                            out.println("SEARCH ALL HOTEL");
                            out.println("Search hotel city:");
                            String hotelsCity = in.readLine();
                            try {
                                Hotel[] found = searchAllHotels(hotelsCity);
                                for (Hotel hotel : found){
                                    out.println(hotel.printInfo()+"\n\n");
                                }
                            } catch (NullPointerException e) {
                                out.println(e.getMessage());
                            }
                            break;


                    }
                } while (!Objects.equals(choice, "0"));
                out.println("Exiting...");
                logout();
                shutdown();

            } catch (IOException e) {
                //TODO: handle
                e.printStackTrace();
            }
        }

        private boolean register(String username, String password) {
            if (User.checkUsername(username)) {
                if (users.stream().noneMatch(user -> user.getUsername().equals(username))) {
                    if (User.checkPassword(password)) {
                        try {
                            users.add(new User(username, password));
                            Writer writer = new FileWriter("Users.json");
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            gson.toJson(users.toArray(), writer);
                            writer.flush();
                            writer.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        out.println(username + " registered!");
                        return true;
                    } else {
                        out.println("Password too short!");
                        return false;
                    }
                } else {
                    out.println("Name already exists!");
                    return false;
                }
            } else {
                out.println("Username too short!");
                return false;
            }
        }

        private boolean login(String username, String password) {
            if (users.stream().anyMatch(user -> user.getUsername().equals(username))) {
                if (users.stream().filter(user -> user.getUsername().equals(username)).findFirst().get().getPassword().equals(password)) {
                    out.println("Logged as " + username);
                    user.setLogged(true);
                    user.setUsername(username);
                    return true;
                } else {
                    out.println("Wrong password!");
                    return false;
                }
            } else {
                out.println("User not registered!");
                return false;
            }
        }

        private boolean logout() {
            user.setLogged(false);
            user.setUsername(null);
            return true;
        }

        private Hotel searchHotel(String name, String city) throws NullPointerException {
            Hotel search = hotels.stream().filter((hotel -> hotel.getName().contains(name) && hotel.getCity().contains(city))).findFirst().orElse(null);
            if (search != null) return search;
            else throw new NullPointerException("Hotel not found with these parameters!");
        }

        private Hotel[] searchAllHotels(String city) throws NullPointerException {
            Hotel[] search = hotels.stream().filter((hotel -> hotel.getCity().contains(city))).toArray(Hotel[]::new);
            if (search.length > 0) return search;
            else throw new NullPointerException("There are no hotels with this parameter!");
        }

        public void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                //TODO: handle
                e.printStackTrace();
            }
        }


    }

    public void loadFiles() {
        try {
            Gson gson = new Gson();
            JsonReader hotelsReader = new JsonReader(new FileReader("Hotels.json"));
            JsonReader usersReader = new JsonReader(new FileReader("Users.json"));
            hotels = gson.fromJson(hotelsReader, new TypeToken<ArrayList<Hotel>>() {
            }.getType());
            users = gson.fromJson(usersReader, new TypeToken<ArrayList<User>>() {
            }.getType());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


}
