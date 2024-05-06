package server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import server.entities.Badge;
import server.entities.Hotel;
import server.entities.Review;
import server.entities.User;
import utils.ServerConfig;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import static utils.Cities.getCities;

public class ServerMain implements Runnable {
    protected static ServerMain server; //Istanza server Singleton
    private ArrayList<ConnectionHandler> connections; //Lista di tutti gli utenti connessi
    private ServerSocket serverSocket;
    private ExecutorService pool;
    protected ArrayList<Hotel> hotels = new ArrayList<>();
    protected ArrayList<User> users = new ArrayList<>();
    protected Map<String, Badge> userBadges;
    protected final Map<String, ArrayList<Hotel>> localRankings = new ConcurrentHashMap<>();
    //private final Object lock = new Object(); TODO: eliminare e usare this nei synchronized
    //private boolean rankingsChanged = false; TODO: eliminare
    private final String SERVER_ADDRESS = ServerConfig.getServerAddress();
    private final int SERVER_PORT = ServerConfig.getServerPort();
    protected final String MULTICAST_ADDRESS = ServerConfig.getMulticastAddress();
    protected final int MULTICAST_PORT = ServerConfig.getMulticastPort();
    protected final int UDPNOTIFIER_PERIOD = ServerConfig.getUdpNotifierPeriod();



    public static void main(String[] args) {
        Server().run();
    }

    // Metodo statico per ottenere l'istanza Singleton di Server
    public static synchronized ServerMain Server() {
        if (server == null) {
            server = new ServerMain();
        }
        return server;
    }

    private ServerMain() {
        connections = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            init();
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(SERVER_ADDRESS,SERVER_PORT));
            pool = Executors.newCachedThreadPool();
            Thread notifier = new Thread(new UDPNotifier());
            notifier.start();
            while (true) {
                Socket client = serverSocket.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            //TODO: handle
            e.printStackTrace();
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
            //TODO: Syncronized
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

    public void loadUserBadges() { //TODO: Inserire nel UDPNotifier
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
            localRankings.put(c, new ArrayList<>());
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
}
