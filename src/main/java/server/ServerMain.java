package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import server.entities.Badge;
import server.entities.Hotel;
import server.entities.Review;
import server.entities.User;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import static utils.Cities.getCities;

public class ServerMain implements Runnable {
    private ArrayList<ConnectionHandler> connections; //Lista di tutti gli utenti connessi
    private ServerSocket server;
    private ExecutorService pool;
    private ScheduledExecutorService scheduledPool;//TODO: OK?
    private ArrayList<Hotel> hotels;
    private ArrayList<User> users;
    private Map<String, Badge> userBadges;
    private final Map<String, ArrayList<Hotel>> localRankings = new ConcurrentHashMap<>();
    //private final Object lock = new Object(); TODO: eliminare e usare this nei synchronized
    //private boolean rankingsChanged = false; TODO: eliminare
    private static final int TCP_PORT = 9999; // Porta TCP per accettare le connessioni dei client
    private static final int UDP_PORT = 8888; // Porta UDP per inviare le notifiche ai client
    private static final String MULTICAST_GROUP = "230.0.0.0";
    private static final int MAX_PACKET_SIZE = 1024;


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
            init();
            server = new ServerSocket(TCP_PORT);
            pool = Executors.newCachedThreadPool();
            scheduledPool = Executors.newScheduledThreadPool(1);
            scheduledPool.scheduleAtFixedRate(new UDPNotifier(), 15, 15, TimeUnit.SECONDS);
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
//                    broadcast("user joined the chat...");
                    if (!user.isLogged()) {
                        out.println("Welcome to Hotelier!");
                        out.println("/register - Register");
                        out.println("/login - Log In");
                        out.println("/search - Search Hotel");
                        out.println("/searchall - Search all Hotels");
                        out.println("/rankings - Show rankings by city");
                        out.println("/exit - Exit");
                    } else {
                        out.println(user.getUsername() + ", welcome to Hotelier!");
                        out.println("/search - Search Hotel");
                        out.println("/searchall - Search all Hotels");
                        out.println("/review - Insert a Review");
                        out.println("/rankings - Show rankings by city");
                        out.println("/logout - Logout");
                        out.println("/exit - Exit");

                    }

