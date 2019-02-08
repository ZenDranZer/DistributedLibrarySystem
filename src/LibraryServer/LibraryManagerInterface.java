package LibraryServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

//an interface providing roles of manager.
public interface LibraryManagerInterface extends Remote {

    //Manager role
    //This method will help manager to add an item into the library.
    public String addItem(String managerID, String itemName, int quantity) throws RemoteException;

    //This method will let the manager change the quantity of the item or remove it
    public String removeItem(String managerID, String itemID, int quantity) throws RemoteException;

    //This method will allow manager to list all the items with their availability.
    public String listItemAvailability(String managerID) throws RemoteException;

    //This method will allow manager to add a user.
    public String createUser(String managerID) throws RemoteException;

    //This method will allow manager to add a manager.
    public String createManager(String managerID) throws RemoteException;

    //This method will validate userID
    public boolean validateClientID(String userID) throws RemoteException;

}
