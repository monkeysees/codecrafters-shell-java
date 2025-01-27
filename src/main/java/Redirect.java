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

    final String name;

    static final Map<String, Redirect> NAME_TO_REDIRECT = new HashMap<>();

    static final Set<Character> DESCRIPTORS = new HashSet<>();

    static {
        for (Redirect command : values()) {
            NAME_TO_REDIRECT.put(command.name, command);
        }
        DESCRIPTORS.add('0');
        DESCRIPTORS.add('1');
        DESCRIPTORS.add('2');
    }

    Redirect(String name) {
        this.name = name;
    }

    public static Redirect fromDescriptorAndOperator(Character descriptor, String operator) {
        return NAME_TO_REDIRECT.getOrDefault(descriptor + operator, Redirect.OUTPUT);
    }

    public static boolean isDescriptor(Character code) {
        return DESCRIPTORS.contains(code);
    }
}
