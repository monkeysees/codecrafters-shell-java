import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    static File cwd = new File(System.getProperty("user.dir"));

    static void setCwd(String cwd) {
        File newLocation = new File(cwd);
        if (!newLocation.exists()) {
            throw new IllegalArgumentException("No such file or directory");
        }
        if (!newLocation.isDirectory()) {
            throw new IllegalArgumentException("Not a directory");
        }
        Main.cwd = newLocation;
    }

    private static class Program {
        static final String PATH = System.getenv("PATH");
        static final String[] DIRS = (PATH != null && !PATH.isEmpty())
                ? PATH.split(":")
                : new String[] {};
        private final String name;

        static String getAbsolutePath(String programName) {
            for (String dirPath : DIRS) {
                File program = new File(dirPath, programName);
                if (program.exists()) {
                    return program.getAbsolutePath();
                }
            }
            return null;
        }

        static String[] parseArgs(String args) {
            if (!args.isBlank()) {
                return args.split(" ");
            } else {
                return new String[0];
            }
        }

        Program(String name) {
            String programPath = getAbsolutePath(name);
            if (programPath == null) {
                throw new IllegalArgumentException(String.format("The program %s is not available via PATH.", name));
            }
            this.name = name;
        }

        void run(String[] args) {
            List<String> programArgs = Arrays.stream(args)
                    .filter(s -> !s.isEmpty())
                    .toList();
            List<String> processArgs = new ArrayList<>();
            processArgs.add(this.name);
            processArgs.addAll(programArgs);
            ProcessBuilder processBuilder = new ProcessBuilder(processArgs);
            processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process process;
            try {
                process = processBuilder.start();
                process.waitFor();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            } catch (InterruptedException e) {
                System.err.println(String.format(
                        "Error while running program %s.\n%s",
                        this.name,
                        e.getMessage()));
            }
        }

    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("$ ");
            String[] input = scanner.nextLine().split(" ", 2);
            String command = input[0];
            String commandArgs = (input.length > 1) ? input[1] : "";
            switch (command) {
                case "exit":
                    if (commandArgs.equals("0")) {
                        scanner.close();
                        System.exit(0);
                    } else {
                        break;
                    }
                case "echo":
                    System.out.println(commandArgs);
                    break;
                case "type":
                    List<String> knownBuiltinCommands = new ArrayList<>();
                    knownBuiltinCommands.add("exit");
                    knownBuiltinCommands.add("echo");
                    knownBuiltinCommands.add("type");
                    knownBuiltinCommands.add("pwd");
                    knownBuiltinCommands.add("cd");

                    if (knownBuiltinCommands.contains(commandArgs)) {
                        System.out.println(String.format("%s is a shell builtin", commandArgs));
                    } else {
                        String commandPath = Program.getAbsolutePath(commandArgs);
                        if (commandPath != null) {
                            System.out.println(String.format("%s is %s", commandArgs, commandPath));
                        } else {
                            System.err.println(String.format("%s: not found", commandArgs));
                        }
                    }
                    break;
                case "pwd":
                    if (commandArgs.isEmpty()) {
                        System.out.println(cwd);
                    } else {
                        System.err.println("pwd: too many arguments");
                    }
                    break;
                case "cd":
                    if (commandArgs.isEmpty()) {
                        break;
                    }
                    File newLocation = new File(commandArgs);
                    if (!newLocation.exists()) {
                        System.err.println(String.format("cd: %s: No such file or directory", commandArgs));
                        break;
                    } 
                    if (!newLocation.isDirectory()) {
                        System.err.println(String.format("cd: %s: Not a directory", commandArgs));
                        break;
                    }
                    try {
                        setCwd(commandArgs);    
                    } catch (IllegalArgumentException e) {
                        System.err.println(String.format("cd: %s: %s", commandArgs, e.getMessage()));
                    }
                    break;
                default:
                    try {
                        Program program = new Program(command);
                        String[] programArgs = Program.parseArgs(commandArgs);
                        program.run(programArgs);
                    } catch (IllegalArgumentException e) {
                        System.err.println(String.format("%s: command not found", command));
                    }
            }
        }
    }
}
