import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Input {
    public String command;
    public List<String> args;
    public Map<RedirectDescriptor, ProcessBuilder.Redirect> redirects;

    Input(String command, List<String> args, Map<RedirectDescriptor, ProcessBuilder.Redirect> redirects) {
        this.command = command;
        this.args = args;
        this.redirects = redirects;
    }

    public static Input fromString(String s) {
        List<String> parsedCommandAndArgs = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        HashMap<RedirectDescriptor, ProcessBuilder.Redirect> redirects = new HashMap<>();

        Boolean insideSingleQuotes = false;
        Boolean insideDoubleQuotes = false;
        Boolean isBackslashed = false;
        Character potentialRedirectDescriptor = null;
        Boolean isRedirectOperator = false;
        StringBuilder redirectArg = null;

        Set<Character> specialDoubleQuotesCharacters = Set.of('\\', '$', '"', '\n');

        for (Character c : s.toCharArray()) {
            if (c == '>' &&
                    potentialRedirectDescriptor == null &&
                    currentArg.length() == 0 &&
                    !insideSingleQuotes &&
                    !insideDoubleQuotes) {
                potentialRedirectDescriptor = '1';
            }

            if (potentialRedirectDescriptor != null && redirectArg == null) {
                if (isRedirectOperator) {
                    if (!Character.isWhitespace(c)) {
                        currentArg.append(potentialRedirectDescriptor);
                        currentArg.append(">");
                        potentialRedirectDescriptor = null;
                        isRedirectOperator = false;
                        redirectArg = null;
                        continue;
                    } else {
                        redirectArg = new StringBuilder();
                        continue;
                    }
                }

                if (c == '>') {
                    isRedirectOperator = true;
                    continue;
                } else {
                    currentArg.append(potentialRedirectDescriptor);
                    potentialRedirectDescriptor = null;
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

            if (RedirectDescriptor.isRedirectDescriptor(c) &&
                    currentArg.length() == 0 &&
                    !insideSingleQuotes &&
                    !insideDoubleQuotes) {
                potentialRedirectDescriptor = c;
                continue;
            }

            if (Character.isWhitespace(c) && !insideSingleQuotes && !insideDoubleQuotes) {
                if (redirectArg != null) {
                    if (isRedirectOperator && potentialRedirectDescriptor != null) {
                        File redirectFile = new File(redirectArg.toString());
                        if (redirectFile.isFile() &&
                                (redirectFile.exists() || redirectFile.getParentFile().exists())) {
                            redirects.put(
                                    RedirectDescriptor.fromCode(potentialRedirectDescriptor),
                                    ProcessBuilder.Redirect.to(redirectFile));
                        } else {
                            System.err.println(String.format("No such file or directory: %s", redirectFile.toString()));
                        }
                    }
                    potentialRedirectDescriptor = null;
                    isRedirectOperator = false;
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
            if (isRedirectOperator && potentialRedirectDescriptor != null) {
                File redirectFile = new File(redirectArg.toString());
                File redirectParentFile = redirectFile.getParentFile();
                if (redirectFile.exists() || (redirectParentFile != null && redirectParentFile.exists())) {
                    redirects.put(
                            RedirectDescriptor.fromCode(potentialRedirectDescriptor),
                            ProcessBuilder.Redirect.to(redirectFile));
                } else {
                    System.err.println(String.format("No such file or directory: %s", redirectFile.toString()));
                }
            }
        } else {
            if (potentialRedirectDescriptor != null) {
                currentArg.append(potentialRedirectDescriptor);
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
