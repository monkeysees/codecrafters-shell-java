import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("$ ");
            String[] input = scanner.nextLine().split(" ", 2);
            String command = input[0];
            String commandArg = (input.length > 1) ? input[1] : "";
            switch (command) {
                case "exit":
                    if (commandArg.equals("0")) {
                        scanner.close();
                        System.exit(0);

                    } else {
                        break;
                    }
                case "echo":
                    System.out.println(commandArg);
                    break;
                case "type":
                    List<String> knownBuiltinCommands = new ArrayList<>();
                    knownBuiltinCommands.add("exit");
                    knownBuiltinCommands.add("echo");
                    knownBuiltinCommands.add("type");

                    if (knownBuiltinCommands.contains(commandArg)) {
                        System.out.println(String.format("%s is a shell builtin", commandArg));
                    } else {
                        System.out.println(String.format("%s: not found", commandArg));
                    }

                    break;
                default:
                    System.out.println(String.format("%s: command not found", command));
            }
        }
    }
}
