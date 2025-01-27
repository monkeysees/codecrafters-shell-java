import java.util.HashMap;
import java.util.Map;

public enum RedirectDescriptor {
    INPUT('0'),
    OUTPUT('1'),
    ERROR('2');

    final Character code;

    static final Map<Character, RedirectDescriptor> CODE_TO_DESCRIPTOR = new HashMap<>();

    static {
        for (RedirectDescriptor command : values()) {
            CODE_TO_DESCRIPTOR.put(command.code, command);
        }
    }

    RedirectDescriptor(Character code) {
        this.code = code;
    }

    public static RedirectDescriptor fromCode(Character code) {
        RedirectDescriptor command = CODE_TO_DESCRIPTOR.get(code);
        if (command == null) {
            return RedirectDescriptor.OUTPUT;
        } else {
            return command;
        }
    }

    public static boolean isRedirectDescriptor(Character code) {
        return CODE_TO_DESCRIPTOR.containsKey(code);
    }
}
