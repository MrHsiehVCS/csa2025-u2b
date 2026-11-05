package u2bpp;

import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.AssertionFailedError;

/*
 * Used by every Part*Tests class (@ExtendWith). Every test has a time limit (set in pom.xml), and
 * a test that runs past it is stopped. JUnit then just says "timed out"; this turns that into a
 * hint a student can act on. Not a test class itself, so it belongs to no part.
 */
class TimeLimitHint implements TestExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable thrown) throws Throwable {
        if (!(thrown instanceof TimeoutException)) {
            throw thrown;
        }
        AssertionFailedError hint = new AssertionFailedError(thrown.getMessage()
                + " and was stopped (a correct solution takes well under a second). Likely cause: an "
                + "infinite loop - a loop whose condition never becomes false, for example a while loop "
                + "that never changes the variable it checks (a missing i++ or count++), or a loop that "
                + "never reaches the line that ends it.");
        hint.initCause(thrown);
        throw hint;
    }
}
