import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Shell {
    File cwd;
    String homeDir;
    String dirsWithExecutables;
    Scanner scanner;

    Shell() {
        cwd = new File(System.getProperty("user.dir"));
        homeDir = System.getenv("HOME");
    }

    void run() {
        scanner = new Scanner(System.in);
        while (true) {
            System.out.print("$ ");
            List<String> input = parseArguments(scanner.nextLine());
            String command = input.get(0);
            List<String> commandArgs = input.size() > 1
                ? input.subList(1, input.size())
                : new ArrayList<String>();
            Program program;
            try {
                program = (BuiltinCommand.isBuiltin(command))
                        ? BuiltinCommand.fromString(command)
                        : new Executable(command);
            } catch (IllegalArgumentException e) {
                System.err.println(String.format("%s: command not found", command));
                continue;
            }
            ExecutionResult result = program.execute(this, commandArgs);
            if (result instanceof ExecutionError) {
                System.err.println(((ExecutionError) result).message);
            }
        }
    }

    void changeDirectory(String path) {
        String normalizedPath = normalizePath(path);
        File newLocation;
        if (isLocalPath(normalizedPath)) {
            newLocation = cwd;
            String[] pathParts = normalizedPath.contains("/")
                    ? normalizedPath.split("/")
                    : new String[] { ".", normalizedPath };
            for (String pathPart : pathParts) {
                newLocation = switch (pathPart) {
                    case "." -> newLocation;
                    case ".." -> newLocation.getParentFile();
                    default -> new File(newLocation, pathPart);
                };
                ensureDirectory(newLocation);
            }
        } else {
            newLocation = new File(normalizedPath);
            ensureDirectory(newLocation);
        }
        cwd = newLocation;
    }

    String normalizePath(String path) {
        return path.equals("~") || path.startsWith("~/")
                ? path.replaceFirst("~", homeDir)
                : path;
    }

    static Boolean isLocalPath(String path) {
        return path.startsWith(".") || !path.contains("/");
    }

    static void ensureDirectory(File dir) {
        if (!dir.exists()) {
            throw new IllegalArgumentException("No such file or directory");
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Not a directory");
        }
    }

    static List<String> parseArguments(String args) {
        List<String> parsedArgs = new ArrayList<String>();
        StringBuilder currentArg = new StringBuilder();
        Boolean insideSingleQuotes = false;

        for (Character c : args.toCharArray()) {
            if (c == '\'') {
                insideSingleQuotes = !insideSingleQuotes;
                continue;
            }

            if (Character.isWhitespace(c) && !insideSingleQuotes) {
                if (currentArg.length() > 0) {
                    parsedArgs.add(currentArg.toString());
                    currentArg = new StringBuilder();
                }
                continue;
            }

            currentArg.append(c);
        }
        if (currentArg.length() > 0) {
            parsedArgs.add(currentArg.toString());
        }

        return parsedArgs;
    }
}