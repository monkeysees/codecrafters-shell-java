import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    static String EXEC_PATHS = System.getenv("PATH");
    static String[] DIRS_WITH_EXECS = (EXEC_PATHS != null && !EXEC_PATHS.isEmpty())
            ? EXEC_PATHS.split(":")
            : new String[] {};

    private static String getExecAbsolutePath(String execName) {
        for (String dirPath : DIRS_WITH_EXECS) {
            File exec = new File(dirPath, execName);
            if (exec.exists()) {
                return exec.getAbsolutePath();
            }
        }
        return null;
    }

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
                        String commandPath = getExecAbsolutePath(commandArg);
                        if (commandPath != null) {
                            System.out.println(String.format("%s is %s", commandArg, commandPath));
                        } else {
                            System.out.println(String.format("%s: not found", commandArg));
                        }
                    }

                    break;
                default:
                    System.out.println(String.format("%s: command not found", command));
            }
        }
    }
}
