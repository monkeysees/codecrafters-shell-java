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

    Executable(String name, Map<Redirect, ProcessBuilder.Redirect> redirects) {
        super(name, redirects);
        ensureExecutableExists();
    }

    public ExecutionResult execute(Shell shell, List<String> args) {
        List<String> processArgs = new ArrayList<>();
        processArgs.add(this.name);
        processArgs.addAll(args);
        ProcessBuilder processBuilder = new ProcessBuilder(processArgs);
        processBuilder.directory(shell.cwd);
        processBuilder.redirectInput(
                (this.inputRedirect != null)
                        ? (ProcessBuilder.Redirect) this.inputRedirect
                        : (this.inputRedirectAppend != null)
                                ? (ProcessBuilder.Redirect) this.inputRedirectAppend
                                : ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectOutput(
                (this.outputRedirect != null)
                        ? (ProcessBuilder.Redirect) this.outputRedirect
                        : (this.outputRedirectAppend != null)
                                ? (ProcessBuilder.Redirect) this.outputRedirectAppend
                                : ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectError(
                (this.errorRedirect != null)
                        ? (ProcessBuilder.Redirect) this.errorRedirect
                        : (this.errorRedirectAppend != null)
                                ? (ProcessBuilder.Redirect) this.errorRedirectAppend
                                : ProcessBuilder.Redirect.INHERIT);
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
}