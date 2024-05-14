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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

import static utils.Cities.getCities;

//ServerMain: Classe principale del server che si occupa di istanziare un Singleton accessibile dal package. Memorizza e persiste i dati accessibili dagli utenti e fornisce i metodi di salvataggio e aggiornamento di essi.
public class ServerMain implements Runnable {
    protected static ServerMain server; //Istanza server Singleton
    private ArrayList<ConnectionHandler> connections; //Lista di tutti gli utenti connessi
    private ServerSocket serverSocket; //Socket del server
    private ExecutorService pool; //Pool di ConnectionHandler per ogni utente
    protected ArrayList<Hotel> hotels = new ArrayList<>(); //Lista di hotel del server
    protected ArrayList<User> users = new ArrayList<>();//Lista degli utenti che possono autenticarsi sul server
    protected final Map<String, Badge> userBadges = new ConcurrentHashMap<>();//Map Concorrente di utenti che hanno recensito almeno un hotel -> il badge più alto che hanno mai raggiunto
    protected final Map<String, ArrayList<Hotel>> localRankings = new ConcurrentHashMap<>();//Map Concorrente di città -> Hotel appartenenti a essa ordinati per punteggio decrescente
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
            serverSocket.bind(new InetSocketAddress(SERVER_ADDRESS, SERVER_PORT)); //Inizializzazione indirizzo e porta del socket
            pool = Executors.newCachedThreadPool(); //Inizializzazione CachedThreadPool
            Thread notifier = new Thread(new UDPNotifier()); //Inizializzazione thread per scansione e invio notifiche
            notifier.start(); //Esecuzione del thread
            while (true) {
                Socket client = serverSocket.accept(); //Ogni richiesta in arrivo dai client viene accettata
                ConnectionHandler handler = new ConnectionHandler(client); //Viene inizializzato un ConnectionHandler con la connessione stabilita
                connections.add(handler); //Viene aggiunta la connessione alla lista
                pool.execute(handler); //Viene eseguito il ConnectionHandler che si occuperà di tutte le richieste del client
            }
        } catch (IOException e) {
            System.err.println("Error while starting the server: "+e.getMessage());
            System.exit(1);
        }
    }


    public void init() {
        try {
            //Carico i file nelle variabili del server all'avvio
            Gson gson = new Gson();
            JsonReader hotelsReader = new JsonReader(new FileReader("Hotels.json"));
            JsonReader usersReader = new JsonReader(new FileReader("Users.json"));
            synchronized (hotels) {
                hotels = gson.fromJson(hotelsReader, new TypeToken<ArrayList<Hotel>>() {
                }.getType()); //Caricamento Hotels da json
            }
            synchronized (users) {
                users = gson.fromJson(usersReader, new TypeToken<ArrayList<User>>() {
                }.getType()); //Caricamento Users da json
            }
        } catch (FileNotFoundException e) {
            System.err.println("File Hotels.json or Users.json not found! Place them in the main directory.");
            System.exit(1);
        }
        //Calcolo i rate a partire dalle recensioni
        synchronized (hotels) {
            for (Hotel h : hotels) {
                h.calculateRate();
            }
        }
        //Calcolo i badge degli utenti a partire dalle recensioni
        loadUserBadges();
        //Calcolo dei rankings locali a partire dai Rate dei singoli hotel, raggruppati per Città
        calculateLocalRankings();
    }

    public void loadUserBadges() {
        userBadges.clear();
        Map<String, Integer> nReviews = new HashMap<>();
        //Creazione map con username -> numero di recensioni per ogni utente che ha scritto almeno una recensione
        synchronized (hotels) {
            for (Hotel h : hotels)
                for (Review r : h.getReviews())
                    if (nReviews.containsKey(r.getUsername()))
                        nReviews.put(r.getUsername(), nReviews.get(r.getUsername()) + 1);
                    else
                        nReviews.putIfAbsent(r.getUsername(), 1);
        }
        //Associazione per ogni utente in nReviews del badge, costruito da numero di recensioni effettuate
        for (Map.Entry<String, Integer> entry : nReviews.entrySet()) {
            userBadges.putIfAbsent(entry.getKey(), new Badge(entry.getValue()));
        }
    }

    public void calculateLocalRankings() {
        //Creazione map con città -> Array di hotel presenti in essa
        localRankings.clear();
        //Inizializzazione della map con i capoluoghi italiani e un array di hotel vuoto
        for (String c : getCities()) {
            localRankings.put(c, new ArrayList<>());
        }
        //Aggiunta di ogni hotel alla città di appartenenza
        synchronized (hotels) {
            for (Hotel h : hotels) {
                synchronized (localRankings.get(h.getCity())) { //Evitare accessi concorrenti all' ArrayList di hotel, che non è un tipo thread safe, anche se si trova in una ConcurrentMap
                    localRankings.get(h.getCity()).add(h);
                }
            }
        }

        // Definizione di un Comparator personalizzato per ordinare gli hotel per localRank in ordine decrescente
        Comparator<Hotel> rankComparator = Comparator.comparingDouble(Hotel::getRate).reversed();

        System.out.println("\n\n["+ LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) +"] Updating local rankings...");
        //Ordinamento decrescente degli hotel di ogni città
        for (ArrayList<Hotel> hotels : localRankings.values()) {
            hotels.sort(rankComparator);
            for (Hotel h : hotels)
                System.out.println(h.getName() + " - Punteggio: " + h.getRate() + " - " + h.getCity());
        }
    }
}
