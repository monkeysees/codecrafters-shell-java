import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BuiltinCommand extends Program {
    static Set<String> COMMANDS = new HashSet<>(Arrays.asList("exit", "echo", "pwd", "cd", "type"));

    BuiltinCommand(String name) {
        this(name, null);
    }

    BuiltinCommand(String name, Map<RedirectDescriptor, ProcessBuilder.Redirect> redirects) {
        super(name, redirects);
        Object inputRedirect = redirects.get(RedirectDescriptor.INPUT);
        Object outputRedirect = redirects.get(RedirectDescriptor.OUTPUT);
        Object errorRedirect = redirects.get(RedirectDescriptor.ERROR);
        if (inputRedirect == null) {
            this.inputRedirect = System.in;
        }
        if (outputRedirect == null) {
            this.outputRedirect = System.out;
        }
        if (errorRedirect == null) {
            this.errorRedirect = System.err;
        }
    }

    static class Exit extends BuiltinCommand {
        String name = "exit";

        Exit(Map<RedirectDescriptor, ProcessBuilder.Redirect> redirects) {
            super("exit", redirects);
        }

        public ExecutionResult execute(Shell shell, List<String> args) {
            if (args.get(0).equals("0")) {
                shell.scanner.close();
                System.exit(0);
            }
            return new ExecutionResult();
        }
    }

    static class Echo extends BuiltinCommand {
        String name = "echo";

        Echo(Map<RedirectDescriptor, ProcessBuilder.Redirect> redirects) {
            super("echo", redirects);
        }

        public ExecutionResult execute(Shell shell, List<String> args) {
            Printer.print(this.outputRedirect, String.join(" ", args));
            return new ExecutionResult();
        }
    }

    static class PWD extends BuiltinCommand {
        String name = "pwd";

        PWD(Map<RedirectDescriptor, ProcessBuilder.Redirect> redirects) {
            super("pwd", redirects);
        }

        public ExecutionResult execute(Shell shell, List<String> args) {
            if (args.isEmpty()) {
                Printer.print(this.outputRedirect, shell.cwd.toString());
                return new ExecutionResult();
            } else {
                return new ExecutionError("pwd: too many arguments");
            }
        }
    }

    static class CD extends BuiltinCommand {
        String name = "cd";

        CD(Map<RedirectDescriptor, ProcessBuilder.Redirect> redirects) {
            super("cd", redirects);
        }

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
        String name = "type";

        Type(Map<RedirectDescriptor, ProcessBuilder.Redirect> redirects) {
            super("type", redirects);
        }

        public ExecutionResult execute(Shell shell, List<String> args) {
            for (String arg : args) {
                if (isBuiltin(arg)) {
                    Printer.print(this.outputRedirect, String.format("%s is a shell builtin", arg));
                } else {
                    String path = Executable.findExecutablePath(arg);
                    if (path != null) {
                        Printer.print(this.outputRedirect, String.format("%s is %s", arg, path));
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

    public static BuiltinCommand fromName(String name, Map<RedirectDescriptor, ProcessBuilder.Redirect> redirects) {
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
}
