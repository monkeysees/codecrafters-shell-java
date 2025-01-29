import java.util.HashMap;
import java.util.Map;

public enum RedirectType {
    OUTPUT("1>"),
    OUTPUT_APPEND("1>>"),
    ERROR("2>"),
    ERROR_APPEND("2>>");

    final String operator;

    static final Map<String, RedirectType> OPERATOR_TO_REDIRECT = new HashMap<>();

    static {
        for (RedirectType command : values()) {
            OPERATOR_TO_REDIRECT.put(command.operator, command);
        }
        OPERATOR_TO_REDIRECT.put(">", OUTPUT);
        OPERATOR_TO_REDIRECT.put(">>", OUTPUT_APPEND);
    }

    RedirectType(String operator) {
        this.operator = operator;
    }

    public static RedirectType fromString(String operator) {
        return OPERATOR_TO_REDIRECT.getOrDefault(operator, RedirectType.OUTPUT);
    }

    public static Boolean isAppend(RedirectType redirectType) {
        return redirectType == RedirectType.OUTPUT_APPEND || redirectType == RedirectType.ERROR_APPEND;
    }
}
