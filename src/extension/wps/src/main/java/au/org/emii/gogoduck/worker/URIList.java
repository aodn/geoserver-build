package au.org.emii.gogoduck.worker;

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class URIList extends LinkedHashSet<URI> {
    public URI first() {
        Iterator<URI> iterator = iterator();
        return iterator.hasNext()?iterator.next():null;
    }
}
