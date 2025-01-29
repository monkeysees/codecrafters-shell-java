import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum Redirect {
    INPUT("0>"),
    INPUT_APPEND("0>>"),
    OUTPUT("1>"),
    OUTPUT_APPEND("1>>"),
    ERROR("2>"),
    ERROR_APPEND("2>>");

    final String operator;

    static final Map<String, Redirect> OPERATOR_TO_REDIRECT = new HashMap<>();

    static final Set<Character> DESCRIPTORS = new HashSet<>();

    static {
        for (Redirect command : values()) {
            OPERATOR_TO_REDIRECT.put(command.operator, command);
        }
        DESCRIPTORS.add('0');
        DESCRIPTORS.add('1');
        DESCRIPTORS.add('2');
    }

    Redirect(String operator) {
        this.operator = operator;
    }

    public static Redirect fromString(String operator) {
        return OPERATOR_TO_REDIRECT.getOrDefault(operator, Redirect.OUTPUT);
    }
}
