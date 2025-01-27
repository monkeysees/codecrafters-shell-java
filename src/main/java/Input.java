import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Input {
    public String command;
    public List<String> args;
    public Map<Redirect, ProcessBuilder.Redirect> redirects;

    Input(String command, List<String> args, Map<Redirect, ProcessBuilder.Redirect> redirects) {
        this.command = command;
        this.args = args;
        this.redirects = redirects;
    }

    public static Input fromString(String s) {
        List<String> parsedCommandAndArgs = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        HashMap<Redirect, ProcessBuilder.Redirect> redirects = new HashMap<>();

        Boolean insideSingleQuotes = false;
        Boolean insideDoubleQuotes = false;
        Boolean isBackslashed = false;
        Character potentialRedirect = null;
        String potentialRedirectOperator = null;
        StringBuilder redirectArg = null;

        Set<Character> specialDoubleQuotesCharacters = Set.of('\\', '$', '"', '\n');

        for (Character c : s.toCharArray()) {
            if (c == '>' &&
                    potentialRedirect == null &&
                    currentArg.length() == 0 &&
                    !insideSingleQuotes &&
                    !insideDoubleQuotes) {
                potentialRedirect = '1';
            }

            if (potentialRedirect != null && redirectArg == null) {
                if (potentialRedirectOperator != null) {
                    if (c == '>' && potentialRedirectOperator.equals(">")) {
                        potentialRedirectOperator = ">>";
                        continue;
                    } else if (!Character.isWhitespace(c)) {
                        currentArg.append(potentialRedirect);
                        currentArg.append(">");
                        potentialRedirect = null;
                        potentialRedirectOperator = null;
                        redirectArg = null;
                        continue;
                    } else {
                        redirectArg = new StringBuilder();
                        continue;
                    }
                }

                if (c == '>') {
                    potentialRedirectOperator = ">";
                    continue;
                } else {
                    currentArg.append(potentialRedirect);
                    potentialRedirect = null;
                }
            }

            if (isBackslashed) {
                if (insideDoubleQuotes && !specialDoubleQuotesCharacters.contains(c)) {
                    if (redirectArg != null) {
                        redirectArg.append('\\');
                    } else {
                        currentArg.append('\\');
                    }
                }
                if (redirectArg != null) {
                    redirectArg.append(c);
                } else {
                    currentArg.append(c);
                }
                isBackslashed = false;
                continue;
            }

            if (c == '\\' && !insideSingleQuotes) {
                isBackslashed = true;
                continue;
            }

            if (c == '\'' && !insideDoubleQuotes) {
                insideSingleQuotes = !insideSingleQuotes;
                continue;
            }

            if (c == '\"' && !insideSingleQuotes) {
                insideDoubleQuotes = !insideDoubleQuotes;
                continue;
            }

            if (Redirect.isDescriptor(c) &&
                    currentArg.length() == 0 &&
                    !insideSingleQuotes &&
                    !insideDoubleQuotes) {
                potentialRedirect = c;
                continue;
            }

            if (Character.isWhitespace(c) && !insideSingleQuotes && !insideDoubleQuotes) {
                if (redirectArg != null) {
                    if (potentialRedirectOperator != null && potentialRedirect != null) {
                        File redirectFile = new File(redirectArg.toString());
                        if (redirectFile.isFile() &&
                                (redirectFile.exists() || redirectFile.getParentFile().exists())) {
                            redirects.put(
                                    Redirect.fromDescriptorAndOperator(potentialRedirect, potentialRedirectOperator),
                                    (potentialRedirectOperator.equals(">"))
                                            ? ProcessBuilder.Redirect.to(redirectFile)
                                            : ProcessBuilder.Redirect.appendTo(redirectFile));
                        } else {
                            System.err.println(String.format("No such file or directory: %s", redirectFile.toString()));
                        }
                    }
                    potentialRedirect = null;
                    potentialRedirectOperator = null;
                    redirectArg = null;
                } else if (currentArg.length() > 0) {
                    parsedCommandAndArgs.add(currentArg.toString());
                    currentArg = new StringBuilder();
                }
                continue;
            }

            if (redirectArg != null) {
                redirectArg.append(c);
            } else {
                currentArg.append(c);
            }
        }
        if (redirectArg != null) {
            if (potentialRedirectOperator != null && potentialRedirect != null) {
                File redirectFile = new File(redirectArg.toString());
                File redirectParentFile = redirectFile.getParentFile();
                if ((redirectParentFile != null && redirectParentFile.exists())) {
                    if (!redirectFile.exists()) {
                        try {
                            redirectFile.createNewFile();
                        } catch (IOException e) {
                            System.err.println(String.format("Couldn't create the file to redirect to: %s",
                                    redirectFile.toString()));
                        }
                    }
                    redirects.put(
                            Redirect.fromDescriptorAndOperator(potentialRedirect, potentialRedirectOperator),
                            (potentialRedirectOperator.equals(">"))
                                    ? ProcessBuilder.Redirect.to(redirectFile)
                                    : ProcessBuilder.Redirect.appendTo(redirectFile));
                } else {
                    System.err.println(String.format("No such file or directory: %s", redirectFile.toString()));
                }
            }
        } else {
            if (potentialRedirect != null) {
                currentArg.append(potentialRedirect);
            }
            if (currentArg.length() > 0) {
                parsedCommandAndArgs.add(currentArg.toString());
            }
        }

        String command = parsedCommandAndArgs.get(0);
        List<String> commandArgs = parsedCommandAndArgs.size() > 1
                ? parsedCommandAndArgs.subList(1, parsedCommandAndArgs.size())
                : new ArrayList<>();

        return new Input(command, commandArgs, redirects);
    }
}
