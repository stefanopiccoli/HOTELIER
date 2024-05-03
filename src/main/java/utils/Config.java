package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final String CONFIG_FILE = "config.properties";

    private static String serverAddress;
    private static int tcpPort;
    private static String multicastGroup;
    private static int udpPort;
    private static int udpNotifierPeriod;
    private static int udpNotifierInitialDelay;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            Properties prop = new Properties();
            prop.load(input);
            serverAddress = prop.getProperty("server.address", "127.0.0.1");
            tcpPort = Integer.parseInt(prop.getProperty("tcp.port", "9999"));
            multicastGroup = prop.getProperty("multicast.group", "230.0.0.0");
            udpPort = Integer.parseInt(prop.getProperty("udp.port", "8888"));
            udpNotifierPeriod = Integer.parseInt(prop.getProperty("udp.notifier.period", "60"));
            udpNotifierInitialDelay = Integer.parseInt(prop.getProperty("udp.notifier.initial.delay", "60"));
        } catch (IOException e) {
            // Gestione dell'errore nel caso in cui il file di configurazione non sia presente
            e.printStackTrace();
            System.err.println("Impossibile caricare il file di configurazione. Utilizzo dei valori di default.");
            serverAddress = "127.0.0.1";
            tcpPort = 9999;
            multicastGroup = "230.0.0.0";
            udpPort = 8888;
            udpNotifierPeriod = 60;
            udpNotifierInitialDelay = 60;
        }
    }

    // Metodi di accesso ai valori di configurazione
    public static String getServerAddress() {
        return serverAddress;
    }

    public static int getTcpPort() {
        return tcpPort;
    }

    public static String getMulticastGroup() {
        return multicastGroup;
    }

    public static int getUdpPort() {
        return udpPort;
    }

    public static int getUdpNotifierPeriod() {
        return udpNotifierPeriod;
    }

    public static int getUdpNotifierInitialDelay() {
        return udpNotifierInitialDelay;
    }
}