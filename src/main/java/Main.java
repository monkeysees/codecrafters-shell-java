import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine();
            if (input.equals("exit 0")) {
                scanner.close();
                System.exit(0);
            }
            System.out.println(String.format("%s: command not found", input));
        }
    }
}
