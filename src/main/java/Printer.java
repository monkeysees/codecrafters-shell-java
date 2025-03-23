import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

interface Printer {
    public void print(Redirect redirect, String data);

    static void print(PrintStream destination, String data) {
        destination.println(data);
    }

    static void print(File destination, String data) {
        print(destination, data, false);
    }

    static void print(File destination, String data, Boolean isAppend) {
        if (destination != null) {
            try (
                    FileWriter fw = new FileWriter(destination, isAppend);
                    PrintWriter writer = new PrintWriter(fw)) {
                writer.println(data);
            } catch (FileNotFoundException e) {
                Printer.print(System.err, String.format("No such file: %s", destination));
            } catch (IOException e) {
                Printer.print(System.err, String.format("Error writing to file %s: %s", destination, e.getMessage()));
            }
        }
    }
}
