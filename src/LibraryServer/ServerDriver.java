package LibraryServer;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerDriver {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.createRegistry(1304);
            DLSServer concordia = new DLSServer("CON");
            registry.bind("CON",concordia);
            Thread concordiaDelegate = new Thread(new Delegate(1301,concordia));
            concordiaDelegate.start();
            System.out.println("Concordia Library Server bound and started\nInter Server ON.");

            DLSServer mcgill = new DLSServer("MCG");
            registry.bind("MCG",mcgill);
            Thread mcgillDelegate = new Thread(new Delegate(1302,mcgill));
            mcgillDelegate.start();
            System.out.println("McGill Library Server bound and started\nInter Server ON.");


            DLSServer montreal = new DLSServer("MON");
            registry.bind("MON",montreal);
            Thread montrealDelegate = new Thread(new Delegate(1303,montreal));
            montrealDelegate.start();
            System.out.println("Montreal Library Server bound and started]\nInter Server ON.");
            while(true){}

        }catch (RemoteException e){
            System.out.println("Remote exception.");
            e.printStackTrace();
        }catch (AlreadyBoundException e){
            System.out.println("The object is already bound");
            e.printStackTrace();
        }
    }
}
