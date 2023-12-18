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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
                    if (user.isLogged())
                        out.println(user.getUsername() + ", welcome to Hotelier!");
                    else
                        out.println("Welcome to Hotelier!");
                    out.println("1. Register");
                    out.println("2. Login");
                    out.println("0. Esci");
                    choice = in.readLine();
                    switch (choice) {
                        case "1":
                            boolean done = false;
                            while (!done) {

                                out.println("Choose your username:");
                                String username = in.readLine();
                                if (!username.isBlank() && username.length() > 8) {
                                    out.println("Choose your password:");
                                    String password = in.readLine();
                                    if (!password.isBlank() && password.length() > 8) {
                                        done = true;
                                    }
                                }
                            }
                            break;
                        case "2":
                            done = false;
                            while (!done) {
                                out.println("LOGIN");
                                out.println("Choose your username:");
                                String username = in.readLine();
                                if (users.stream().anyMatch(user -> user.getUsername().equals(username))) {
                                    out.println("Choose your password:");
                                    String password = in.readLine();
                                    if (users.stream().filter(user -> user.getUsername().equals(username)).findFirst().get().getPassword().equals(password)) {
                                        out.println("Logged as " + username);
                                        user.setLogged(true);
                                        user.setUsername(username);
                                        done = true;
                                    } else {
                                        out.println("Wrong password");
                                    }
                                } else {
                                    out.println("User not registered");
                                }
                            }
                            break;
                    }
                } while (!Objects.equals(choice, "/exit"));


            } catch (IOException e) {
                //TODO: handle
                e.printStackTrace();
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
}
