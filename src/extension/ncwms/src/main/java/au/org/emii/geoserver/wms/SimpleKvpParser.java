package au.org.emii.geoserver.wms;

import org.geoserver.ows.KvpParser;

public class SimpleKvpParser extends KvpParser {
    public SimpleKvpParser(String key) {
        super(key, String.class);
    }

    @Override
    public Object parse(String s) throws Exception {
        return s;
    }
}
