import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Executable extends Program {
    static final String PATH = System.getenv("PATH");
    static final String[] DIRS = (PATH != null && !PATH.isEmpty())
            ? PATH.split(":")
            : new String[] {};

    Executable(String name) {
        this(name, null);
    }

    Executable(String name, Map<RedirectType, File> redirects) {
        super(name, redirects);
        ensureExecutableExists();
    }

    public ExecutionResult execute(Shell shell, List<String> args) {
        List<String> processArgs = new ArrayList<>();
        processArgs.add(this.name);
        processArgs.addAll(args);
        ProcessBuilder processBuilder = new ProcessBuilder(processArgs);
        processBuilder.directory(shell.cwd);
        processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectOutput(getProcessRedirect(getOutputRedirect()));
        processBuilder.redirectError(getProcessRedirect(getErrorRedirect()));
        ;
        try {
            Process process = processBuilder.start();
            process.waitFor();
            return new ExecutionResult();
        } catch (IOException e) {
            return new ExecutionError(e.getMessage());
        } catch (InterruptedException e) {
            return new ExecutionError(String.format(
                    "Error while running program %s.\n%s",
                    this.name,
                    e.getMessage()));
        }
    }

    private void ensureExecutableExists() {
        String programPath = findExecutablePath(this.name);
        if (programPath == null) {
            throw new IllegalArgumentException(String.format("The program %s is not available via PATH.", this.name));
        }
    }

    static String findExecutablePath(String programName) {
        for (String dirPath : DIRS) {
            File program = new File(dirPath, programName);
            if (program.exists()) {
                return program.getAbsolutePath();
            }
        }
        return null;
    }

    public ProcessBuilder.Redirect getProcessRedirect(Redirect redirect) {
        if (redirect == null || redirect.file == null) {
            return ProcessBuilder.Redirect.INHERIT;
        }

        return RedirectType.isAppend(redirect.type)
                ? ProcessBuilder.Redirect.appendTo(redirect.file)
                : ProcessBuilder.Redirect.to(redirect.file);
    }

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
        }
    }
}