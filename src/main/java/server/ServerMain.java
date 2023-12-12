package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import server.entities.Hotel;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain implements Runnable {
    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private ExecutorService pool;
    private Hotel[] hotels;


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
            GsonBuilder builder = new GsonBuilder();
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader("Hotels.json"));
            hotels = gson.fromJson(reader, Hotel[].class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public class ConnectionHandler implements Runnable {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String choice;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                do {
                    out = new PrintWriter(client.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    out.println("Connected to Hotelier!");
                    out.println("1. Register");
                    out.println("2. Login");
                    out.println("0. Esci");
                    out.print("Scelta: ");
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
