package au.org.emii.wps.process;

import org.apache.commons.io.IOUtils;
import org.geoserver.wps.process.AbstractRawData;

import java.io.IOException;
import java.io.InputStream;

public class StringRawData extends AbstractRawData {

    private String data;

    public StringRawData(String data, String mimeType, String extension) {
        super(mimeType, extension);
        this.data = data;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return IOUtils.toInputStream(data);
    }

    @Override
    public String toString() {
        return "StringRawData [data=" + data + ", mimeType=" + mimeType + ", extension="
            + extension + "]";
    }

    public String getData() {
        return data;
    }

}
