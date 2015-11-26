package au.org.emii.notifier;

import java.io.IOException;
import java.net.URL;

public interface HttpNotifier {

    void notify(URL notificationUrl, URL wpsServerUrl, String uuid, boolean successful, String notificationParams) throws IOException;
}
