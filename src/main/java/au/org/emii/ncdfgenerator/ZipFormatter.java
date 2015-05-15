package au.org.emii.ncdfgenerator;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// may want to add a report file etc...

public class ZipFormatter implements IOutputFormatter {

    private ZipOutputStream zipStream;

    public ZipFormatter() {
        this.zipStream = null;
    }

    public final void prepare(OutputStream os) {
        this.zipStream = new ZipOutputStream(os);
    }

    public final void write(String filename, InputStream is) throws Exception { // TODO throw as NcdfException?
        zipStream.putNextEntry(new ZipEntry(filename));
        IOUtils.copy(is, zipStream);
    }

    public final void finish() throws Exception {
        zipStream.close();
    }
}

