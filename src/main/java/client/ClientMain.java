package client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class ClientMain implements Runnable {

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;

    public static void main(String[] args) {
        ClientMain client = new ClientMain();
        client.run();
    }
    @Override
    public void run() {
        try {
            Socket client = new Socket("127.0.0.1", 9999);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            InputHandler inHandler = new InputHandler();
            Thread t = new Thread(inHandler);
            t.start();

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
                    if (message.equals("0")){
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
}
