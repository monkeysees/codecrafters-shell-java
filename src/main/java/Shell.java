import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Shell {
    File cwd;
    String homeDir;

    Shell() {
        cwd = new File(System.getProperty("user.dir"));
        homeDir = System.getenv("HOME");
    }

    @SuppressWarnings({ "CallToPrintStackTrace", "UseSpecificCatch" })
    void run() {
        Executable stty = null;
        String ttyConfig = null;
        Boolean isError = false;
        PrintStream rawModeStream = new PrintStream(System.out) {
            @Override
            public void print(String s) {
                super.print(s.replaceAll("\n", "\r\n"));
            }

            @Override
            public void println() {
                super.print("\r\n");
            }

            @Override
            public void println(String s) {
                print(s);
                println();
            }
        };
        System.setOut(rawModeStream);
        System.setErr(rawModeStream);

        try {
            stty = new Executable("stty", Map.of(RedirectType.INPUT, new File("/dev/tty")));
            stty.toggleShouldReturnOutput();
            ttyConfig = stty.execute(this, Arrays.asList("-g")).value.strip();
            if (ttyConfig == null || ttyConfig.length() == 0) {
                throw new Exception("Couldn't safely set up input handling.");
            }
            stty.execute(this, Arrays.asList("raw", "-echo"));

            while (true) {
                System.out.print("$ ");
                String input = Input.readInput();
                System.out.println();
                processInput(input);
            }
        } catch (Error e) {
            isError = true;
        } catch (UserInitiatedException e) {
            if (e instanceof AbortException) {
                isError = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            isError = true;
        } finally {
            if (ttyConfig != null) {
                stty.execute(this, Arrays.asList(ttyConfig));
            }
        }

        System.exit(isError ? 1 : 0);
    }

    void processInput(String input) throws UserInitiatedException {
        Input preparedInput = Input.fromString(input);
        String command = preparedInput.command;
        List<String> commandArgs = preparedInput.args;
        Program program;
        try {
            program = (BuiltinCommand.isBuiltin(command))
                    ? BuiltinCommand.fromName(command, preparedInput.redirects)
                    : new Executable(command, preparedInput.redirects);
        } catch (IllegalArgumentException e) {
            Printer.print(System.err, String.format("%s: command not found", command));
            return;
        }
        ExecutionResult result = program.execute(this, commandArgs);
        if (result instanceof ExecutionError executionError) {
            program.print(program.getErrorRedirect(), executionError.message);
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