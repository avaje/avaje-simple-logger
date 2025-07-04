package io.avaje.simplelogger.encoder;

import io.avaje.simplelogger.LoggerTestSuite;
import org.junit.jupiter.api.Disabled;
import org.slf4j.Logger;
import org.slf4j.event.Level;

@Disabled
public class AcceptanceTest extends LoggerTestSuite {

    @Override
    public Logger createLogger(ListAppendingOutputStream outputStream, Level level) {
        int logLevel = SimpleLoggerFactory.stringToLevel(level.toString());
        return new SimpleLogger(new PlainLogWriter(System.out, true), "TestSuiteLogger", "TestSuiteLogger", logLevel);
    }

    @Override
    public String extractMessage(String message) {
        return message
                .split("\n")[0]
                .split("- ")[1];
    }

    @Override
    public String extractExceptionMessage(String message) {
        String[] logLines = message.split("\n");

        if (logLines.length < 2) {
            return null;
        }
        String exceptionLine = logLines[1];
        return exceptionLine.split(": ")[1];
    }

    @Override
    public String extractExceptionType(String message) {
        String[] logLines = message.split("\n");

        if (logLines.length < 2) {
            return null;
        }
        String exceptionLine = logLines[1];
        return exceptionLine.split(": ")[0];
    }

}