package au.org.emii.gogoduck.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;

public class UserLog {
    private static final Logger logger = LoggerFactory.getLogger(UserLog.class);

    private FileWriter userLogFileWriter;

    UserLog() {
        this.userLogFileWriter = null;
    }

    UserLog(String logFile) {
        try {
            this.userLogFileWriter = new FileWriter(logFile);
        }
        catch (IOException e) {
            this.userLogFileWriter = null;
            logger.info(String.format("Exception while opening user log at '%s', continuing anyway", logFile));
        }
    }

    public void close() {
        try {
            if (null != userLogFileWriter) {
                userLogFileWriter.close();

            }
        }
        catch (IOException e) {
            logger.debug("Could not close user log file");
        }
    }

    public void log(String s) {
        if (null != userLogFileWriter) {
            try {
                userLogFileWriter.write(s);
                userLogFileWriter.write(System.lineSeparator());
            }
            catch (IOException e) {
                logger.debug("Exception while writing to user log");
            }
        }
    }
}
