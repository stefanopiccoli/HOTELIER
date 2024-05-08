package server;

import server.entities.Hotel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;
import static server.ServerMain.Server;

//UDPNotifier si occupa di scansionare, a intervallo fisso di tempo, le valutazioni degli hotel, aggiornarle e notificare a tutti i client connessi l'eventuale aggiornamento della prima posizione della classifica.
class UDPNotifier implements Runnable {
    @Override
    public void run() {
        InetAddress multicastGroup = null;
        MulticastSocket socket = null;
        try {
            multicastGroup = InetAddress.getByName(Server().MULTICAST_ADDRESS);
            socket = new MulticastSocket();
        } catch (IOException e) {
            System.err.println("Error while initializing multicast group: "+e.getMessage());
        }
        while (true) {
            try {
                sleep(TimeUnit.SECONDS.toMillis(Server().UDPNOTIFIER_PERIOD)); //Intervallo di tempo
            } catch (InterruptedException e) {
                System.err.println("Notification system experienced an unexpected shutdown!");
            }

            Map<String, ArrayList<Hotel>> oldLocalRankings = new HashMap<>(Server().localRankings); //Copio il ranking attuale
            Server().calculateLocalRankings(); //Aggiorno il ranking del server
            Map<String, Hotel> changedRankings = new HashMap<>(); //Inizializzo la variabile che conterrà i cambiamenti delle prime posizioni

            //Inserisco in changedRankings tutti gli hotel che sono passati in prima posizione
            for (String city : oldLocalRankings.keySet()) { //Ciclo sul vecchio ranking
                if (!oldLocalRankings.get(city).isEmpty() && !Server().localRankings.get(city).isEmpty()) {
                    Hotel firstOld = oldLocalRankings.get(city).get(0);//TODO: check not null
                    Hotel firstNew = Server().localRankings.get(city).get(0);//TODO: check not null
                    if (!firstOld.getName().equals(firstNew.getName())) { //Verifico se la prima posizione è cambiata
                        changedRankings.putIfAbsent(city, firstNew); //Inserisco nei cambiamenti il nuovo primo hotel
                    }
                }
            }

            //TODO: comment

            //Creazione del messaggio
            try {
                String message;
                if (changedRankings.isEmpty()) { //Se non ci sono cambiamenti notifica esclusivamente che la procedura è stata effettuata
                    message = "Rankings updated! Nothing changed.\n";
                } else { //Se ci sono cambiamenti mostra tutti i nuovi hotel in prima posizione
                    message = "New hotels rank available: \n";
                    for (Hotel h : changedRankings.values()) {
                        message = message.concat(h.getName() + " is now first!\n");
                    }
                }

                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, multicastGroup, Server().MULTICAST_PORT);
                socket.send(packet);
            } catch (IOException e) {
                System.err.println("Error while sending the notification message!");
            }

            Server().loadUserBadges(); //Aggiornamento dei badge
        }
    }
}
