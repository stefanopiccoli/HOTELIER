package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientConfig {
    private static final String CONFIG_FILE = "client.properties";

    private static String serverAddress;
    private static int serverPort;
    private static String multicastAddress;
    private static int multicastPort;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            Properties prop = new Properties();
            prop.load(input);
            serverAddress = prop.getProperty("server.address", "127.0.0.1");
            serverPort = Integer.parseInt(prop.getProperty("server.port", "9999"));
            multicastAddress = prop.getProperty("multicast.address", "230.0.0.0");
            multicastPort = Integer.parseInt(prop.getProperty("multicast.port", "8888"));
            System.out.println("Configurazioni caricate:");
            System.out.println("Server address:"+serverAddress);
            System.out.println("Server port:"+serverPort);
            System.out.println("Multicast address:"+multicastAddress);
            System.out.println("Multicast port:"+multicastPort);
            System.out.println("\n");
        } catch (IOException e) {
            // Gestione dell'errore nel caso in cui il file di configurazione non sia presente
            System.out.println("Impossibile caricare il file di configurazione. Utilizzo dei valori di default.");
            serverAddress = "127.0.0.1";
            serverPort = 9999;
            multicastAddress = "230.0.0.0";
            multicastPort = 8888;
        }
    }

    // Metodi di accesso ai valori di configurazione
    public static String getServerAddress() {
        return serverAddress;
    }

    public static int getServerPort() {
        return serverPort;
    }

    public static String getMulticastAddress() {
        return multicastAddress;
    }

    public static int getMulticastPort() {
        return multicastPort;
    }

}