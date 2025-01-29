import java.io.File;
import java.util.List;
import java.util.Map;

abstract class Program implements Printer {
    String name;
    public File outputRedirect;
    public File outputRedirectAppend;
    public File errorRedirect;
    public File errorRedirectAppend;

    Program(String name) {
        this(name, null);
    }

    Program(Map<RedirectType, File> redirects) {
        this(null, redirects);
    }

    Program(String name, Map<RedirectType, File> redirects) {
        if (name != null) {
            this.name = name;
        }
        if (redirects != null) {
            outputRedirect = redirects.get(RedirectType.OUTPUT);
            outputRedirectAppend = redirects.get(RedirectType.OUTPUT_APPEND);
            errorRedirect = redirects.get(RedirectType.ERROR);
            errorRedirectAppend = redirects.get(RedirectType.ERROR_APPEND);
        }
    }

    abstract ExecutionResult execute(Shell shell, List<String> args);

    public Redirect getOutputRedirect() {
        return (outputRedirectAppend != null)
                ? new Redirect(outputRedirectAppend, RedirectType.OUTPUT_APPEND)
                : new Redirect(outputRedirect, RedirectType.OUTPUT);
    }

    public Redirect getErrorRedirect() {
        return (errorRedirectAppend != null)
                ? new Redirect(errorRedirectAppend, RedirectType.ERROR_APPEND)
                : new Redirect(errorRedirect, RedirectType.ERROR);
    }
}