                    choice = in.readLine();
                    switch (choice) {
                        case "/register":
                            boolean done = false;
                            if (!user.isLogged()) {
                                while (!done) {
                                    out.println("REGISTER");
                                    out.println("Choose your username:");
                                    String username = in.readLine();
                                    out.println("Choose your password:");
                                    String password = in.readLine();
                                    done = register(username, password);
                                }
                            } else {
                                out.println("Already registered with " + user.getUsername() + ".");
                            }
                            break;
                        case "/login":
                            done = false;
                            if (!user.isLogged()) {
                                while (!done) {
                                    out.println("LOGIN");
                                    out.println("Choose your username:");
                                    String username = in.readLine();
                                    out.println("Choose your password:");
                                    String password = in.readLine();
                                    done = login(username, password);
                                }
                            } else {
                                out.println("Already logged!");
                            }
                            break;
                        case "/logout":
                            if (user.isLogged()) {
                                logout();
                            } else {
                                out.println("You are not Logged In, try /login.");
                            }
                            break;
                        case "/search":
                            out.println("SEARCH HOTEL");
                            out.println("Search hotel name:");
                            String hotelName = in.readLine();
                            out.println("Search hotel city:");
                            String hotelCity = in.readLine();
                            try {
                                Hotel found = searchHotel(hotelName, hotelCity);
                                out.println(found.printInfo(userBadges));
                            } catch (NullPointerException e) {
                                out.println(e.getMessage());
                            }
                            break;
                        case "/searchall":
                            out.println("SEARCH ALL HOTEL");
                            out.println("Search hotel city:");
                            String hotelsCity = in.readLine();
                            try {
                                Hotel[] found = searchAllHotels(hotelsCity);
                                for (Hotel hotel : found) {
                                    out.println(hotel.printInfo(userBadges) + "\n\n");
                                }
                            } catch (NullPointerException e) {
                                out.println(e.getMessage());
                            }
                            break;
                        case "/review":
                            Hotel found = null;
                            if (user.isLogged()) {
                                out.println("SEARCH HOTEL");
                                try {
                                    do {
                                        out.println("Search hotel name:");
                                        hotelName = in.readLine();
                                        out.println("Search hotel city:");
                                        hotelCity = in.readLine();
                                        found = searchHotel(hotelName, hotelCity);
                                        out.println(found.printInfo(userBadges));
                                        out.println("Do you want to insert a review on this hotel? Y/n");
                                    } while (!Objects.equals(in.readLine(), "Y"));
                                    done = false;
                                    do {
                                        Review userReview = new Review(user.getUsername(), -1, -1, -1, -1, -1);
                                        out.println("Rate:");
                                        userReview.setRate(Integer.parseInt(in.readLine()));
                                        out.println("Cleaning:");
                                        userReview.setCleaning(Integer.parseInt(in.readLine()));
                                        out.println("Position:");
                                        userReview.setPosition(Integer.parseInt(in.readLine()));
                                        out.println("Services:");
                                        userReview.setServices(Integer.parseInt(in.readLine()));
                                        out.println("Quality:");
                                        userReview.setQuality(Integer.parseInt(in.readLine()));
                                        out.println(userReview.getRate());
                                        done = insertReview(found.getId(), userReview.getRate(), new ArrayList<Integer>(List.of(userReview.getCleaning(), userReview.getPosition(), userReview.getServices(), userReview.getQuality())));
                                    } while (!done);
                                } catch (NullPointerException e) {
                                    out.println(e.getMessage());
                                }
                            } else {
                                out.println("You are not Logged In, try /login.");
                            }
                            break;
                        case "/badge":
                            if (user.isLogged()) {
                                Badge badge = showMyBadge();
                                out.println("BADGE:");
                                out.println(user.getUsername() + " : " + badge.getBadge() + " (" + badge.getInitials() + ")");
                            } else {
                                out.println("You are not Logged In, try /login.");
                            }
                            break;
                        case "/rankings":
                            out.println("RANKINGS");
                            out.println("Select city:");
                            String city = in.readLine();
                            for (String key : localRankings.keySet()) {
                                int position = 1;
                                if (key.toLowerCase().contains(city.toLowerCase())) {
                                    for (Hotel h : localRankings.get(key)) {
                                        out.println(position + ". " + h.getName() + " (" + h.getRate() + ")");
                                        position++;
                                    }
                                    break;
                                }
                            }

                            break;
                        case "7":
                            for (Hotel h : hotels) {
                                h.calculateRate();
                            }
                            Writer writer = new FileWriter("Hotels.json");
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            gson.toJson(hotels.toArray(), writer);
                            writer.flush();
                            writer.close();
                            out.println("DONE");
//                            TODO: DELETE
                            break;
                        case "9":
                            loadUserBadges();
                            break;
                        case "10":
                            calculateLocalRankings();
                            break;


                    }
                } while (!Objects.equals(choice, "exit"));
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

        private boolean insertReview(int id, int GlobalScore, ArrayList<Integer> SingleScores) {
            Writer writer = null;
            boolean isValid = true;
            //Validazione dei punteggi
            for (Integer i : SingleScores)
                if (i < 0 || i > 5) {
                    isValid = false;
                    break;
                }
            //Validazione dello score totale
            if (GlobalScore >= 0 && GlobalScore <= 5 && isValid) {
                try {
                    //Ottengo l'hotel dall'id
                    Hotel found = hotels.stream().filter(hotel -> hotel.getId() == id).findFirst().orElse(null);
                    if (found != null) {
                        //Scrivo sul file degli hotel la recensione e ricalcolo il rate
                        writer = new FileWriter("Hotels.json");
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        found.addReview(new Review(user.getUsername(), GlobalScore, SingleScores.get(0), SingleScores.get(1), SingleScores.get(2), SingleScores.get(3)));
                        found.calculateRate();
                        hotels.set(hotels.indexOf(found), found);
                        gson.toJson(hotels.toArray(), writer);
                        writer.flush();
                        writer.close();
                        out.println("The review has been successfully submitted!");
                        return true;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            out.println("Ratings must be between 1 and 10!");
            return false;
        }

        private Badge showMyBadge() {
            if (userBadges.containsKey(user.getUsername())) {
                return userBadges.get(user.getUsername());
            } else {
                return new Badge(0); //Il badge non è ancora stato inserito
            }

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

    private class UDPNotifier implements Runnable {
        @Override
        public synchronized void run() {
            synchronized (localRankings) {
                Map<String, ArrayList<Hotel>> oldLocalRankings = new HashMap<>(localRankings);
                calculateLocalRankings();
                Map<String, Hotel> changedRankings = new HashMap<>();

                //Inserisco in changedRankings tutti gli hotel che sono passati in prima posizione
                for (String city : oldLocalRankings.keySet()) {
                    Hotel firstOld = oldLocalRankings.get(city).get(0);//TODO: check not null
                    Hotel firstNew = localRankings.get(city).get(0);//TODO: check not null
                    if (!firstOld.getName().equals(firstNew.getName())) {
                        changedRankings.putIfAbsent(city, firstNew);
                    }else {
                        System.out.print(oldLocalRankings.get("Aosta").get(0).getName()+" = "+localRankings.get("Aosta").get(0).getName()+"\n");
                    }
                }


                try {
                    //TODO: comment
                    InetAddress multicastGroup = InetAddress.getByName(MULTICAST_GROUP);
                    try (MulticastSocket socket = new MulticastSocket()) {
                        //Creazione del messaggio
                        String message = "";
                        if (changedRankings.isEmpty()) {
                            message = "Rankings updated! Nothing changed.\n";
                        } else {
                            message = "New hotels rank available: \n";
                            for (Hotel h : changedRankings.values()) {
                                message = message.concat(h.getName() + " is now first!\n");
                            }
                        }

                        byte[] buffer = message.getBytes();

                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, multicastGroup, UDP_PORT);
                        socket.send(packet);
                        //System.out.println("Notification sent to clients: " + message); TODO: Remove

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public void init() {
        try {
            //Carico i file nelle variabili del server all'avvio
            Gson gson = new Gson();
            JsonReader hotelsReader = new JsonReader(new FileReader("Hotels.json"));
            JsonReader usersReader = new JsonReader(new FileReader("Users.json"));
            hotels = gson.fromJson(hotelsReader, new TypeToken<ArrayList<Hotel>>() {
            }.getType());
            users = gson.fromJson(usersReader, new TypeToken<ArrayList<User>>() {
            }.getType());
            //TODO: WRITE CALCULATED READ
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        //Calcolo i rate a partire dalle recensioni
        for (Hotel h : hotels) {
            h.calculateRate();
        }
        //Calcolo i badge degli utenti a partire dalle recensioni
        loadUserBadges();
        //Calcolo dei rankings locali a partire dai Rate dei singoli hotel, raggruppati per Città
        calculateLocalRankings();
    }

    public void loadUserBadges() {
        userBadges = new ConcurrentHashMap<>();
        Map<String, Integer> nReviews = new HashMap<>();
        //Creazione map con username -> numero di recensioni per ogni utente che ha scritto almeno una recensione
        for (Hotel h : hotels)
            for (Review r : h.getReviews())
                if (nReviews.containsKey(r.getUsername()))
                    nReviews.put(r.getUsername(), nReviews.get(r.getUsername()) + 1);
                else
                    nReviews.putIfAbsent(r.getUsername(), 1);
        //Associazione per ogni utente in nReviews del badge, costruito da numero di recensioni effettuate
        for (Map.Entry<String, Integer> entry : nReviews.entrySet()) {
            userBadges.putIfAbsent(entry.getKey(), new Badge(entry.getValue()));
//            System.out.println("Chiave: " + entry.getKey() + ", Valore: " + entry.getValue() + ", Badge: " + userBadges.get(entry.getKey()).getBadge()); TODO: remove
        }
    }

    public void calculateLocalRankings() {
        //Creazione map con città -> Array di hotel presenti in essa
        localRankings.clear();
        for (String c : getCities()) {
            localRankings.put(c, new ArrayList<Hotel>());
        }
        for (Hotel h : hotels) {
            localRankings.get(h.getCity()).add(h);
        }

        // Definizione di un Comparator personalizzato per ordinare gli hotel per localRank in ordine decrescente
        Comparator<Hotel> rankComparator = Comparator.comparingDouble(Hotel::getRate).reversed();

        //Ordinamento decrescente degli hotel di ogni città
        for (ArrayList<Hotel> hotels : localRankings.values()) {
            hotels.sort(rankComparator);
            for (Hotel h : hotels)
                System.out.println(h.getName() + ", Punteggio:" + h.getRate() + " ," + h.getCity()); //TODO: remove
        }
    }

    public void broadcast(String message) {
        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.out.println(message);
            }
        }
    }

}
