# netCDF Download Generator

This plugin returns a zipped set of netCDF files from information stored in the database

### Mimicking 'ncdfgenerator' download request from the portal


  1. Create an xml file that will contain parameters to the WPS ncdfgenerator service.
The example below needs setting up for your dataset:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<wps:Execute xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:ows="http://www.opengis.net/ows/1.1"
    xmlns:xlink="http://www.w3.org/1999/xlink" service="WPS" version="1.0.0">
    <ows:Identifier>gs:NetcdfOutput</ows:Identifier>
    <wps:DataInputs>
        <wps:Input>
            <ows:Identifier>typeName</ows:Identifier>
            <wps:Data>
                <wps:LiteralData>[workspace]:[layername]</wps:LiteralData>
            </wps:Data>
        </wps:Input>
        <wps:Input>
            <ows:Identifier>cqlFilter</ows:Identifier>
            <wps:Data>
                <wps:LiteralData><![CDATA[TIME >= '2009-01-13T23:00:00Z' AND TIME <= '2009-02-01T00:00:00Z']]></wps:LiteralData>
            </wps:Data>
        </wps:Input>
    </wps:DataInputs>
    <wps:ResponseForm>
        <wps:ResponseDocument lineage="false" status="true" storeExecuteResponse="true">
            <wps:Output asReference="true" mimeType="application/zip">
                <ows:Identifier>result</ows:Identifier>
            </wps:Output>
        </wps:ResponseDocument>
    </wps:ResponseForm>
</wps:Execute>
```

   2. Excecute using curl command with the above xml file:
   
   
   ```curl --data @[path-to/XML-filename].xml --header "Expect:" --header "Content-Type: application/xml" http://[theserver]/wps```
