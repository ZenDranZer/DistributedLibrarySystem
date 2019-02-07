package Client;

import java.util.Scanner;

public class ManagerDriver {
    public static void main(String[] args) {
        System.out.println("Enter your Manager ID :");
        Scanner sc = new Scanner(System.in);
        String managerID = sc.nextLine();
        Thread manager = new Thread(new Manager(managerID));
        manager.start();    }
}
