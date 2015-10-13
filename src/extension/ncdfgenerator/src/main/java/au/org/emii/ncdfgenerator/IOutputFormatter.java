package au.org.emii.ncdfgenerator;

import java.io.InputStream;
import java.io.OutputStream;

public interface IOutputFormatter {
    void prepare(OutputStream os);

    void write(String filename, InputStream is) throws Exception;

    void finish() throws Exception;
}

