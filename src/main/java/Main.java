import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("$ ");
            String[] input = scanner.nextLine().split(" ", 2);
            String command = input[0];
            String commandArg = (input.length > 1) ? input[1] : "";
            if (command.equals("exit") && commandArg.equals("0")) {
                scanner.close();
                System.exit(0);
            }
            if (command.equals("echo")) {
                System.out.println(commandArg);
            } else {
                System.out.println(String.format("%s: command not found", command));
            }
        }
    }
}
