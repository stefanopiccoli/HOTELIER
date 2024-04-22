package client;


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
    private static final int TCP_PORT = 9999;
    private static final int UDP_PORT = 8888;
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final String MULTICAST_GROUP = "230.0.0.0";

    public static void main(String[] args) {
        ClientMain client = new ClientMain();
        client.run();
    }
    @Override
    public void run() {
        try {
            Socket client = new Socket(SERVER_ADDRESS,TCP_PORT);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            //Inizializzazione thread InputHandler
            Thread t = new Thread(new InputHandler());
            t.start();

            //Inizializzazione thread Notifiche UDP
            Thread udpThread = new Thread(new UDPHandler());
            udpThread.start();

            String inMessage;
            while((inMessage=in.readLine())!=null){
                System.out.println(inMessage);
            }
        } catch (IOException e) {
            //TODO: handle
            e.printStackTrace();
        }
    }
    public void shutdown(){
        done=true;
        try {
            in.close();
            out.close();
            if(!client.isClosed()){
                client.close();
            }
        }catch (IOException e){
            //TODO: handle
            e.printStackTrace();
        }
    }
    public class InputHandler implements Runnable{
        @Override
        public void run(){
            try {
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                while (!done){
                    String message = inReader.readLine();
                    if (message.equals("/exit")){
                        inReader.close();
                        shutdown();
                    }else {
                        out.println(message);
                    }

                }
            }catch (IOException e){
                //TODO: handle
                e.printStackTrace();
            }
        }
    }
    private class UDPHandler implements Runnable {
        @Override
        public void run() {
            try {
                InetAddress multicastGroup = InetAddress.getByName(MULTICAST_GROUP);
                MulticastSocket socket = new MulticastSocket(UDP_PORT);
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
