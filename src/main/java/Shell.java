// Setting the console raw mode for character-buffered input
// instead of a line-buffered one is inspired by the following resource:
// https://darkcoding.net/software/non-blocking-console-io-is-not-possible/

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Shell {
    File cwd;
    String homeDir;
    static final Pattern COMMAND_BEGINNING_PATTERN = Pattern.compile("^\\s*(\\S+)$");

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
                String input = readInput();
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
            if (stty != null && ttyConfig != null) {
                stty.execute(this, Arrays.asList(ttyConfig));
            }
        }

        System.exit(isError ? 1 : 0);
    }

    String readInput() throws IOException, UserInitiatedException {
        StringBuilder input = new StringBuilder();
        ByteArrayOutputStream bout;
        int c;
        Boolean isEscaped = false;

        while (true) {
            bout = new ByteArrayOutputStream();
            c = System.in.read();

            // abort
            if (c == -1 || c == 3 || c == 4) {
                throw new AbortException();
            }

            // new line
            if ((c == 13 || c == 10) && !isEscaped) {
                return input.toString();
            }

            // delete
            if (c == 127) {
                if (input.length() > 0) {
                    input.deleteCharAt(input.length() - 1);
                    System.out.print("\b \b");
                    System.out.flush();
                }
                continue;
            }

            // tab autocomplete
            if (c == 9) {
                Matcher commandBeginningMatcher = COMMAND_BEGINNING_PATTERN.matcher(input);
                String commandBeginning = commandBeginningMatcher.find() ? commandBeginningMatcher.group(1) : null;
                if (commandBeginning != null) {
                    String autocompletedProgramName = BuiltinCommand.autocomplete(commandBeginning);
                    if (autocompletedProgramName != null
                            && commandBeginning.length() < autocompletedProgramName.length()) {
                        String autocompletedPortion = autocompletedProgramName.substring(commandBeginning.length())
                                + " ";
                        input.append(autocompletedPortion);
                        System.out.print(autocompletedPortion);
                        continue;
                    }
                }
            }

            bout.write(c);
            String binaryRepresentation = Integer.toBinaryString(c);
            if (binaryRepresentation.length() == 8) {
                // two byte character
                if (binaryRepresentation.startsWith("110")) {
                    bout.write(System.in.read());
                }
                // three byte character
                else if (binaryRepresentation.startsWith("1110")) {
                    bout.write(System.in.read());
                    bout.write(System.in.read());
                }
                // four byte character
                else if (binaryRepresentation.startsWith("11110")) {
                    bout.write(System.in.read());
                    bout.write(System.in.read());
                    bout.write(System.in.read());
                }
            }

            if (c == 13 || c == 10) {
                input.append('\n');
                System.out.print("\r\n");
            } else {
                input.append(bout);
                System.out.print(bout);
            }

            if (c == 92) {
                isEscaped = !isEscaped;
            } else if (isEscaped) {
                isEscaped = false;
            }
        }
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