import java.util.List;
import java.util.Map;

abstract class Program {
    String name;
    public Object inputRedirect;
    public Object outputRedirect;
    public Object errorRedirect;

    abstract ExecutionResult execute(Shell shell, List<String> args);

    Program(String name) {
        this(name, null);
    }

    Program(Map<RedirectDescriptor, ? extends Object> redirects) {
        this(null, redirects);
    }

    Program(String name, Map<RedirectDescriptor, ? extends Object> redirects) {
        if (name != null) {
            this.name = name;
        }
        if (redirects != null) {
            inputRedirect = redirects.get(RedirectDescriptor.INPUT);
            outputRedirect = redirects.get(RedirectDescriptor.OUTPUT);
            errorRedirect = redirects.get(RedirectDescriptor.ERROR);
        }
    }
}
