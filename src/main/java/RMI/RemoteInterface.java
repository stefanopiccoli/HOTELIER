package RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    boolean register(String username, String password) throws RemoteException;
}
