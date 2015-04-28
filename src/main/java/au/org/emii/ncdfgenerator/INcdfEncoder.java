
package au.org.emii.ncdfgenerator;

import java.io.InputStream;

public interface INcdfEncoder {
    void prepare() throws Exception;

    InputStream get() throws Exception;
}

