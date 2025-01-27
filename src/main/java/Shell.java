import java.io.File;
import java.util.List;
import java.util.Scanner;

public class Shell {
    File cwd;
    String homeDir;
    Scanner scanner;

    Shell() {
        cwd = new File(System.getProperty("user.dir"));
        homeDir = System.getenv("HOME");
    }

    void run() {
        scanner = new Scanner(System.in);
        while (true) {
            System.out.print("$ ");
            String scannedInput = scanner.nextLine();
            Input input = Input.fromString(scannedInput);
            String command = input.command;
            List<String> commandArgs = input.args;
            Program program;
            try {
                program = (BuiltinCommand.isBuiltin(command))
                        ? BuiltinCommand.fromName(command, input.redirects)
                        : new Executable(command, input.redirects);
            } catch (IllegalArgumentException e) {
                System.err.println(String.format("%s: command not found", command));
                continue;
            }
            ExecutionResult result = (program instanceof Executable)
                    ? program.execute(this, commandArgs)
                    : program.execute(this, commandArgs);
            if (result instanceof ExecutionError executionError) {
                switch (program) {
                    case Executable exec -> {
                        Printer.print(exec.errorRedirect != null
                                ? exec.errorRedirect
                                : exec.errorRedirectAppend != null
                                        ? exec.errorRedirectAppend
                                        : ProcessBuilder.Redirect.INHERIT,
                                executionError.message, true);
                    }
                    case BuiltinCommand builtin -> {
                        Printer.print(builtin.errorRedirect != null
                                ? builtin.errorRedirect
                                : builtin.errorRedirectAppend != null
                                        ? builtin.errorRedirectAppend
                                        : System.err,
                                executionError.message, true);
                    }
                    default -> {
                    }
                }
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
}