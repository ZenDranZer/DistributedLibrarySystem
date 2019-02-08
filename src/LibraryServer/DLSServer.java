package LibraryServer;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class DLSServer extends UnicastRemoteObject implements LibraryUserInterface,LibraryManagerInterface {

    protected final HashMap<String,User> user;
    protected final HashMap<String,Manager> manager;
    protected final HashMap<String,Item> item;
    protected final HashMap<User,HashMap<Item,Integer>> borrow;
    protected final HashMap<Item, HashMap<User,Integer>> waitingQueue;
    private String library;
    private Integer next_User_ID;
    private Integer next_Manager_ID;
    private Integer next_Item_ID;
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
        next_User_ID = 1003;
        next_Manager_ID = 1002;
        next_Item_ID = 1004;
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
        init();
    }

    private void init(){
        String initManagerID = library + "M" + 1001;
        Manager initManager = new Manager(initManagerID);
        manager.put(initManagerID,initManager);
        writeToLogFile("Initial manager created.");
        String initUserID1001 = library + "U" + 1001;
        String initUserID1002 = library + "U" + 1002;
        User initUser1001 = new User(initUserID1001);
        User initUser1002 = new User(initUserID1002);
        user.put(initUserID1001,initUser1001);
        user.put(initUserID1002,initUser1002);
        writeToLogFile("Initial users created.");
        String initItemID1001 = library + 1001;
        String initItemID1002 = library + 1002;
        String initItemID1003 = library + 1003;
        Item initItem1001 = new Item(initItemID1001,"Distributed Systems",5);
        Item initItem1002 = new Item(initItemID1002,"Parallel Programming",6);
        Item initItem1003 = new Item(initItemID1003,"Algorithm Designs",7);
        item.put(initItemID1001,initItem1001);
        item.put(initItemID1002,initItem1002);
        item.put(initItemID1003,initItem1003);
        writeToLogFile("Initial items created.");
    }

    @Override
    public String borrowItem(String userID, String itemID, int numberOfDays) throws RemoteException {
        User currentUser = user.get(userID);
        String reply =  "Borrow Request : Server : " + library +
                " User : " + userID +
                " Item :" + itemID +
                "Status : ";
        if(!item.containsKey(itemID)){
            reply += "outsourced.";
            writeToLogFile(reply);
            return "outsourced";
        }

        Item requestedItem;
        synchronized (this){
        requestedItem = item.get(itemID);
        }
        if(requestedItem.getItemCount() == 0){
            reply += "outsourced";
            writeToLogFile(reply);
            return "outsourced";
        }else {
            HashMap<Item,Integer> entry;
            requestedItem.setItemCount(requestedItem.getItemCount() - 1);
            item.remove(itemID);
            item.put(itemID, requestedItem);
            if (borrow.containsKey(currentUser)) {
                if (borrow.get(currentUser).containsKey(requestedItem)) {
                    reply += "Unsuccessful. \n Note : User have already borrowed.";
                    return reply;
                } else {
                    entry = borrow.get(currentUser);
                    borrow.remove(currentUser);
                }
            } else {
                entry = new HashMap<>();
            }
            entry.put(requestedItem, numberOfDays);
            borrow.put(currentUser, entry);
            reply += "Successful.";
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
            currentUser = user.get(userID);
        else{
            message += "Unsuccessful. " +
                            "\nNote : You are not allowed to use this feature.";
            writeToLogFile(message);
            return message;
        }

        if(!itemID.substring(0,3).equals("CON")){
            returnToOtherLibrary(userID,itemID);
        }
        if(borrow.containsKey(currentUser))
            System.out.println("key");
        else
            System.out.println("no key");

        HashMap<Item,Integer> set = borrow.get(currentUser);
        Iterator<Map.Entry<Item,Integer>> value = set.entrySet().iterator();
        if(!value.hasNext()){
            message +="Unsuccessful. " +
                    "\nNote : You have not borrowed the item.";
            writeToLogFile(message);
            return message;
        }
        while(value.hasNext()) {
            Map.Entry<Item, Integer> pair = value.next();
            if(pair.getKey().getItemID().equals(itemID)){
                borrow.get(currentUser).remove(pair.getKey());
                updateItemCount(itemID);
                message += "Successful ";
                writeToLogFile(message);
                //add functionality for automatic assignment of book which is being returned.
                return message;
            }
        }
        message += "Unsuccessful " +
                    "\nNote: Item have never been borrowed";
        writeToLogFile(message);
           return message;

    }

    @Override
    public String addItem(String managerID, String itemName, int quantity) throws RemoteException {
        String message =
                "Add Item Request : Server : " + library +
                        " Manager : " + managerID ;
        if(managerID.charAt(3) != 'M'){
            message += "Unsuccessful. " +
                    "\nNote : You are not allowed to use this feature.";
            writeToLogFile(message);
            return message;
        }
        String itemID = library + next_Item_ID;
        Item currentItem = new Item(itemID,itemName,quantity);
        item.put(itemID,currentItem);
        next_Item_ID += 1;
        message +=  " Item :" + itemID +
                    "Status : Successful.";
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
        Item currentItem = item.get(itemID);
        User currentUser = user.get(userID);
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

    protected void updateItemCount(String itemID){
        Item currentItem = item.get(itemID);
        currentItem.setItemCount(currentItem.getItemCount()+1);
        item.remove(itemID);
        item.put(itemID,currentItem);
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
