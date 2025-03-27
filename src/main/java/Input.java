// Setting the console raw mode for character-buffered input
// instead of a line-buffered one is inspired by the following resource:
// https://darkcoding.net/software/non-blocking-console-io-is-not-possible/

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Input {
    public String command;
    public List<String> args;
    public Map<RedirectType, File> redirects;
    static final Pattern COMMAND_BEGINNING_PATTERN = Pattern.compile("^\\s*(\\S+)$");
    static final Character BELL_CHARACTER = '\u0007';

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
                File redirectFile = new File(redirectTo);
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

    static String readInput() throws IOException, UserInitiatedException {
        StringBuilder input = new StringBuilder();
        ByteArrayOutputStream bout;
        int c;
        Boolean isEscaped = false;
        Integer consecutiveTabsCount = 0;

        while (true) {
            bout = new ByteArrayOutputStream();
            c = System.in.read();

            // abort
            if (c == -1 || c == 3 || c == 4) {
                throw new AbortException();
            }

            // new line
            if ((c == 13 || c == 10) && !isEscaped) {
                return input.toString();
            }

            // delete
            if (c == 127) {
                if (input.length() > 0) {
                    input.deleteCharAt(input.length() - 1);
                    System.out.print("\b \b");
                    System.out.flush();
                }
                continue;
            }

            // tab autocomplete
            if (c == 9 && input.length() > 0) {
                consecutiveTabsCount++;
                Matcher commandBeginningMatcher = COMMAND_BEGINNING_PATTERN.matcher(input);
                String commandBeginning = commandBeginningMatcher.find() ? commandBeginningMatcher.group(1) : null;
                if (commandBeginning != null) {
                    List<String> autocompleteOptions = Stream
                            .concat(BuiltinCommand.autocomplete(commandBeginning).stream(),
                                    Executable.autocomplete(commandBeginning).stream())
                            .distinct()
                            .sorted()
                            .toList();
                    switch (autocompleteOptions.size()) {
                        case 0 -> System.out.print(BELL_CHARACTER);
                        case 1 -> {
                            String autocompletedInput = autocompleteOptions.getFirst();
                            String newInputPortion = autocompletedInput
                                    .substring(commandBeginning.length())
                                    .concat(" ");
                            input.append(newInputPortion);
                            System.out.print(newInputPortion);
                        }
                        default -> {
                            String autocompletedInput = findLongestCommonPrefix(autocompleteOptions,
                                    commandBeginning);
                            if (autocompletedInput.length() > commandBeginning.length()) {
                                String newInputPortion = autocompletedInput.substring(commandBeginning.length());
                                input.append(newInputPortion);
                                System.out.print(newInputPortion);
                            } else {
                                if (consecutiveTabsCount == 1) {
                                    System.out.print(BELL_CHARACTER);
                                } else {
                                    System.out.println();
                                    System.out.println(String.join(" " + " ", autocompleteOptions));
                                    System.out.print("$ " + input);
                                }
                            }
                        }
                    }
                } else {
                    System.out.print(BELL_CHARACTER);
                }

                continue;
            }

            bout.write(c);
            String binaryRepresentation = Integer.toBinaryString(c);
            if (binaryRepresentation.length() == 8) {
                // two byte character
                if (binaryRepresentation.startsWith("110")) {
                    bout.write(System.in.read());
                }
                // three byte character
                else if (binaryRepresentation.startsWith("1110")) {
                    bout.write(System.in.read());
                    bout.write(System.in.read());
                }
                // four byte character
                else if (binaryRepresentation.startsWith("11110")) {
                    bout.write(System.in.read());
                    bout.write(System.in.read());
                    bout.write(System.in.read());
                }
            }

            if (c == 13 || c == 10) {
                input.append('\n');
                System.out.print("\r\n");
            } else {
                input.append(bout);
                System.out.print(bout);
            }

            if (c == 92) {
                isEscaped = !isEscaped;
            } else if (isEscaped) {
                isEscaped = false;
            }

            if (c != 9 && consecutiveTabsCount > 0) {
                consecutiveTabsCount = 0;
            }
        }
    }

    static String findLongestCommonPrefix(List<String> coll, String basePrefix) {
        String shortestEntry = coll.stream()
                .sorted((s1, s2) -> Integer.compare(s1.length(), s2.length()))
                .findFirst()
                .orElse(null);

        if (shortestEntry == null || shortestEntry.equals(basePrefix)) {
            return basePrefix;
        }

        for (int prefixLen = shortestEntry.length(); prefixLen > basePrefix.length(); prefixLen--) {
            String testPrefix = shortestEntry.substring(0, prefixLen);
            if (coll.stream().allMatch(entry -> entry.startsWith(testPrefix))) {
                return testPrefix;
            }
        }

        return basePrefix;
    }
}
