
package au.org.emii.ncdfgenerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import ucar.nc2.NetcdfFileWriteable;


class CreateWritable implements ICreateWritable {
    // NetcdfFileWriteable is not an abstraction over a stream!. instead it insists on being a file...
    // Makes resource handling tedious to support multithreading and http concurrency.

    private final String tmpDir;

    CreateWritable(String tmpDir) {
        this.tmpDir = tmpDir;
    }

    private String getFilename() {
        long threadId = Thread.currentThread().getId();
        return tmpDir + "/tmpfile" + threadId + ".nc";
    }

    public final NetcdfFileWriteable create() throws IOException {
        String filename = getFilename();
        Files.deleteIfExists(Paths.get(filename));
        return NetcdfFileWriteable.createNew(filename, false);
    }

    public final InputStream getStream() throws IOException {
        return new FileInputStream(getFilename());
    }
}


