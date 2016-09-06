package au.org.emii.gogoduck.worker;

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class URIList extends LinkedHashSet<URI> {
    private double totalFileSize;

    public URI first() {
        Iterator<URI> iterator = iterator();
        return iterator.hasNext()?iterator.next():null;
    }

    public double getTotalFileSize() {
        return totalFileSize;
    }

    public void setTotalFileSize(double totalFileSize) {
        this.totalFileSize = totalFileSize;
    }
}
