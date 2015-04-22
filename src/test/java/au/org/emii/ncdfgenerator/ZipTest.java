package au.org.emii.ncdfgenerator;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

public class ZipTest
{
    @Test
    public void aTest() throws FileNotFoundException, IOException
    {
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
