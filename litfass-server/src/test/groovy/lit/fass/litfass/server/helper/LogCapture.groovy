package lit.fass.litfass.server.helper

import org.hamcrest.Matcher
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

import static org.junit.Assert.assertThat

/**
 * This class is mainly taken from https://github.com/spring-projects/spring-boot/blob/master/spring-boot-project/spring-boot-tools/spring-boot-test-support/src/main/java/org/springframework/boot/testsupport/rule/OutputCapture.java.
 *
 * @author Michael Mair
 */
class LogCapture implements TestRule {

    private LogCaptureStream captureOut
    private LogCaptureStream captureErr
    private ByteArrayOutputStream copy

    private List<Matcher<? super String>> matchers = []

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                captureOutput()
                try {
                    base.evaluate()
                }
                finally {
                    try {
                        if (!LogCapture.this.matchers.isEmpty()) {
                            String output = LogCapture.this.toString()
                            assertThat(output, allOf(LogCapture.this.matchers))
                        }
                    }
                    finally {
                        releaseOutput()
                    }
                }
            }
        };
    }

    protected void captureOutput() {
        this.copy = new ByteArrayOutputStream()
        this.captureOut = new LogCaptureStream(System.out, this.copy)
        this.captureErr = new LogCaptureStream(System.err, this.copy)
        System.setOut(new PrintStream(this.captureOut))
        System.setErr(new PrintStream(this.captureErr))
    }

    protected void releaseOutput() {
        System.setOut(this.captureOut.getOriginal())
        System.setErr(this.captureErr.getOriginal())
        this.copy = null
    }

    public void flush() {
        try {
            this.captureOut.flush()
            this.captureErr.flush()
        }
        catch (IOException ex) {
            // ignore
        }
    }

    @Override
    public String toString() {
        flush();
        return this.copy.toString()
    }

    private static class LogCaptureStream extends OutputStream {

        private final PrintStream original

        private final OutputStream copy

        LogCaptureStream(PrintStream original, OutputStream copy) {
            this.original = original
            this.copy = copy
        }

        @Override
        public void write(int b) throws IOException {
            this.copy.write(b)
            this.original.write(b)
            this.original.flush()
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length)
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.copy.write(b, off, len)
            this.original.write(b, off, len)
        }

        public PrintStream getOriginal() {
            return this.original
        }

        @Override
        public void flush() throws IOException {
            this.copy.flush()
            this.original.flush()
        }

    }

}
