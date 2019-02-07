package Client;

import java.util.Scanner;

public class UserDriver {
    public static void main(String[] args) {
        System.out.println("Enter your User ID :");
        Scanner sc = new Scanner(System.in);
        String userID = sc.nextLine();
        Thread user = new Thread(new User(userID));
        user.start();
    }

}
