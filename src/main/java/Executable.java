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

    Executable(String name, Map<RedirectDescriptor, ProcessBuilder.Redirect> redirects) {
        super(name, redirects);
        ensureExecutableExists();
        Object inputRedirect = redirects.get(RedirectDescriptor.INPUT);
        Object outputRedirect = redirects.get(RedirectDescriptor.OUTPUT);
        Object errorRedirect = redirects.get(RedirectDescriptor.ERROR);
        if (inputRedirect == null) {
            this.inputRedirect = ProcessBuilder.Redirect.INHERIT;
        }
        if (outputRedirect == null) {
            this.outputRedirect = ProcessBuilder.Redirect.INHERIT;
        }
        if (errorRedirect == null) {
            this.errorRedirect = ProcessBuilder.Redirect.INHERIT;
        }
    }

    public ExecutionResult execute(Shell shell, List<String> args) {
        List<String> processArgs = new ArrayList<>();
        processArgs.add(this.name);
        processArgs.addAll(args);
        ProcessBuilder processBuilder = new ProcessBuilder(processArgs);
        processBuilder.directory(shell.cwd);
        processBuilder.redirectInput((ProcessBuilder.Redirect) this.inputRedirect);
        processBuilder.redirectOutput((ProcessBuilder.Redirect) this.outputRedirect);
        processBuilder.redirectError((ProcessBuilder.Redirect) this.errorRedirect);
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