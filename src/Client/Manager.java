package Client;

import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Calendar;
import LibraryServer.LibraryManagerInterface;

public class Manager implements Runnable {

    private String clientID;
    private String library;
    private String type;
    private String index;
    private BufferedReader sc;
    private File logFile;
    private PrintWriter logger;

    public Manager(String clientID){
        this.clientID = clientID;
        this.library = clientID.substring(0,3);
        this.type = String.valueOf(clientID.charAt(3));
        this.index = clientID.substring(4);
        sc = new BufferedReader(new InputStreamReader(System.in));
        logFile = new File("C:\\Users\\SARVESH\\Documents\\DistributedLibrarySystem\\src\\Logs\\log_" + library + "_"+ clientID + ".log");
        try{
            if(!logFile.exists())
                logFile.createNewFile();
            logger = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
        }catch (IOException io){
            System.out.println("Error in creating log file.");
            io.printStackTrace();
        }
        writeToLogFile("Manager " + clientID + " Started.");
    }

    private void printManagerOptions(){
        System.out.println("Features :");
        System.out.println("1) Add an item.");
        System.out.println("2) Remove an Item.");
        System.out.println("3) List Item Availability.");
        System.out.println("4) Create User.");
        System.out.println("5) Create Manager.");
        System.out.println("Press 'N' or 'n' to exit.");
    }

    public void run(){
        char op = 'Y';

        try {
            Registry registry = LocateRegistry.getRegistry(1304);
            LibraryManagerInterface manager = (LibraryManagerInterface) registry.lookup(library);
            if(manager.validateClientID(clientID).equals("false")) {
                System.out.println("Provided ID is wrong!! please invoke the client again.");
                writeToLogFile("UserID : " + clientID +" Provided ID is wrong!! please invoke the client again. ");
                System.exit(0);
            }
            System.out.println("Hello " + clientID);
            while (op == 'Y' || op == 'y'){
                printManagerOptions();
                op = sc.readLine().charAt(0);
                switch (op){
                    case '1':
                        System.out.println("Enter Item ID: ");
                        String itemID = sc.readLine();
                        System.out.println("Enter Item Name: ");
                        String itemName = sc.readLine();
                        System.out.println("Enter Item quantity: ");
                        Integer quantity = new Integer(sc.readLine());
                        String reply = manager.addItem(clientID,itemID,itemName,quantity);
                        writeToLogFile(reply);
                        System.out.println("Reply from Server : " + reply);
                        op = 'Y';
                        break;
                    case '2':
                        System.out.println("Enter Item ID: ");
                        itemID = sc.readLine();
                        System.out.println("Enter Item quantity: ");
                        quantity = new Integer(sc.readLine());
                        reply = manager.removeItem(clientID,itemID,quantity);
                        writeToLogFile(reply);
                        System.out.println("Reply from Server : " + reply);
                        op = 'Y';
                        break;
                    case '3':
                        reply = manager.listItemAvailability(clientID);
                        writeToLogFile(reply);
                        System.out.println("Reply from Server : \n" + reply);
                        op = 'Y';
                        break;
                    case '4':
                        reply = manager.createUser(clientID);
                        writeToLogFile(reply);
                        System.out.println("Reply from Server : \n" + reply);
                        op = 'Y';
                        break;
                    case '5':
                        reply = manager.createManager(clientID);
                        writeToLogFile(reply);
                        System.out.println("Reply from Server : \n" + reply);
                        op = 'Y';
                        break;
                    case 'N':
                        writeToLogFile("Manager Quit : UserID : " + clientID);
                        break;
                    case 'n':
                        writeToLogFile("Manager Quit : UserID : " + clientID);
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
            System.out.println("Server object is not bound.");
            writeToLogFile("Not Bound Exception");
            e.printStackTrace();
        }catch(RemoteException e){
            System.out.println("Remote Exception.");
            writeToLogFile("Remote Exception");
            e.printStackTrace();
        }catch(IOException e){
            System.out.println("IO Exception.");
            writeToLogFile("IO Exception");
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
