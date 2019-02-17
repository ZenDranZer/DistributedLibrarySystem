package LibraryServer;

import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**Main server class which implements all the methods of both the interfaces
 * LibraryUserInterface and LibraryManagerInterface and extends UnicastRemoteObject.*/
public class DLSServer extends UnicastRemoteObject implements LibraryUserInterface,LibraryManagerInterface {

    protected final HashMap<String,User> user;
    protected final HashMap<String,Manager> manager;
    protected final HashMap<String,Item> item;
    protected final HashMap<User,HashMap<Item,Integer>> borrow;
    protected final HashMap<Item, HashMap<User,Integer>> waitingQueue;
    private final HashMap<User,ArrayList<String>> messagesForUser;
    private String library;
    private Integer next_User_ID;
    private Integer next_Manager_ID;
    private File logFile;
    private PrintWriter logger;
    private final Object lock;

    public String getLibrary() {return library;}

    public DLSServer(String library) throws RemoteException{
        super();
        this.library = library;
        user = new HashMap<>();
        manager = new HashMap<>();
        item = new HashMap<>();
        borrow = new HashMap<>();
        waitingQueue = new HashMap<>();
        messagesForUser = new HashMap<>();
        next_User_ID = 1003;
        next_Manager_ID = 1002;
        lock = new Object();
        logFile = new File("C:\\Users\\SARVESH\\Documents\\DistributedLibrarySystem\\src\\Logs\\log_" + library + ".log");
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

    /**Initialize the data structure with some values.*/
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

    /**used to borrow an item by the user. If Item does not exist in the library it
     * will send message to user for asking to borrow from other library and if none
     * of the library has the book it will simply ask for adding the user into waiting
     * queue, if user says yes it will add user into queue. If item borrowed successfully
     * both server and client will keep the log.*/
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
        requestedItem = item.get(itemID);
        if(requestedItem.getItemCount() == 0){
            reply += "outsourced";
            writeToLogFile(reply);
            return "outsourced";
        }else {
            HashMap<Item,Integer> entry;
            requestedItem.setItemCount(requestedItem.getItemCount() - 1);
            synchronized (lock) {
                item.remove(itemID);
                item.put(itemID, requestedItem);
            }
            if (borrow.containsKey(currentUser)) {
                if (borrow.get(currentUser).containsKey(requestedItem)) {
                    reply += "Unsuccessful. \n Note : User have already borrowed.";
                    return reply;
                } else {
                    entry = borrow.get(currentUser);
                    synchronized (lock){
                        borrow.remove(currentUser);
                    }
                }
            } else {
                entry = new HashMap<>();
            }
            entry.put(requestedItem, numberOfDays);
            synchronized (lock) {
                borrow.put(currentUser, entry);
                reply += "Successful.";
            }
            writeToLogFile(reply);
            return reply;
        }
    }

    /**used to find the given item name in all library and return the itemID and availability.*/
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

