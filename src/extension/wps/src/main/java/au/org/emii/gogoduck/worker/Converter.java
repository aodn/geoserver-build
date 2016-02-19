package au.org.emii.gogoduck.worker;

import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Converter {

    public abstract void init();

    public abstract Path convert(Path outputFile) throws GoGoDuckException;

    public abstract String getMimeType();

    public abstract String getExtension();

    public static Converter newInstance(String filter) {
        Logger logger = LoggerFactory.getLogger(Converter.class);
        String thisPackage = Converter.class.getPackage().getName();
        String classToInstantiate = String.format("%sConverter", filter.toUpperCase());

        logger.debug(String.format("Trying class '%s.%s'", thisPackage, classToInstantiate));
        try {
            Class classz = Class.forName(String.format("%s.%s", thisPackage, classToInstantiate));
            Converter converter = (Converter) classz.newInstance();
            converter.init();
            logger.info(String.format("Using class '%s.%s'", thisPackage, classToInstantiate));
            return converter;
        }
        catch (Exception e) {
            logger.debug(String.format("Could not find class for '%s.%s'", thisPackage, classToInstantiate));
        }

        throw new GoGoDuckException(String.format("Error initializing class for filter '%s'", filter));
    }
}
