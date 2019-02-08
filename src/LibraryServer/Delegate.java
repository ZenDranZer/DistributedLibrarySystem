package LibraryServer;

/*
* Library CON port = 1301
* Library MCG port = 1302
* Library MON port = 1303
* */


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Delegate implements Runnable{

    private Integer port;
    private DLSServer library;
    private DatagramSocket mySocket = null;
    private DatagramPacket receiver;
    private byte collector[];

    public Delegate(Integer port, DLSServer library){
        this.port = port;
        this.library = library;
        try {
            mySocket = new DatagramSocket(this.port);
        }catch (SocketException e){
            System.out.println("Socket Exception");
            e.printStackTrace();
        }
        collector = new byte[1024];
        receiver = new DatagramPacket(collector,collector.length);
    }

    @Override
    public void run() {
        while(true){
            try {
                mySocket.receive(receiver);
                new RequestHandler(library,mySocket,receiver).start();
            }catch(IOException e){
                System.out.println("Input/Output exception");
                e.printStackTrace();
            }
        }
    }
}

class RequestHandler extends Thread {

    private DLSServer myServer = null;
    private DatagramSocket mySocket = null;
    private DatagramPacket receiver = null;

    public RequestHandler(DLSServer myServer,DatagramSocket mySocket,DatagramPacket receiver){
        this.myServer = myServer;
        this.mySocket = mySocket;
        this.receiver = receiver;
    }
    /*format |    ServerName:RequestType:Argments     |
    ServerName = from which Server request came
    Request type =  Borrow from other lib.
                    Find at other lib.
                    Return to other lib.
     Arguments = UserID
                 ItemID
                 [ItemNama]
                 [NumberOfDays]
     */
    @Override
    public void run() {
        String data = new String(receiver.getData()).trim();
        String[] request = data.split(":");
        String reply = "Borrow Request : Server : " + myServer.getLibrary();
        String userID,itemID,itemName;
        int numberOfDays;
        switch (request[1]){

            case "borrowFromOther" :
                if(request.length != 5){
                    reply += "\nNote : Number of arguments are not sufficient" +
                             "\nStatus : unsuccessful";
                    break;
                }
                userID = request[2];
                itemID = request[3];
                numberOfDays = Integer.parseInt(request[4]);
                if(!myServer.item.containsKey(itemID)){
                    reply += "\n Note : ItemID does not exist here. \nStatus : unsuccessful";
                    break;
                }
                Item requestedItem = myServer.item.get(itemID);
                if(requestedItem.getItemCount() == 0){
                    reply += "\n Note : Not sufficient books. \nStatus : unsuccessful";
                }
                User currentUser = new User(userID);
                HashMap<Item,Integer> entry;
                requestedItem.setItemCount(requestedItem.getItemCount() - 1);
                myServer.item.remove(itemID);
                myServer.item.put(itemID, requestedItem);
                if (myServer.borrow.containsKey(currentUser)) {
                    if (myServer.borrow.get(currentUser).containsKey(requestedItem)) {
                        reply += "Unsuccessful. \n Note : User have already borrowed.";
                        break;
                    } else {
                        entry = myServer.borrow.get(currentUser);
                        myServer.borrow.remove(currentUser);
                    }
                } else {
                    entry = new HashMap<>();
                    reply += "Successful.";
                }
                entry.put(requestedItem, numberOfDays);
                myServer.borrow.put(currentUser, entry);
                reply += "Successful.";
                    break;

            case "findAtOther" :
                if(request.length != 3){
                    reply += "\nNote : Number of arguments are not sufficient" +
                             "Status : unsuccessful";
                    break;
                }
                reply = reply + "Successful ";
                itemName = request[2];
                Iterator<Map.Entry<String,Item>> iterator = myServer.item.entrySet().iterator();
                while(iterator.hasNext()){
                    Map.Entry<String,Item> pair = iterator.next();
                    if(pair.getValue().getItemName().equals(itemName))
                        reply = reply + "\n" + pair.getKey() + " " +pair.getValue().getItemCount();
                }

            case "returnToOther" :
                if(request.length != 4){
                    reply += "\nNote : Number of arguments are not sufficient" +
                            "Status : unsuccessful";
                    break;
                }
                userID = request[2];
                itemID = request[3];
                currentUser = myServer.user.get(userID);
                Iterator<Map.Entry<Item,Integer>> value = myServer.borrow.get(currentUser).entrySet().iterator();
                if(!value.hasNext()){
                    reply += "\nNote : Not borrowed from this library. \n Status : unsuccessful";
                    break;
                }
                boolean status = false;
                while(value.hasNext()) {
                    Map.Entry<Item, Integer> pair = value.next();
                    if(pair.getKey().getItemID().equals(itemID)){
                        myServer.borrow.get(currentUser).remove(pair.getKey());
                        myServer.updateItemCount(itemID);
                        reply += "Successful";
                        status = true;
                        break;
                    }
                }
                if (status)
                    reply += "\nNote : Item have not borrowed from this library.\n Status : unsuccessful";
             default:
                 reply += "\nNote : Wrong option selection.\nStatus : Unsuccessful";
        }

    }
}