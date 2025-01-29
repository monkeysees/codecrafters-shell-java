import java.io.File;

public class Redirect {
    public File file;
    public RedirectType type;

    Redirect(File file, RedirectType type) {
        this.file = file;
        this.type = type;
    };
}
