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

    public Delegate(Integer port, DLSServer library){
        this.port = port;
        this.library = library;
    }

    @Override
    public void run() {
        DatagramSocket mySocket = null;
        try {
            mySocket = new DatagramSocket(this.port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        while(true){
            try {
                byte collector[] = new byte[1024];
                DatagramPacket receiver = new DatagramPacket(collector,collector.length);
                mySocket.receive(receiver);
                Thread newThread = new Thread(new RequestHandler(library,mySocket,receiver));
                newThread.start();
                System.out.println("in run");
            }catch(IOException e){
                System.out.println("Input/Output exception");
                e.printStackTrace();
            }
        }
    }
}

class RequestHandler implements Runnable {

    private DLSServer myServer = null;
    private DatagramSocket mySocket = null;
    private DatagramPacket receiver = null;

    public RequestHandler(DLSServer myServer,DatagramSocket mySocket,DatagramPacket receiver){
        this.myServer = myServer;
        this.mySocket = mySocket;
        this.receiver = receiver;
        System.out.println("in constructor");
    }
    /*format |    ServerName:RequestType:Argments     |
    ServerName = from which Server request came
    Request type =  Borrow from other lib.
                    Find at other lib.
                    Return to other lib.
     Arguments = [UserID]
                 [ItemID]
                 [ItemNama]
                 [NumberOfDays]
     */
    @Override
    public void run() {
        String data = new String(receiver.getData()).trim();
        System.out.println("in RequestHandler run\n" + data );
        String[] request = data.split(":");
        String reply = "";
        String userID,itemID,itemName;
        int numberOfDays;
        switch (request[1]){

            case "borrowFromOther" :
                if(request.length != 5){
                    reply = "unsuccessful";
                    break;
                }
                userID = request[2];
                itemID = request[3];
                numberOfDays = Integer.parseInt(request[4]);
                if(!myServer.item.containsKey(itemID)){
                    reply = "unsuccessful";
                    break;
                }
                Item requestedItem = myServer.item.get(itemID);
                if(requestedItem.getItemCount() == 0){
                    reply = "unsuccessful";
                }
                User currentUser = new User(userID);
                HashMap<Item,Integer> entry;
                requestedItem.setItemCount(requestedItem.getItemCount() - 1);
                myServer.item.remove(itemID);
                myServer.item.put(itemID, requestedItem);
                if (myServer.borrow.containsKey(currentUser)) {
                    if (myServer.borrow.get(currentUser).containsKey(requestedItem)) {
                        reply = "unsuccessful";
                        break;
                    } else {
                        entry = myServer.borrow.get(currentUser);
                        myServer.borrow.remove(currentUser);
                    }
                } else {
                    entry = new HashMap<>();
                    reply = "successful.";
                }
                entry.put(requestedItem, numberOfDays);
                myServer.borrow.put(currentUser, entry);
                reply = "successful.";
                    break;

            case "findAtOther" :
                if(request.length != 3){
                    reply = "unsuccessful";
                    break;
                }
                itemName = request[2];
                System.out.println("Item name : " + itemName);
                Iterator<Map.Entry<String,Item>> iterator = myServer.item.entrySet().iterator();
                while(iterator.hasNext()){
                    Map.Entry<String,Item> pair = iterator.next();
                    if(pair.getValue().getItemName().equals(itemName))
                        reply = reply + "\n" + pair.getKey() + " " +pair.getValue().getItemCount();
                }
                break;
            case "returnToOther" :
                if(request.length != 4){
                    reply = "unsuccessful";
                    break;
                }
                userID = request[2];
                itemID = request[3];
                currentUser = myServer.user.get(userID);
                Iterator<Map.Entry<Item,Integer>> value = myServer.borrow.get(currentUser).entrySet().iterator();
                if(!value.hasNext()){
                    reply = "unsuccessful";
                    break;
                }
                boolean status = false;
                while(value.hasNext()) {
                    Map.Entry<Item, Integer> pair = value.next();
                    if(pair.getKey().getItemID().equals(itemID)){
                        myServer.borrow.get(currentUser).remove(pair.getKey());
                        myServer.updateItemCount(itemID);
                        Item currentItem = myServer.item.get(itemID);
                        currentItem.setItemCount(currentItem.getItemCount()+1);
                        myServer.item.remove(itemID);
                        myServer.item.put(itemID,currentItem);
                        reply = "successful";
                        status = true;
                        break;
                    }
                }
                if (status)
                    reply = "unsuccessful";
             default:
                 reply = "unsuccessful";
        }
        DatagramPacket sender = new DatagramPacket(reply.getBytes(), reply.length(), receiver.getAddress(), receiver.getPort());
        try {
            mySocket.send(sender); // send the response DatagramPacket object to the requester again
        } catch (IOException e) {
            System.out.println("IO Exception");
            e.printStackTrace();
        }
    }
}