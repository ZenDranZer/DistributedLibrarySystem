package LibraryServer;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class DLSServer extends UnicastRemoteObject implements LibraryUserInterface,LibraryManagerInterface {

    protected HashMap<String,User> user;
    protected HashMap<String,Manager> manager;
    protected HashMap<String,Item> item;
    protected HashMap<User,HashMap<Item,Integer>> borrow;
    protected HashMap<Item, HashMap<User,Integer>> waitingQueue;
    private String library;
    private Integer next_User_ID;
    private Integer next_Manager_ID;
    private File logFile;
    private PrintWriter logger;

    public String getLibrary() {
        return library;
    }

    public DLSServer(String library) throws RemoteException{
        super();
        this.library = library;
        user = new HashMap<>();
        manager = new HashMap<>();
        item = new HashMap<>();
        borrow = new HashMap<>();
        waitingQueue = new HashMap<>();
        next_User_ID = 1005;
        next_Manager_ID = 1002;
        logFile = new File("log_" + library + ".log");
        try{
            if(!logFile.exists())
                logFile.createNewFile();
            logger = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
        }catch (IOException io){
            System.out.println("Error in creating log file.");
            io.printStackTrace();
        }
        writeToLogFile("Server " + library + " Started.");

    }

    @Override
    public String borrowItem(String userID, String itemID, int numberOfDays) throws RemoteException {
        User currentUser;
        if(userID.charAt(3) == 'U')
            currentUser = new User(userID);
        else{
            String message = "Borrow Request : Server : " + library +
                    " User : " + userID +
                    " Item :" + itemID +
                    "Status : Unsuccessful. " +
                    "\nNote :You are not allowed to use this feature.";
            writeToLogFile(message);
            return message;
        }
        Item requestedItem;
        synchronized (this){
        requestedItem = item.get(itemID);
        }
        String reply =  "Borrow Request : Server : " + library +
                        " User : " + userID +
                        " Item :" + requestedItem.getItemName() +
                        "Status : ";
        if(requestedItem == null){
            reply += "outsourced.";
            writeToLogFile(reply);
            return "outsourced";
        }else if(requestedItem.getItemCount() == 0){
            reply += "outsourced";
            writeToLogFile(reply);
            return "outsourced";
        }else{
            requestedItem.setItemCount(requestedItem.getItemCount()-1);
            item.remove(itemID);
            item.put(itemID,requestedItem);
            if(borrow.get(currentUser).isEmpty()){
            borrow.put(currentUser,new HashMap<>());
            borrow.get(currentUser).put(requestedItem,numberOfDays);
            }else {
                final Integer integer = borrow.get(currentUser).putIfAbsent(requestedItem, numberOfDays);
                if(integer != null)
                    reply += "Unsuccessful. \n Note : you already have borrowed.";
                else
                    reply += "Successful.";
            }
            writeToLogFile(reply);
            return reply;
            }
    }

    @Override
    public String findItem(String userID, String itemName) throws RemoteException {
        if(userID.charAt(3) != 'U') {
            String message =
                    "Find Request : Server : " + library +
                    " User : " + userID +
                    " Item :" + itemName +
                    " Status : Unsuccessful " +
                    "\nNote : You are not allowed to use this feature.";
            writeToLogFile(message);
            return message;
        }

        String reply;
        reply = findAtOtherLibrary(itemName);
        //inter server communication
        Iterator<Map.Entry<String,Item>> iterator = item.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String,Item> pair = iterator.next();
            if(pair.getValue().getItemName().equals(itemName))
                reply = reply + "\n" + pair.getKey() + " " +pair.getValue().getItemCount();
        }
        reply = reply + "\nFind Request : Server : " + library +
                        " User : " + userID +
                        " Item :" + itemName +
                        " Status : Successful ";
        writeToLogFile(reply);
        return reply;
    }

    @Override
    public String returnItem(String userID, String itemID) throws RemoteException {
        User currentUser;
        String message =
                        "Return Request : Server : " + library +
                        " User : " + userID +
                        " Item :" + itemID +
                        "Status : ";
        if(userID.charAt(3) == 'U')
            currentUser = new User(userID);
        else{
            message += "Unsuccessful. " +
                            "\nNote : You are not allowed to use this feature.";
            writeToLogFile(message);
            return message;
        }

        if(!itemID.substring(0,3).equals("CON")){
            returnToOtherLibrary(userID,itemID);
        }

        Iterator<Map.Entry<Item,Integer>> value = borrow.get(currentUser).entrySet().iterator();
        if(!value.hasNext()){
            message +="Unsuccessful. " +
                    "\nNote : You have not borrowed the item.";
            writeToLogFile(message);
            return message;
        }
        boolean status = false;
        while(value.hasNext()) {
            Map.Entry<Item, Integer> pair = value.next();
            if(pair.getKey().getItemID().equals(itemID)){
                borrow.get(currentUser).remove(pair.getKey());
                updateItemCount(itemID);
                status = true;
                break;
            }
        }
        if(status){
            message += "Successful ";
            writeToLogFile(message);
            //add functionality for automatic assignment of book which is being returned.
            return message;
        }else{
            message += "Unsuccessful " +
                    "\nNote: Item have never been borrowed";
            writeToLogFile(message);
            return message;
        }
    }

    @Override
    public String addItem(String managerID, String itemID, String itemName, int quantity) throws RemoteException {
        String message =
                "Add Item Request : Server : " + library +
                        " Manager : " + managerID +
                        " Item :" + itemID +
                        "Status : ";
        if(managerID.charAt(3) != 'M'){
            message += "Unsuccessful. " +
                    "\nNote : You are not allowed to use this feature.";
            writeToLogFile(message);
            return message;
        }
        Item currentItem = item.get(itemID);
        if(currentItem == null){
            item.put(itemID,new Item(itemID,itemName,quantity));
        }else{
            item.get(itemID).setItemCount(item.get(itemID).getItemCount()+quantity);
        }
        message += " Successful.";
        writeToLogFile(message);
        return message;
    }

    @Override
    public String removeItem(String managerID, String itemID, int quantity) throws RemoteException {
        String message =
                        "Remove Request : Server : " + library +
                        " Manager : " + managerID +
                        " Item :" + itemID +
                        "Status : ";

        if(managerID.charAt(3) != 'M') {
            message += "Unsuccessful. " +
                    "\nNote : You are not allowed to use this feature.";
            writeToLogFile(message);
            return message;
        }
        Item currentItem = item.get(itemID);
        if(currentItem == null){
            message += "Unsuccessful. " +
                    "\nNote : The item do not exist in inventory.";
            writeToLogFile(message);
            return message;
        }
        if(currentItem.getItemCount() < quantity){
            quantity = quantity - currentItem.getItemCount();
            item.remove(itemID);
            message+= "Partially successful." +
                    "\nNote : Number of items in the inventory is less than desired quantity." +
                    "\n Balance quantity :" + quantity;
            writeToLogFile(message);
            return message;
        }else if(currentItem.getItemCount() > quantity){
            currentItem.setItemCount(currentItem.getItemCount() - quantity);
            item.remove(itemID);
            item.put(itemID,currentItem);
            message += "Successful." +
                    "\nNote : Number of items in the inventory is greater than desired quantity.";
            writeToLogFile(message);
            return message;
        }else{
            item.remove(itemID);
            message += "Successful.";
            writeToLogFile(message);
            return message;
        }
    }

    @Override
    public String listItemAvailability(String managerID) throws RemoteException {
        if(managerID.charAt(3) != 'M') {
            String message =
                            "Item availability Request : Server : " + library +
                            " Manager : " + managerID +
                            "Status : Unsuccessful. " +
                            "\nNote : You are not allowed to use this feature.";
            writeToLogFile(message);
            return message;
        }
        String reply =  "Item availability Request : Server : " + library +
                        " Manager : " + managerID +
                        "Status : Successful. " +
                        "\nAvailability:\n";
        Iterator<Map.Entry<String,Item>> iterator = item.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String,Item> pair = iterator.next();
            reply += pair.getValue().getItemName() + " " + pair.getValue().getItemCount() + "\n";
        }
        writeToLogFile(reply);
        return reply;
    }

    @Override
    public String createUser(String managerID) throws RemoteException {
        if(managerID.charAt(3) != 'M'){
            String message =
                            "New User Request : Server : " + library +
                            " Manager : " + managerID +
                            "Status : Unsuccessful. " +
                            "\nNote : You are not allowed to use this feature.";
            writeToLogFile(message);
            return  message;
        }
        String userID = library + "U" + next_User_ID ;
        User currentUser = new User(userID);
        user.put(userID,currentUser);
        next_User_ID += 1;
        String message =
                        "New User Request : Server : " + library +
                        " Manager : " + managerID +
                        " New User ID : "+ userID +
                        " Status : Successful.";
        writeToLogFile(message);
        return  message;
    }

    @Override
    public String createManager(String managerID) throws RemoteException{
        if(managerID.charAt(3) != 'M'){
            String message =
                            "New User Request : Server : " + library +
                            " Manager : " + managerID +
                            "Status : Unsuccessful. " +
                            "\nNote : You are not allowed to use this feature.";
            writeToLogFile(message);
            return  message;
        }
        String newManagerID = library + "M" + next_Manager_ID ;
        Manager currentManager = new Manager(newManagerID);
        manager.put(newManagerID,currentManager);
        next_Manager_ID += 1;
        String message =
                        "New User Request : Server : " + library +
                        " Manager : " + managerID +
                        " New Manager ID : "+ newManagerID +
                        " Status : Successful.";
        writeToLogFile(message);
        return  message;
    }

    @Override
    public boolean validateClientID(String id){
        User currentUser;
        Manager currentManager;
        if(id == null){
            writeToLogFile("Validate ID request : ID : "+ id);
            return false;
        }
        else if(id.charAt(3) == 'U'){
            synchronized(this){
            currentUser = user.get(id);
            }
            writeToLogFile("Validate ID request : ID : "+ id);
            return currentUser != null;
        }
        else if(id.charAt(3) == 'M'){
            synchronized (this){
            currentManager = manager.get(id);
            }
            writeToLogFile("Validate ID request : ID : "+ id);
            return currentManager != null;
        }
        writeToLogFile("Validate ID request : ID : "+ id);
        return false;
    }

    @Override
    public String addToQueue(String userID, String itemID,Integer numberOfDays) throws RemoteException {
        Item currentItem = new Item(itemID);
        User currentUser = new User(userID);
        HashMap<User,Integer> waitingUsers = waitingQueue.get(currentItem);
            waitingUsers.put(currentUser,numberOfDays);
            waitingUsers.remove(currentItem);
            waitingQueue.put(currentItem,waitingUsers);
            String message =
                            "Add to Queue Request : Server : " + library +
                            " User ID :" + userID +
                            " Item ID : "+ itemID +
                            " Status : Successful.";
            writeToLogFile(message);
            return  message;
    }

    @Override
    public String borrowFromOtherLibrary(String userID, String itemID, Integer numberOfDays){
        User currentUser = user.get(userID);

        //Correct this function
        if(currentUser.isOutsourced()){
            String message =
                            "Outsourcing borrow Request : Server : " + library +
                            " User : " + userID +
                            " Item :" + itemID +
                            "Status : Unsuccessful" +
                            "\n Note : You can only borrow one book from other than your library";
            writeToLogFile(message);
            return message;
        }
        //Inter Server Communication
        return null;
    }


    private String findAtOtherLibrary(String itemName){

        return null;
    }

    private void returnToOtherLibrary(String userID, String itemID){


    }
    private void updateItemCount(String itemid){

    }

    synchronized private void writeToLogFile(String message) {
        try {
            if (logger == null)
                return;
            // print the time and the message to log file
            logger.println(Calendar.getInstance().getTime().toString() + " - " + message);
            logger.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
