import java.util.List;
import java.util.Map;

abstract class Program {
    String name;
    public Object inputRedirect;
    public Object inputRedirectAppend;
    public Object outputRedirect;
    public Object outputRedirectAppend;
    public Object errorRedirect;
    public Object errorRedirectAppend;

    abstract ExecutionResult execute(Shell shell, List<String> args);

    Program(String name) {
        this(name, null);
    }

    Program(Map<Redirect, ? extends Object> redirects) {
        this(null, redirects);
    }

    Program(String name, Map<Redirect, ? extends Object> redirects) {
        if (name != null) {
            this.name = name;
        }
        if (redirects != null) {
            inputRedirect = redirects.get(Redirect.INPUT);
            inputRedirectAppend = redirects.get(Redirect.INPUT_APPEND);
            outputRedirect = redirects.get(Redirect.OUTPUT);
            outputRedirectAppend = redirects.get(Redirect.OUTPUT_APPEND);
            errorRedirect = redirects.get(Redirect.ERROR);
            errorRedirectAppend = redirects.get(Redirect.ERROR_APPEND);
        }
    }
}
