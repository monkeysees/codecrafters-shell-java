import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Executable implements Program {
    static final String PATH = System.getenv("PATH");
    static final String[] DIRS = (PATH != null && !PATH.isEmpty())
            ? PATH.split(":")
            : new String[] {};
    final String name;

    Executable(String name) {
        String programPath = findProgramPath(name);
        if (programPath == null) {
            throw new IllegalArgumentException(String.format("The program %s is not available via PATH.", name));
        }
        this.name = name;
    }

    public ExecutionResult execute(Shell shell, String args) {
        List<String> programArgs = Arrays.stream(parseArgs(args))
                .filter(s -> !s.isEmpty())
                .toList();
        List<String> processArgs = new ArrayList<>();
        processArgs.add(this.name);
        processArgs.addAll(programArgs);
        ProcessBuilder processBuilder = new ProcessBuilder(processArgs);
        processBuilder.directory(shell.cwd);
        processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
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

    static String findProgramPath(String programName) {
        for (String dirPath : DIRS) {
            File program = new File(dirPath, programName);
            if (program.exists()) {
                return program.getAbsolutePath();
            }
        }
        return null;
    }

    static String[] parseArgs(String args) {
        if (!args.isBlank()) {
            return args.split(" ");
        } else {
            return new String[0];
        }
    }

}