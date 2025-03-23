import java.io.File;
import java.io.IOException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Input {
    public String command;
    public List<String> args;
    public Map<RedirectType, File> redirects;

    Input(String command, List<String> args, Map<RedirectType, File> redirects) {
        this.command = command;
        this.args = args;
        this.redirects = redirects;
    }

    private static String parseArgFromIterator(CharacterIterator it) {
        StringBuilder arg = new StringBuilder();

        for (Character c = it.current(); c != CharacterIterator.DONE && !Character.isWhitespace(c); c = it.next()) {
            if (c == '\'') {
                Character quotedChar = it.next();
                while (quotedChar != '\'' && quotedChar != CharacterIterator.DONE) {
                    arg.append(quotedChar);
                    quotedChar = it.next();
                }
                continue;
            }

            if (c == '"') {
                Character quotedChar = it.next();
                while (quotedChar != '"' && quotedChar != CharacterIterator.DONE) {
                    if (quotedChar == '\\') {
                        Character nextChar = it.next();
                        if (Set.of('\\', '$', '"', '\n').contains(nextChar)) {
                            quotedChar = nextChar;
                        } else {
                            it.previous();
                        }
                    }
                    arg.append(quotedChar);
                    quotedChar = it.next();
                }
                continue;
            }

            if (c == '\\') {
                Character nextChar = it.next();
                if (nextChar != CharacterIterator.DONE) {
                    arg.append(nextChar);
                }
                continue;
            }

            arg.append(c);
        }

        return arg.toString();
    }

    private static String parseRedirectTypeFromIterator(CharacterIterator it) {
        StringBuilder operator = new StringBuilder();

        Character firstChar = it.current();

        if (!Set.of('1', '2', '>').contains(firstChar)) {
            return null;
        }

        operator.append(firstChar);

        for (Character c = it.next(); c == '>' && operator.length() < 3; c = it.next()) {
            operator.append(c);
        }

        if (!Character.isWhitespace(it.current()) || !operator.toString().matches("^[012]?>{1,2}$")) {
            for (int i = 0; i < operator.length(); i++) {
                it.previous();
            }
            return null;
        }

        return operator.toString();
    }

    private static void skipIteratorWhitespace(CharacterIterator it) {
        while (Character.isWhitespace(it.current())) {
            it.next();
        }
    }

    public static Input fromString(String s) {
        List<String> parsedCommandAndArgs = new ArrayList<>();
        HashMap<RedirectType, File> redirects = new HashMap<>();

        CharacterIterator it = new StringCharacterIterator(s);
        while (it.current() != CharacterIterator.DONE) {
            skipIteratorWhitespace(it);

            String redirectOperator = parseRedirectTypeFromIterator(it);
            if (redirectOperator != null) {
                skipIteratorWhitespace(it);
                String redirectTo = parseArgFromIterator(it);
                File redirectFile = new File(redirectTo.toString());
                File redirectParentFile = redirectFile.getParentFile();
                if (redirectParentFile != null && redirectParentFile.exists()) {
                    if (!redirectFile.exists()) {
                        try {
                            redirectFile.createNewFile();
                        } catch (IOException e) {
                            Printer.print(System.err, String.format("Couldn't create the file to redirect to: %s",
                                    redirectFile.toString()));
                            continue;
                        }
                    }
                    redirects.put(RedirectType.fromString(redirectOperator), new File(redirectTo));
                } else {
                    Printer.print(System.err, String.format("No such file or directory: %s", redirectFile.toString()));
                }
                continue;
            }

            parsedCommandAndArgs.add(parseArgFromIterator(it));
        }

        String command = parsedCommandAndArgs.get(0);
        List<String> commandArgs = parsedCommandAndArgs.size() > 1
                ? parsedCommandAndArgs.subList(1, parsedCommandAndArgs.size())
                : new ArrayList<>();

        return new Input(command, commandArgs, redirects);
    }
}
