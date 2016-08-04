package au.org.emii.gogoduck.worker;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import au.org.emii.gogoduck.exception.GoGoDuckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpIndexReader implements IndexReader {
    private static final Logger logger = LoggerFactory.getLogger(HttpIndexReader.class);

    protected String geoserver = null;
    private UserLog userLog = null;

    public HttpIndexReader(UserLog userLog, String geoserver) {
        this.userLog = userLog;
        this.geoserver = geoserver;
    }

    @Override
    public URIList getUriList(String profile, String timeField, String urlField, GoGoDuckSubsetParameters subset) throws GoGoDuckException {
        String timeCoverageStart = subset.get("TIME").start;
        String timeCoverageEnd = subset.get("TIME").end;

        URIList uriList = new URIList();

        try {
            String downloadUrl = String.format("%s/wfs", geoserver);
            String cqlFilter = String.format("%s >= %s AND %s <= %s",
                timeField, timeCoverageStart, timeField, timeCoverageEnd
            );

            Map<String, String> params = new HashMap<String, String>();
            params.put("typeName", profile);
            params.put("SERVICE", "WFS");
            params.put("outputFormat", "csv");
            params.put("REQUEST", "GetFeature");
            params.put("VERSION", "1.0.0");
            params.put("CQL_FILTER", cqlFilter);

            byte[] postDataBytes = encodeMapForPostRequest(params);

            URL url = new URL(downloadUrl);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);
            conn.getOutputStream().write(postDataBytes);

            InputStream inputStream = conn.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(inputStream));

            logger.info(String.format("Getting list of files from '%s'", downloadUrl));
            logger.debug(String.format("Parameters: '%s'", new String(postDataBytes)));
            String line = null;
            Integer i = 0;
            while ((line = dataInputStream.readLine()) != null) {
                if (i > 0) { // Skip first line - it's the headers
                    String[] lineParts = line.split(",");
                    uriList.add(new URI(lineParts[2]));
                }
                i++;
            }
        }
        catch (Exception e) {
            userLog.log("We could not obtain list of URLs, does the collection still exist?");
            throw new GoGoDuckException(String.format("Could not obtain list of URLs: '%s'", e.getMessage()));
        }

        return uriList;
    }

    private byte[] encodeMapForPostRequest(Map<String, String> params) {
        byte[] postDataBytes = null;
        try {
            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, String> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }
            postDataBytes = postData.toString().getBytes("UTF-8");
        }
        catch (Exception e) {
            logger.error(String.format("Error encoding parameters: '%s'", e.getMessage()));
        }

        return postDataBytes;
    }
}
