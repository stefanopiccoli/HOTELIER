package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import server.entities.Badge;
import server.entities.Hotel;
import server.entities.Review;
import server.entities.User;

import java.io.*;
import java.net.*;
import java.util.*;


import static server.ServerMain.Server;

//ConnectionHandler si occupa di gestire ogni connessione Client/Server. Ogni istanza corrisponde a un client connesso al server.
//Gestisce tutti gli input inviati dal client, li interpreta e risponde con il risultato richiesto.
public class ConnectionHandler implements Runnable {
    private final Socket client;

    private BufferedReader in;
    private PrintWriter out;
    private User user;

    public ConnectionHandler(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        user = new User(); //Utente in sessione
        try {
            String choice; //Variabile che contiene il comando inviato dal client
            do {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                if (!user.isLogged()) {//Menu per utenti non registrati
                    out.println("Welcome to Hotelier!");
                    out.println("/register - Register");
                    out.println("/login - Log In");
                    out.println("/search - Search Hotel");
                    out.println("/searchall - Search all Hotels");
                    out.println("/rankings - Show rankings by city");
                    out.println("/exit - Exit");
                } else {//Menu per utenti registrati
                    out.println(user.getUsername() + ", welcome to Hotelier!");
                    out.println("/search - Search Hotel");
                    out.println("/searchall - Search all Hotels");
                    out.println("/review - Insert a Review");
                    out.println("/badge - Show my top badge");
                    out.println("/rankings - Show rankings by city");
                    out.println("/logout - Logout");
                    out.println("/exit - Exit");
                }

                choice = in.readLine();//Acquisizione del comando
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
                                done = register(username, password);//Procedura di registrazione
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
                                done = login(username, password);//Procedura di login
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
                            out.println(found.printInfo(Server().userBadges));
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
                                out.println(hotel.printInfo(Server().userBadges) + "\n\n");
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
                                    out.println(found.printInfo(Server().userBadges));
                                    out.println("Do you want to insert a review on this hotel? Y/n");
                                } while (!Objects.equals(in.readLine(), "Y"));
                                done = false;
                                do {
                                    Review userReview = new Review(user.getUsername(), -1, -1, -1, -1, -1);
                                    out.println("Rate:");
                                    userReview.setGlobalScore(Integer.parseInt(in.readLine()));
                                    out.println("Cleaning:");
                                    userReview.setCleaning(Integer.parseInt(in.readLine()));
                                    out.println("Position:");
                                    userReview.setPosition(Integer.parseInt(in.readLine()));
                                    out.println("Services:");
                                    userReview.setServices(Integer.parseInt(in.readLine()));
                                    out.println("Quality:");
                                    userReview.setQuality(Integer.parseInt(in.readLine()));
                                    out.println(userReview.getGlobalScore());
                                    done = insertReview(found.getId(), userReview.getGlobalScore(), new ArrayList<>(List.of(userReview.getCleaning(), userReview.getPosition(), userReview.getServices(), userReview.getQuality())));
                                } while (!done);
                            } catch (RuntimeException e) {
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
                        for (String key : Server().localRankings.keySet()) {
                            int position = 1;
                            if (key.toLowerCase().contains(city.toLowerCase())) {
                                for (Hotel h : Server().localRankings.get(key)) {
                                    out.println(position + ". " + h.getName() + " (" + h.getRate() + ")");
                                    position++;
                                }
                                break;
                            }
                        }

                        break;
                    case "/refresh"://Aggiornamento manuale del rate degli hotel, dei ranking locali, dei badge e riscrittura del file json (comando non visibile nell'interfaccia utente)
                        synchronized (Server().hotels) {
                            for (Hotel h : Server().hotels) {
                                h.calculateRate();
                            }
                            Writer writer = new FileWriter("Hotels.json");
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            gson.toJson(Server().hotels.toArray(), writer);
                            writer.flush();
                            writer.close();
                            out.println("DONE");
                        }
                        Server().loadUserBadges();
                        Server().calculateLocalRankings();
                        break;

                    case "/exit":
                        break;

                    default:
                        out.println("Invalid command.");
                        break;
                }
            } while (!Objects.equals(choice, "/exit"));
            out.println("Disconnected.");
            logout();
            shutdown();

        } catch (IOException e) {
            out.println("Server error: "+e.getMessage());
        }
    }

    //Registrazione utente
    private boolean register(String username, String password) {
        synchronized (Server().users) {
            if (User.checkUsername(username)) {//Controllo requisiti dei caratteri
                if (Server().users.stream().noneMatch(user -> user.getUsername().equals(username))) {//Controllo sull'unicità dell'username
                    if (User.checkPassword(password)) {//Controllo requisiti dei caratteri
                        try {
                            //Aggiunta dell'utente e scrittura file
                            Server().users.add(new User(username, password));
                            Writer writer = new FileWriter("Users.json");
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            gson.toJson(Server().users.toArray(), writer);
                            writer.flush();
                            writer.close();
                        } catch (IOException e) {
                            out.println("Error, not registered: "+e.getMessage());
                            return false;
                        }
                        out.println(username + " registered!");
                        return true; //Registrazione avvenuta con successo
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
    }

    //Accesso utente
    private boolean login(String username, String password) {
        synchronized (Server().users) {
            if (Server().users.stream().anyMatch(user -> user.getUsername().equals(username))) {//Controllo della presenza dell'username registrato
                if (Server().users.stream().filter(user -> user.getUsername().equals(username)).findFirst().get().getPassword().equals(password)) {//Controllo della corrispondenza tra password immessa e password registrata
                    out.println("Logged as " + username);
                    user.setLogged(true);
                    user.setUsername(username);
                    return true;//Login effettuato e sessione memorizzata
                } else {
                    out.println("Wrong password!");
                    return false;
                }
            } else {
                out.println("User not registered!");
                return false;
            }
        }
    }

    //Disconnessione utente
    private void logout() {//Chiusura della sessione di login
        user.setLogged(false);
        user.setUsername(null);
    }

    //Ricerca hotel per nome e città
    private Hotel searchHotel(String name, String city) throws NullPointerException {//
        synchronized (Server().hotels) {
            Hotel search = Server().hotels.stream().filter((hotel -> hotel.getName().contains(name) && hotel.getCity().contains(city))).findFirst().orElse(null);//Ricerca hotel contenente nome e città inserite
            if (search != null) return search;
            else throw new NullPointerException("Hotel not found with these parameters!");
        }
    }

    //Ricerca di tutti gli hotel presenti in una città
    private Hotel[] searchAllHotels(String city) throws NullPointerException {
        synchronized (Server().hotels) {
            Comparator<Hotel> rankComparator = Comparator.comparingDouble(Hotel::getRate).reversed();//Definizione di un comparatore usato per ordinare per rank decrescente
            Hotel[] search = Server().hotels.stream().filter((hotel -> hotel.getCity().contains(city))).sorted(rankComparator).toArray(Hotel[]::new);//Ricerca degli hotel che contengono la città inserita
            if (search.length > 0) return search;
            else throw new NullPointerException("There are no hotels with this parameter!");
        }
    }

    //Inserimento recensione
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
                synchronized (Server().hotels) {
                    //Ottengo l'hotel dall'id
                    Hotel found = Server().hotels.stream().filter(hotel -> hotel.getId() == id).findFirst().orElse(null);
                    if (found != null) {
                        //Scrittura sul file degli hotel, della recensione e ricalcolo del rate
                        writer = new FileWriter("Hotels.json");
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        found.addReview(new Review(user.getUsername(), GlobalScore, SingleScores.get(0), SingleScores.get(1), SingleScores.get(2), SingleScores.get(3)));
                        found.calculateRate();
                        Server().hotels.set(Server().hotels.indexOf(found), found);
                        gson.toJson(Server().hotels.toArray(), writer);
                        writer.flush();
                        writer.close();
                        out.println("The review has been successfully submitted!");
                        return true;
                    }
                }
            } catch (IOException e) {
                out.println("Error, review not submitted: "+e.getMessage());
            }
        }
        out.println("Ratings must be between 1 and 10!");
        return false;
    }

    //Mostra il badge piú importante raggiunto dall'utente in sessione
    private Badge showMyBadge() {
        if (Server().userBadges.containsKey(user.getUsername())) {
            return Server().userBadges.get(user.getUsername());
        } else {
            return new Badge(0); //Il badge non è ancora stato inserito
        }

    }

    //Chiusura degli stream e del socket
    public void shutdown() {
        try {
            in.close(); //Chiude lo stream di input (che invia al client)
            out.close();//Chiude lo stream di output (che riceve dal server)
            if (!client.isClosed()) {//Verifica se il socket è chiuso
                client.close();//Chiude il socket
            }
        } catch (IOException e) {
            out.println("An error occurred while closing the connection.");
        }
    }


}
