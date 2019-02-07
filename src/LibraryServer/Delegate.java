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
        String reply = "Borrow Request : Server : " + myServer.getLibrary() +
                "Status :";
        String userID,itemID,itemName;
        int numberOfDays;
        switch (request[1]){
            case "borrowFromOther" :
                if(request.length != 5){
                    reply +=  "Unsuccessful. " +
                              "\nNote : Number of arguments are not sufficient";
                }
                userID = request[2];
                itemID = request[3];
                numberOfDays = Integer.parseInt(request[4]);
                reply = myServer.borrowFromOtherLibrary(userID,itemID,numberOfDays);
                break;
            case "findAtOther" :
                if(request.length != 4){
                    reply +=  "Unsuccessful. " +
                            "\nNote : Number of arguments are not sufficient";
                }
                userID = request[2];
                itemName = request[3];

                break;
            case "returnToOther" :

                break;
             default:
                 break;
        }
    }
}