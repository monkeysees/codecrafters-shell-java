import java.util.List;

interface Program {
    ExecutionResult execute(Shell shell, List<String> args);
}