        String reply = "";
        Iterator<Map.Entry<String, Item>> iterator;
        synchronized (lock) {
            iterator = item.entrySet().iterator();
        }
        while(iterator.hasNext()){
            Map.Entry<String,Item> pair = iterator.next();
            if(pair.getValue().getItemName().equals(itemName))
                reply = reply + "\n" + pair.getKey() + " " +pair.getValue().getItemCount();
        }
        reply += findAtOtherLibrary(itemName);
        reply = reply + "\nFind Request : Server : " + library +
                        " User : " + userID +
                        " Item :" + itemName +
                        " Status : Successful ";
        writeToLogFile(reply);
        return reply;
    }

    /**used to return the item borrowed by the user. If user has not borrowed any
     * book return unsuccessful note. Returned book will be automatically given
     * to those who are in the waiting queue.*/
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

        if(!itemID.substring(0,3).equals(library)){
            message = returnToOtherLibrary(userID,itemID);
            return message;
        }
        if(borrow.containsKey(currentUser)){
        HashMap<Item,Integer> set = borrow.get(currentUser);
        Iterator<Map.Entry<Item,Integer>> value;
        synchronized (lock){ value = set.entrySet().iterator(); }
        if(!value.hasNext()){
            message +="Unsuccessful. " +
                    "\nNote : You have no borrowed item.";
            writeToLogFile(message);
            return message;
        }
        while(value.hasNext()) {
            Map.Entry<Item, Integer> pair = value.next();
            if(pair.getKey().getItemID().equals(itemID)){
                synchronized (lock){borrow.get(currentUser).remove(pair.getKey());}
                updateItemCount(itemID);
                automaticAssignmentOfBooks(itemID);
                message += "Successful ";
                writeToLogFile(message);
                return message;
            }
        }
        }
        message += "Unsuccessful " +
                    "\nNote: Item have never been borrowed";
        writeToLogFile(message);
           return message;

    }

    /**String addItem(String managerID, String itemID,String itemName, int quantity)*/
    @Override
    public String addItem(String managerID, String itemID,String itemName, int quantity) throws RemoteException {
        String message =
                "Add Item Request : Server : " + library +
                        " Manager : " + managerID ;
        if(managerID.charAt(3) != 'M'){
            message += "Unsuccessful. " +
                    "\nNote : You are not allowed to use this feature.";
            writeToLogFile(message);
            return message;
        }
        Item currentItem;
        if(authenticateItemID(itemID)){
            if(item.containsKey(itemID)){
                currentItem = item.get(itemID);
                currentItem.setItemCount(currentItem.getItemCount()+quantity);
                synchronized (lock){item.remove(itemID);}
            }else{
                currentItem = new Item(itemID,itemName,quantity);
            }
            synchronized (lock){item.put(itemID,currentItem);}
            message +=  " Item :" + itemID +
                    "Status : Successful.";
            automaticAssignmentOfBooks(itemID);
            writeToLogFile(message);
            return message;
        }
        message +=  " Item :" + itemID +
                "Status : Unsuccessful." +
                "\n Note : Invalid ItemID.";
        writeToLogFile(message);
        return message;
    }

    /**used by the manager to remove the item completely or decrease the quantity of it.*/
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
        if(!item.containsKey(itemID)){
            message += "Unsuccessful. " +
                    "\nNote : The item do not exist in inventory.";
            writeToLogFile(message);
            return message;
        }
        Item currentItem = item.get(itemID);

        if(quantity < 0){
            item.remove(itemID);
            waitingQueue.remove(currentItem);
            Iterator<Map.Entry<User,HashMap<Item,Integer>>> iterator = borrow.entrySet().iterator();
            while(iterator.hasNext()){
                Map.Entry<User,HashMap<Item,Integer>> pair = iterator.next();
                if(pair.getValue().containsKey(currentItem)){
                    borrow.get(pair.getKey()).remove(currentItem);
                    if(messagesForUser.containsKey(pair.getKey())){
                        messagesForUser.get(pair.getKey()).add("The item " + itemID + " is being removed from library" +
                                "and you no longer owe this item.");
                    }else{
                        ArrayList<String> messages = new ArrayList<>();
                        messages.add("The item " + itemID + " is being removed from library" +
                                "and you no longer owe this item.");
                        messagesForUser.put(pair.getKey(),messages);
                    }
                }
            }
            message+= " Successful." +
                    "\nNote : All items have been removed.";
            writeToLogFile(message);
            return message;
        }else if(currentItem.getItemCount() < quantity){
            quantity = quantity - currentItem.getItemCount();
            synchronized (lock){item.remove(itemID);
            currentItem.setItemCount(0);
            item.put(itemID,currentItem);}
            message+= " Partially successful." +
                      "\nNote : Number of items in the inventory is less than desired quantity." +
                      "\n Balance quantity :" + quantity;
            writeToLogFile(message);
            return message;
        }else if(currentItem.getItemCount() > quantity){
            currentItem.setItemCount(currentItem.getItemCount() - quantity);
            synchronized (lock){
                item.remove(itemID);
                item.put(itemID,currentItem);
            }
            message += "Successful.";
            writeToLogFile(message);
            return message;
        }else{
            synchronized (lock){item.remove(itemID);}
            message += "Successful.";
            writeToLogFile(message);
            return message;
        }
    }

    /**It shows all the available books in that library to the manager.*/
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
        Iterator<Map.Entry<String,Item>> iterator;
        synchronized (lock) { iterator = item.entrySet().iterator(); }
        while(iterator.hasNext()){
            Map.Entry<String,Item> pair = iterator.next();
            reply += "\n" + pair.getValue().getItemName() + " " + pair.getValue().getItemCount() + "\n";
        }
        writeToLogFile(reply);
        return reply;
    }

    /**allows the manager to create a user for that library. It returns new userID.*/
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
        synchronized (lock) {user.put(userID,currentUser);
        next_User_ID += 1;}
        String message =
                        "New User Request : Server : " + library +
                        " Manager : " + managerID +
                        " New User ID : "+ userID +
                        " Status : Successful.";
        writeToLogFile(message);
        return  message;
    }

    /**allows the manager to create a manager for that library. It returns new managerID.*/
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
        synchronized (lock) {manager.put(newManagerID,currentManager);
        next_Manager_ID += 1;}
        String message =
                        "New User Request : Server : " + library +
                        " Manager : " + managerID +
                        " New Manager ID : "+ newManagerID +
                        " Status : Successful.";
        writeToLogFile(message);
        return  message;
    }

    @Override
    public String validateClientID(String id){
        User currentUser;
        Manager currentManager;
        if(id == null){
            writeToLogFile("Validate ID request : ID : "+ id);
            return "false";
        }
        else if(id.charAt(3) == 'U'){
            synchronized(lock){ currentUser = user.get(id); }
            String message;
            if(currentUser == null){
                writeToLogFile("Validate ID request : ID : "+ id +" Status : Unsuccessful");
                return "false";
            }else{
                message = "true";
                if(messagesForUser.containsKey(currentUser)){
                    ArrayList<String> messages = messagesForUser.get(currentUser);
                    for (String message1 : messages) {
                        message += ":" + message1;
                    }
                }
                return message;
            }
        }
        else if(id.charAt(3) == 'M'){
            synchronized (this){
            currentManager = manager.get(id);
            }
            String message;
            if(currentManager == null){
                writeToLogFile("Validate ID request : ID : "+ id +" Status : Unsuccessful");
                return "false";
            }else{
                writeToLogFile("Validate ID request : ID : "+ id +" Status : Successful");
                return  "true";
            }
        }
        writeToLogFile("Validate ID request : ID : "+ id + " Status : Unsuccessful");
        return "false";
    }

    /**It adds the given userID to the waiting list for given itemID with numberOfDays.*/
    @Override
    public String addToQueue(String userID, String itemID,Integer numberOfDays) throws RemoteException {
        Item currentItem = item.get(itemID);
        User currentUser = user.get(userID);
        HashMap<User,Integer> waitingUsers = new HashMap<>();
        if(!waitingQueue.containsKey(currentItem)){
            synchronized (lock) {waitingQueue.put(currentItem,waitingUsers);}
        }
        synchronized (lock) {waitingUsers = waitingQueue.get(currentItem);
        waitingUsers.put(currentUser,numberOfDays);
        waitingUsers.remove(currentUser);
        waitingQueue.put(currentItem,waitingUsers);}
        String message =
                "Add to Queue Request : Server : " + library +
                        " User ID :" + userID +
                        " Item ID : "+ itemID +
                        " Status : Successful.";
        writeToLogFile(message);
        return  message;
    }

    /**It allows the user to borrow an item from other library. If not present
     * at all libraries then it will ask user whether to add the user into the queue or not.*/
    @Override
    public String borrowFromOtherLibrary(String userID, String itemID, Integer numberOfDays){
        User currentUser;
        synchronized (lock) { currentUser = user.get(userID); }
        String reply =  "Borrow Request : Server : " + library +
                        " User : " + userID +
                        " Item :" + itemID +
                        " Status : " ;
        try {
            DatagramSocket mySocket = new DatagramSocket();
            InetAddress host = InetAddress.getLocalHost();
            String result = "unsuccessful";
            String request = library+":borrowFromOther:"+userID+":"+itemID+":"+numberOfDays;
            boolean lib1 = false,lib2 = false;
            String library1 = "";
            String library2 = "";
            int index1 = -1, index2 = -1;
            int port1 = 1,port2 = 1;
            if (library.equals("CON")){
                index1 = 1;
                index2 = 2;
                synchronized (lock) {
                    lib1 = currentUser.getOutsourced()[index1];
                    lib2 = currentUser.getOutsourced()[index2];
                }
                library1 = "MCG";
                library2 = "MON";
                port1 = 1302;
                port2 = 1303;
            }
            if (library.equals("MCG")){
                index1 = 0;
                index2 = 2;
                synchronized (lock) {
                    lib1 = currentUser.getOutsourced()[index1];
                    lib2 = currentUser.getOutsourced()[index2];
                }
                library1 = "CON";
                library2 = "MON";
                port1 = 1301;
                port2 = 1303;
            }
            if (library.equals("MON")){
                index1 = 0;
                index2 = 1;
                synchronized (lock) {
                    lib1 = currentUser.getOutsourced()[index1];
                    lib2 = currentUser.getOutsourced()[index2];
                }
                library1 = "CON";
                library2 = "MCG";
                port1 = 1301;
                port2 = 1302;
            }
            if(!lib1){
                DatagramPacket sendRequest = new DatagramPacket(request.getBytes(),request.length(),host,port1);
                mySocket.send(sendRequest);
                byte[] receive = new byte[1024];
                DatagramPacket receivedReply = new DatagramPacket(receive,receive.length);
                mySocket.receive(receivedReply);
                result = new String(receivedReply.getData()).trim();
            }
            if(result.equals("successful")){
                reply += result + " Delegated Library : " + library1 ;
                boolean[] isOutsourced;
                synchronized (lock) {isOutsourced = currentUser.getOutsourced();}
                isOutsourced[index1] = true;
                synchronized (lock) {currentUser.setOutsourced(isOutsourced);}
            }else if(!lib2){
                DatagramPacket sendRequest = new DatagramPacket(request.getBytes(),request.length(),host,port2);
                mySocket.send(sendRequest);
                byte[] receive = new byte[1024];
                DatagramPacket receivedReply = new DatagramPacket(receive,receive.length);
                mySocket.receive(receivedReply);
                result = new String(receivedReply.getData()).trim();
                System.out.println(result);
            }
            if(result.equals("successful")){
                System.out.println(result);
                reply += result + " Delegated Library : " + library2 ;
                boolean[] isOutsourced;
                synchronized (lock) {isOutsourced = currentUser.getOutsourced();}
                isOutsourced[index2] = true;
                synchronized (lock) {currentUser.setOutsourced(isOutsourced);}
            }
            else{
                reply = "queue";
            }
        }catch (SocketException e){
            writeToLogFile("Socket Exception");
            System.out.println("Socket Exception.");
            e.printStackTrace();
        }catch (UnknownHostException e){
            writeToLogFile("Unknown host Exception");
            System.out.println("Unknown host Exception.");
            e.printStackTrace();
        }catch (IOException e){
            writeToLogFile("IO Exception");
            System.out.println("IO Exception.");
            e.printStackTrace();
        }
        writeToLogFile(reply);
        return reply;
    }

    /**finds the given item name in other library and returns the itemID and availability.*/
    private String findAtOtherLibrary(String itemName){
        String reply = "";
        try{
        DatagramSocket mySocket = new DatagramSocket();
        InetAddress host = InetAddress.getLocalHost();
        int port1 = -1, port2 = -1;
            if (library.equals("CON")){
                port1 = 1302;
                port2 = 1303;
            }
            if (library.equals("MCG")){
                port1 = 1301;
                port2 = 1303;
            }
            if (library.equals("MON")){
                port1 = 1301;
                port2 = 1302;
            }
            String request = library+":findAtOther:"+itemName;
            DatagramPacket sendRequest = new DatagramPacket(request.getBytes(),request.length(),host,port1);
            mySocket.send(sendRequest);
            byte[] receive = new byte[1024];
            DatagramPacket receivedReply = new DatagramPacket(receive,receive.length);
            mySocket.receive(receivedReply);
            reply += new String(receivedReply.getData()).trim();
            reply += "\n";
            sendRequest = new DatagramPacket(request.getBytes(),request.length(),host,port2);
            mySocket.send(sendRequest);
            receive = new byte[1024];
            receivedReply = new DatagramPacket(receive,receive.length);
            mySocket.receive(receivedReply);
            reply += new String(receivedReply.getData()).trim();
        }catch (SocketException e){
            writeToLogFile("Socket Exception");
            System.out.println("Socket Exception.");
            e.printStackTrace();
        }catch (UnknownHostException e){
            writeToLogFile("Unknown host Exception");
            System.out.println("Unknown host Exception.");
            e.printStackTrace();
        }catch (IOException e){
            writeToLogFile("IO Exception");
            System.out.println("IO Exception.");
            e.printStackTrace();
        }
        writeToLogFile(reply);
        return reply;
    }

    /**it simply returns the given item to the library where it belongs to.*/
    private String returnToOtherLibrary(String userID, String itemID){
        String reply ="";
        try{
            DatagramSocket mySocket = new DatagramSocket();
            InetAddress host = InetAddress.getLocalHost();
            int port = -1;
            String library1 = "";
            if (itemID.substring(0,3).equals("CON")){
                port = 1301;
                library1 = "CON";
            }
            if (itemID.substring(0,3).equals("MCG")){
                port = 1302;
                library1 = "MCG";
            }
            if (itemID.substring(0,3).equals("MON")){
                port = 1303;
                library1 = "MON";
            }
            String request = library+":findAtOther:"+userID+":"+itemID;
            DatagramPacket sendRequest = new DatagramPacket(request.getBytes(),request.length(),host,port);
            mySocket.send(sendRequest);
            byte[] receive = new byte[1024];
            DatagramPacket receivedReply = new DatagramPacket(receive,receive.length);
            mySocket.receive(receivedReply);
            reply =  "Borrow Request : Server : " + library +
                    " User : " + userID +
                    " Item :" + itemID +
                    " Status : Successful" +
                    " Delegated Library : " + library1 ;
        }catch (SocketException e){
            writeToLogFile("Socket Exception");
            System.out.println("Socket Exception.");
            e.printStackTrace();
        }catch (UnknownHostException e){
            writeToLogFile("Unknown host Exception");
            System.out.println("Unknown host Exception.");
            e.printStackTrace();
        }catch (IOException e){
            writeToLogFile("IO Exception");
            System.out.println("IO Exception.");
            e.printStackTrace();
        }
        writeToLogFile(reply);
        return reply;
    }

    /**it increases the item count by 1.*/
    protected synchronized void updateItemCount(String itemID){
        Item currentItem = item.get(itemID);
        currentItem.setItemCount(currentItem.getItemCount()+1);
        item.remove(itemID);
        item.put(itemID,currentItem);
    }

    /**to write the logs into log file.*/
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

    /**to authenticate legal item id*/
    private boolean authenticateItemID(String id){
        return id.substring(0, 3).equals(library) && id.length() == 7;
    }

    /**If the returned item is in the waiting queue list, it will
     * automatically assign the item to the first user and send
     * a message to the user.*/
    protected void automaticAssignmentOfBooks(String itemID) {
        Item currentItem = item.get(itemID);
        if(waitingQueue.containsKey(currentItem)){
            HashMap<User,Integer> userList;
            Iterator<Map.Entry<User,Integer>> iterator;
            synchronized (lock) {
                userList = waitingQueue.get(currentItem);
                iterator = userList.entrySet().iterator();
            }
            if(iterator.hasNext()){
                Map.Entry<User,Integer> pair = iterator.next();
                User currentUser =  pair.getKey();
                HashMap<Item,Integer> borrowedItems;
                if(!borrow.containsKey(currentUser)){
                   borrowedItems = new HashMap<>();
                }else{
                    borrowedItems = borrow.get(currentUser);
                    synchronized (lock) {borrow.remove(currentUser);}
                }
                borrowedItems.put(currentItem,pair.getValue());
                synchronized (lock) {borrow.put(currentUser,borrowedItems);}
                ArrayList<String> messages;
                if(messagesForUser.containsKey(currentUser)){
                    messages = messagesForUser.get(currentUser);
                }else{
                    messages = new ArrayList<>();
                }
                String message = "Borrow Request Status : Successful UserID : " + currentUser.getUserID() +" ItemID : " + itemID;
                synchronized (lock) {messages.add(message);}
                writeToLogFile(message);
            }
        }
    }
}
