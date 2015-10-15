package au.org.emii.ncdfgenerator;

import java.io.InputStream;

public interface IOutputFormatter {

    void write(String filename, InputStream is) throws Exception;
    void close() throws Exception;
}

