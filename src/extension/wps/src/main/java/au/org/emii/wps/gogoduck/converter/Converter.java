package au.org.emii.wps.gogoduck.converter;

import java.nio.file.Path;

import au.org.emii.wps.gogoduck.exception.GoGoDuckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Converter {

    public abstract void convert(Path outputFile, Path convertedFile) throws GoGoDuckException;

    public abstract String getMimeType();

    public abstract String getExtension();

    public static Converter newInstance(String format) {
        Logger logger = LoggerFactory.getLogger(Converter.class);

        if (format == null) {
            return new NetCDFConverter();
        } else if (format.equals(TextCsvConverter.MIME_TYPE)) {
            return new TextCsvConverter();
        } else if (format.equals(NetCDFConverter.MIME_TYPE)) {
            return new NetCDFConverter();
        } else {
            String message = String.format("Invalid output format requested: %s", format);
            logger.error(message);
            throw new GoGoDuckException(message);
        }
    }

}
