package Client;

import java.rmi.Remote;
import java.rmi.RemoteException;

//an generic interface consisting of all the methods that a server can perform.
public interface LibraryUserInterface extends Remote {

    //User role
    //This method will allow a user to borrow an item if its available.
    public String borrowItem(String userID, String itemID, int numberOfDays) throws RemoteException;

    //This method will let the user find the item from all the libraries and return the count.
    public String findItem(String userID, String itemName) throws RemoteException;

    //This method will help user to return the item.
    public String returnItem(String userID, String itemID) throws RemoteException;

    //This method will add user in a queue.
    public String addToQueue(String userID, String itemID,Integer numberOfDays) throws RemoteException;

    //This method will validate userID
    public boolean validateClientID(String userID) throws RemoteException;

    //This method will help user to borrow item from another library
    public String borrowFromOtherLibrary(String userID, String itemID, Integer numberOfDays);
}
