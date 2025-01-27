import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
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
                        try (
                                FileWriter fw = new FileWriter(
                                        redirect.file(),
                                        redirect.type() == ProcessBuilder.Redirect.Type.APPEND);
                                PrintWriter writer = new PrintWriter(fw)) {
                            writer.println(data);
                        } catch (FileNotFoundException e) {
                            System.err.println(String.format("No such file: %s", redirect.file()));
                        } catch (IOException e) {
                            System.err.println(
                                    String.format("Error writing to file %s: %s", redirect.file(), e.getMessage()));
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
