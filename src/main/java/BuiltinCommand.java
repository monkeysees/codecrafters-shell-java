import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BuiltinCommand extends Program {
    static Set<String> COMMANDS = new HashSet<>(Arrays.asList("exit", "echo", "pwd", "cd", "type"));

    BuiltinCommand(String name, Map<RedirectType, File> redirects) {
        super(name, redirects);
    }

    static class Exit extends BuiltinCommand {
        Exit(Map<RedirectType, File> redirects) {
            super("exit", redirects);
        }

        @Override
        public ExecutionResult execute(Shell shell, List<String> args) throws ExitException {
            throw new ExitException();
        }
    }

    static class Echo extends BuiltinCommand {
        Echo(Map<RedirectType, File> redirects) {
            super("echo", redirects);
        }

        @Override
        public ExecutionResult execute(Shell shell, List<String> args) {
            print(getOutputRedirect(), String.join(" ", args));
            return new ExecutionResult();
        }
    }

    static class PWD extends BuiltinCommand {
        PWD(Map<RedirectType, File> redirects) {
            super("pwd", redirects);
        }

        @Override
        public ExecutionResult execute(Shell shell, List<String> args) {
            if (args.isEmpty()) {
                print(getOutputRedirect(), shell.cwd.toString());
                return new ExecutionResult();
            } else {
                return new ExecutionError("pwd: too many arguments");
            }
        }
    }

    static class CD extends BuiltinCommand {
        CD(Map<RedirectType, File> redirects) {
            super("cd", redirects);
        }

        @Override
        public ExecutionResult execute(Shell shell, List<String> args) {
            if (args.size() > 1) {
                return new ExecutionError("cd: Only one argument is allowed");
            }

            if (!args.isEmpty()) {
                try {
                    shell.changeDirectory(args.get(0));
                } catch (IllegalArgumentException e) {
                    return new ExecutionError(String.format("cd: %s: %s", args.get(0), e.getMessage()));
                }
            }
            return new ExecutionResult();
        }
    }

    static class Type extends BuiltinCommand {
        Type(Map<RedirectType, File> redirects) {
            super("type", redirects);
        }

        @Override
        public ExecutionResult execute(Shell shell, List<String> args) {
            for (String arg : args) {
                if (isBuiltin(arg)) {
                    print(getOutputRedirect(), String.format("%s is a shell builtin", arg));
                } else {
                    String path = Executable.findExecutablePath(arg);
                    if (path != null) {
                        print(getOutputRedirect(), String.format("%s is %s", arg, path));
                    } else {
                        return new ExecutionError(String.format("%s: not found", arg));
                    }
                }
            }
            return new ExecutionResult();
        }

    }

    public static BuiltinCommand fromName(String name) {
        return fromName(name, null);
    }

    public static BuiltinCommand fromName(String name, Map<RedirectType, File> redirects) {
        return switch (name) {
            case "exit" -> new Exit(redirects);
            case "echo" -> new Echo(redirects);
            case "pwd" -> new PWD(redirects);
            case "cd" -> new CD(redirects);
            case "type" -> new Type(redirects);
            default -> throw new IllegalArgumentException("Unknown command: " + name);
        };
    }

    public static boolean isBuiltin(String name) {
        return COMMANDS.contains(name);
    }

    public static List<String> autocomplete(String commandBeginning) {
        return COMMANDS.stream()
                .filter(command -> command.startsWith(commandBeginning))
                .toList();
    }

    @Override
    public void print(Redirect redirect, String data) {
        switch (redirect.type) {
            case OUTPUT -> {
                if (redirect.file != null) {
                    Printer.print(redirect.file, data);
                } else {
                    Printer.print(System.out, data);
                }
            }

            case OUTPUT_APPEND -> {
                if (redirect.file != null) {
                    Printer.print(redirect.file, data, true);
                } else {
                    Printer.print(System.out, data);
                }
            }

            case ERROR -> {
                if (redirect.file != null) {
                    Printer.print(redirect.file, data);
                } else {
                    Printer.print(System.err, data);
                }
            }

            case ERROR_APPEND -> {
                if (redirect.file != null) {
                    Printer.print(redirect.file, data, true);
                } else {
                    Printer.print(System.err, data);
                }
            }

            case INPUT -> throw new UnsupportedOperationException("Unimplemented case: " + redirect.type);
            case INPUT_APPEND -> throw new UnsupportedOperationException("Unimplemented case: " + redirect.type);
            default -> throw new IllegalArgumentException("Unexpected value: " + redirect.type);
        }
    }
}
