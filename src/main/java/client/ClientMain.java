package client;


import utils.ClientConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;


public class ClientMain implements Runnable {

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done = false;
    private static final String SERVER_ADDRESS = ClientConfig.getServerAddress();
    private static final int SERVER_PORT = ClientConfig.getServerPort();
    private static final String MULTICAST_ADDRESS = ClientConfig.getMulticastAddress();
    private static final int MULTICAST_PORT = ClientConfig.getMulticastPort();

    public static void main(String[] args) {
        ClientMain client = new ClientMain();
        client.run();
    }

    //run() inizializza due thread: uno si occuperà di acquisire e inviare al server gli input e l'altro di ricevere e l'altro di ricevere le notifiche dal gruppo multicast
    //Inoltre si occupa di rimanere in ascolto sullo stream del socket e stampare i messaggi in arrivo dal server
    @Override
    public void run() {
        try {
            client = new Socket(SERVER_ADDRESS, SERVER_PORT); //Inizializzazione Socket
            out = new PrintWriter(client.getOutputStream(), true); //PrintWriter utilizzato per inviare i messaggi al server
            in = new BufferedReader(new InputStreamReader(client.getInputStream())); //Buffer utilizzato per ricevere i messaggi dal server

            //Inizializzazione thread InputHandler
            Thread t = new Thread(new InputHandler());
            t.start();

            //Inizializzazione thread Notifiche UDP
            Thread udpThread = new Thread(new UDPHandler());
            udpThread.start();

            String inMessage;
            while (!done && (inMessage = in.readLine()) != null) { //Acquisizione continua dei messaggi in arrivo dal server e stampa sul client
                System.out.println(inMessage);
            }
            //Chiusura
            out.close();
            in.close();
            if (!client.isClosed()) {
                client.close();//Chiusura socket
            }
        } catch (IOException e) {
            System.err.println("Error in received message: " + e.getMessage());
        }
    }

    //InputHandler si occupa di acquisire i messaggi dell'utente dal terminale e di inviarli al server
    public class InputHandler implements Runnable {
        @Override
        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in)); //Inizializzazione buffer per acquisire gli input dell'utente
                while (!done) {
                    String message = input.readLine(); //Acquisizione dell'input
                    out.println(message); //Invio al server
                    //Inizializza la chiusura
                    if (message.equals("/exit")){ //Comando di uscita
                        shutdown(); //Procedura di spegnimento
                        input.close(); //Chiusura input
                    }
                }
            } catch (IOException e) {
                System.err.println("Error while sending message to server: " + e.getMessage());
            }
        }
    }

    //UDPHandler si occupa di unirsi al gruppo multicast, ricevere i datagrammi e stamparli sul client
    private class UDPHandler implements Runnable {
        @Override
        public void run() {
            try {
                InetAddress multicastGroup = InetAddress.getByName(MULTICAST_ADDRESS);
                MulticastSocket socket = new MulticastSocket(MULTICAST_PORT); //Inizializzazione Socket Multicast
                socket.joinGroup(multicastGroup); //Unione al gruppo

                while (!done) { //Acquisizione continua dei datagrammi e composizione della stringa
                    byte[] buffer = new byte[1024];
                    DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
                    socket.receive(datagram);

                    String message = new String(datagram.getData(), 0, datagram.getLength());
                    System.out.println("Notification: " + message);
                }
                //Chiusura multicast
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error in notification system: " + e.getMessage());
            }
        }
    }

    //Chiusura del client
    public void shutdown() {
        done = true;
        System.out.println("Disconnected, now you can close the client!");
        System.out.close();
    }
}
