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
    private boolean done;
    private static final String SERVER_ADDRESS = ClientConfig.getServerAddress();
    private static final int SERVER_PORT = ClientConfig.getServerPort();//TODO: PARAMETERS (ports, address, schedule time)
    private static final String MULTICAST_ADDRESS = ClientConfig.getMulticastAddress();
    private static final int MULTICAST_PORT = ClientConfig.getMulticastPort();

    public static void main(String[] args) {
        ClientMain client = new ClientMain();
        client.run();
    }

    @Override
    public void run() {
        try {
            Socket client = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            //Inizializzazione thread InputHandler
            Thread t = new Thread(new InputHandler());
            t.start();

            //Inizializzazione thread Notifiche UDP
            Thread udpThread = new Thread(new UDPHandler());
            udpThread.start();

            String inMessage;
            while ((inMessage = in.readLine()) != null) {
                System.out.println(inMessage);
            }
        } catch (IOException e) {
            //TODO: handle
            e.printStackTrace();
        }
    }

    public void shutdown() {
        done = true;
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

    public class InputHandler implements Runnable {
        @Override
        public void run() {
            try {
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                while (!done) {
                    String message = inReader.readLine();
                    out.println(message);
                }
            } catch (IOException e) {
                //TODO: handle
                e.printStackTrace();
            }
        }
    }

    private class UDPHandler implements Runnable {
        @Override
        public void run() {
            try {
                InetAddress multicastGroup = InetAddress.getByName(MULTICAST_ADDRESS);
                MulticastSocket socket = new MulticastSocket(MULTICAST_PORT);
                socket.joinGroup(multicastGroup);

                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Notification: " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
