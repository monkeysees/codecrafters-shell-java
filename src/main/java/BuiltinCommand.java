import java.util.HashMap;
import java.util.Map;

public enum BuiltinCommand implements Program {
    EXIT("exit") {
        @Override
        public ExecutionResult execute(Shell shell, String args) {
            if (args.equals("0")) {
                shell.scanner.close();
                System.exit(0);
            }
            return new ExecutionResult();
        }
    },
    ECHO("echo") {
        @Override
        public ExecutionResult execute(Shell shell, String args) {
            System.out.println(args);
            return new ExecutionResult();
        }
    },
    PWD("pwd") {
        @Override
        public ExecutionResult execute(Shell shell, String args) {
            if (args.isEmpty()) {
                System.out.println(shell.cwd);
                return new ExecutionResult();
            } else {
                return new ExecutionError("pwd: too many arguments");
            }
        }
    },
    CD("cd") {
        @Override
        public ExecutionResult execute(Shell shell, String args) {
            if (!args.isEmpty()) {
                try {
                    shell.changeDirectory(args);
                } catch (IllegalArgumentException e) {
                    return new ExecutionError(String.format("cd: %s: %s", args, e.getMessage()));
                }
            }
            return new ExecutionResult();
        }
    },
    TYPE("type") {
        @Override
        public ExecutionResult execute(Shell shell, String args) {
            if (isBuiltin(args)) {
                System.out.println(String.format("%s is a shell builtin", args));
            } else {
                String path = Executable.findProgramPath(args);
                if (path != null) {
                    System.out.println(String.format("%s is %s", args, path));
                } else {
                    return new ExecutionError(String.format("%s: not found", args));
                }
            }
            return new ExecutionResult();
        }
    };

    public abstract ExecutionResult execute(Shell shell, String args);

    final String name;

    static final Map<String, BuiltinCommand> NAME_TO_COMMAND = new HashMap<>();

    static {
        for (BuiltinCommand command : values()) {
            NAME_TO_COMMAND.put(command.name, command);
        }
    }

    BuiltinCommand(String name) {
        this.name = name;
    }

    public static BuiltinCommand fromString(String name) {
        BuiltinCommand command = NAME_TO_COMMAND.get(name);
        if (command == null) {
            throw new IllegalArgumentException("Unknown command: " + name);
        }
        return command;
    }

    public static boolean isBuiltin(String name) {
        return NAME_TO_COMMAND.containsKey(name);
    }
}
