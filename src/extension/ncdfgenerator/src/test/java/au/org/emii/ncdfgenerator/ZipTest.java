package au.org.emii.ncdfgenerator;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;

public class ZipTest {
    @Test
    public void aTest() throws FileNotFoundException, IOException {
        OutputStream os = new ByteArrayOutputStream();
        ZipOutputStream zipStream = new ZipOutputStream(os);
        zipStream.setLevel(ZipOutputStream.STORED);

        String filenameToUse = "whoot";
        zipStream.putNextEntry(new ZipEntry(filenameToUse));

        String data = "mydata...";
        InputStream is = new ByteArrayInputStream(data.getBytes());
        int bytesCopied = IOUtils.copy(is, zipStream);

        zipStream.close();
        assertEquals(bytesCopied, 9);
    }
}
