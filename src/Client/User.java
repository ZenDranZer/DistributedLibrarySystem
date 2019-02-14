package Client;

import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Calendar;
import LibraryServer.LibraryUserInterface;


public class User implements Runnable {

    private String clientID;
    private String library;
    private String type;
    private String index;
    private BufferedReader sc;
    private File logFile;
    private PrintWriter logger;


    public User(String clientID){
        this.clientID = clientID;
        this.library = clientID.substring(0,3);
        this.type = String.valueOf(clientID.charAt(3));
        this.index = clientID.substring(4);
        sc = new BufferedReader(new InputStreamReader(System.in));
        logFile = new File("C:\\Users\\SARVESH\\Documents\\DistributedLibrarySystem\\src\\Logs\\log_" + library + "_"+ clientID+ ".log");
        try{
            if(!logFile.exists())
                logFile.createNewFile();
            logger = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
        }catch (IOException io){
            System.out.println("Error in creating log file.");
            io.printStackTrace();
        }
        writeToLogFile("User " + clientID + " Started.");
    }

    private void printUserOptions(){
        System.out.println("Features :");
        System.out.println("1) Borrow an item.");
        System.out.println("2) Find an Item.");
        System.out.println("3) Return an Item.");
        System.out.println("Press 'N' or 'n' to exit.");
    }

    public void run(){
        char op = 'Y';
        try {
            Registry registry = LocateRegistry.getRegistry(1304);
            LibraryUserInterface user = (LibraryUserInterface) registry.lookup(library);
            String[] response = user.validateClientID(clientID).split(":");
            if(response[0].equals("false")) {
                System.out.println("Provided ID is wrong!! please invoke the client again.");
                writeToLogFile("User id: " + clientID + " Provided ID is wrong!! please invoke the client again.");
                System.exit(0);
            }
            System.out.println("Hello " + clientID + " Messages from Server");
            for (String message : response) {
                System.out.println("Message : " + message);
            }
        while (op == 'Y' || op == 'y'){
            printUserOptions();
            op = sc.readLine().charAt(0);
            switch (op){
                case '1':
                    System.out.println("Borrow Item Section :");
                    System.out.println("Enter Item ID: ");
                    String itemID = sc.readLine();
                    System.out.println("Enter for how many days you want to borrow ?");
                    Integer numberOfDays = new Integer(sc.readLine());
                    String reply = user.borrowItem(clientID,itemID,numberOfDays);
                    System.out.println("Reply from server : ");
                    if(reply.equals("outsourced")){
                        System.out.println("Your library does not have demanded item.");
                        System.out.println("Do you want to get it from another library (Y/N)?");
                        String ch = sc.readLine();
                        if (ch.charAt(0) == 'Y' || ch.charAt(0) == 'y')
                            reply = user.borrowFromOtherLibrary(clientID,itemID,numberOfDays);
                        if(reply.equals("queue")){
                            System.out.println("Item is not available at all library, do wanna put yourself in a queue (Y/N) ? ");
                            ch = sc.readLine();
                            if (ch.charAt(0) == 'Y' || ch.charAt(0) == 'y')
                                reply = user.addToQueue(clientID,itemID,numberOfDays);
                        }
                    }
                    writeToLogFile(reply);
                    System.out.println(reply);
                    op = 'Y';
                    break;
                case '2':
                    System.out.println("Find Item Section :");
                    System.out.println("Enter Item Name :");
                    itemID = sc.readLine();
                    reply = user.findItem(clientID,itemID);
                    writeToLogFile(reply);
                    System.out.println("Reply from server : " + reply);
                    op = 'Y';
                    break;
                case '3':
                    System.out.println("Return Item Section :");
                    System.out.println("Enter Item ID :");
                    itemID = sc.readLine();
                    reply = user.returnItem(clientID,itemID);
                    writeToLogFile(reply);
                    System.out.println("Reply from server : " + reply);
                    op = 'Y';
                    break;
                case 'N':
                    writeToLogFile("User Quit : UserID : " + clientID);
                    break;
                case 'n':
                    writeToLogFile("User Quit : UserID : " + clientID);
                    break;
                default:
                    System.out.println("Wrong Selection!");
                    op = 'Y';
                    break;
            }
        }
        System.out.println("Bye " + clientID);
        System.exit(0);
        }catch(NotBoundException e){
            writeToLogFile("Object is not bound");
            System.out.println("Server object is not bound.");
            e.printStackTrace();
        }catch(RemoteException e){
            writeToLogFile("Remote Exception");
            System.out.println("Remote Exception.");
            e.printStackTrace();
        }
        catch (IOException e){
            writeToLogFile("IO Exception");
            System.out.println("IO Exception.");
            e.printStackTrace();
        }
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
