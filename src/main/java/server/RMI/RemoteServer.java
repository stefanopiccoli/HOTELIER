package server.RMI;

import RMI.RemoteInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteServer extends UnicastRemoteObject implements RemoteInterface {
    // Costruttore che lancia l'eccezione RemoteException
    public RemoteServer() throws RemoteException {
        super();
    }

    @Override
    public boolean register(String username, String password) throws RemoteException {
        if (username.length() < 8 || password.length() < 8 || !password.contains("!"))
            return false;
        else
            return true;
    }

}
