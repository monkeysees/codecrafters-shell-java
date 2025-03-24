import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Executable extends Program {
    static final String PATH = System.getenv("PATH");
    static final String[] DIRS = (PATH != null && !PATH.isEmpty())
            ? PATH.split(":")
            : new String[] {};
    Boolean shouldReturnOutput = false;

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

        ProcessBuilder.Redirect redirectInput = ProcessBuilder.Redirect.INHERIT;
        ProcessBuilder.Redirect redirectOutput = getProcessRedirect(getOutputRedirect());
        ProcessBuilder.Redirect redirectError = getProcessRedirect(getErrorRedirect());

        processBuilder.redirectInput(redirectInput);
        if (!shouldReturnOutput) {
            processBuilder.redirectOutput(redirectOutput);
        }
        processBuilder.redirectError(redirectError);

        try {
            Process process = processBuilder.start();
            process.waitFor();
            String output = null;

            if (shouldReturnOutput || redirectOutput == ProcessBuilder.Redirect.PIPE) {
                output = new String(process.getInputStream().readAllBytes());
                if (!shouldReturnOutput) {
                    if (output.length() > 0) {
                        System.out.print(output);
                    }
                }
            }

            if (redirectError == ProcessBuilder.Redirect.PIPE) {
                String error = new String(process.getErrorStream().readAllBytes());
                if (error.length() > 0) {
                    System.err.print(error);
                }
            }

            return shouldReturnOutput
                    ? new ExecutionResult(output)
                    : new ExecutionResult();
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
            return ProcessBuilder.Redirect.PIPE;
        }

        return RedirectType.isAppend(redirect.type)
                ? ProcessBuilder.Redirect.appendTo(redirect.file)
                : ProcessBuilder.Redirect.to(redirect.file);
    }

    public static String autocomplete(String commandBeginning) {
        return Arrays.stream(DIRS)
                .map(dir -> new File(dir))
                .flatMap(dirFile -> dirFile.listFiles() != null ? Arrays.stream(dirFile.listFiles()) : null)
                .map(exec -> exec.getName())
                .filter(execName -> execName.startsWith(commandBeginning))
                .findFirst()
                .orElse(null);
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

    void toggleShouldReturnOutput() {
        this.shouldReturnOutput = !this.shouldReturnOutput;
    }
}