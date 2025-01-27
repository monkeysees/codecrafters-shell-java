import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;

public class Printer {
    static void print(Object destination, String data) {
        print(destination, data, false);
    }

    static void print(Object destination, String data, Boolean isError) {
        switch (destination) {
            case PrintStream ps -> ps.println(data);
            case ProcessBuilder.Redirect redirect -> {
                {
                    File file = redirect.file();
                    if (file != null) {
                        try (PrintWriter writer = new PrintWriter(redirect.file())) {
                            writer.println(data);
                        } catch (FileNotFoundException e) {
                            System.err.println(String.format("No such file: %s", file.toString()));
                        }
                    } else {
                        if (isError) {
                            System.err.println(data);
                        } else {
                            System.out.println(data);
                        }
                    }
                }
            }
            default -> {
            }
        }
    }
}
