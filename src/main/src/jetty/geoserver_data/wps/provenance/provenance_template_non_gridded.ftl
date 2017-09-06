<?xml version="1.0" encoding="UTF-8"?>

<prov:document
        xmlns:prov="http://www.w3.org/ns/prov#"
        xmlns:gnprov="http://geonetwork-opensource.org/prov-xml"
        xmlns:aodnprov="http://geonetwork-opensource.org/aodn/prov-xml"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:dct="http://purl.org/dc/terms/"
        xmlns:msr="http://standards.iso.org/iso/19115/-3/msr/1.0"
        xmlns:gex="http://standards.iso.org/iso/19115/-3/gex/1.0"
        xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
        xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0"
        xmlns:gml="http://www.opengis.net/gml/3.2"
        xmlns:wps="http://www.opengis.net/wps/1.0.0"
        xsi:schemaLocation="http://www.w3.org/ns/prov# http://www.w3.org/ns/prov.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2008/02/11/dc.xsd http://purl.org/dc/terms/ http://dublincore.org/schemas/xmls/qdc/2008/02/11/dcterms.xsd http://standards.iso.org/iso/19115/-3/gex/1.0 http://standards.iso.org/iso/19115/-3/gex/1.0/gex.xsd http://standards.iso.org/iso/19115/-3/msr/1.0 http://standards.iso.org/iso/19115/-3/msr/1.0/msr.xsd http://standards.iso.org/iso/19115/-3/cit/1.0 http://standards.iso.org/iso/19115/-3/cit/1.0/cit.xsd  http://standards.iso.org/iso/19115/-3/gco/1.0 http://standards.iso.org/iso/19115/-3/gco/1.0/gco.xsd http://geonetwork-opensource.org/prov-xml http://geonetwork-opensource.org/prov-xml/gnprov.xsd http://geonetwork-opensource.org/aodn/prov-xml http://geonetwork-opensource.org/aodn/prov-xml/aodnprov.xsd http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">

    <prov:entity prov:id="WPS-Aggregator Dataset UID">
        <!-- location is the (UID) URL for this zipped output file. Note entities such as this and others
             below must persist for the provenance to be of value -->
        <prov:location>${aggregatedDataUrl?xml}</prov:location>
        <!-- prov:type is Mandatory -->
        <prov:type codeList="codeListLocation#type" codeListValue="output">output</prov:type>
    </prov:entity>

    <prov:activity prov:id="WPS-NonGridded-Aggregator-Service-Job:${jobId}">
        <!-- startTime is the time the service was initiated -->
        <prov:startTime>${startTime}</prov:startTime>
        <!-- endTime is unknown at this document creation -->
        <prov:endTime>${endTime}</prov:endTime>
        <dc:description>A general description of the service</dc:description>
    </prov:activity>

    <!-- WPSQuery is a prov:entity with a wps:Data element in it. A user provides this entity to constrain
         the search of the WPS -->
    <prov:entity prov:id="WPSQuery">
        <aodnprov:wpsQuery>
            <wps:Data>
                <LiteralData>${wpsQuery?xml}</LiteralData>
            </wps:Data>
        </aodnprov:wpsQuery>
        <prov:type codeList="codeListLocation#type" codeListValue="ecqlFilter">ecqlFilter</prov:type>
    </prov:entity>

    <!-- Layer name used to lookup netcdf generation template to use -->
    <prov:entity prov:id="layerName">
        <!-- location is the name/URL of the selected Layer-->
        <prov:location>${layerName}</prov:location>
        <!-- prov:type is Mandatory -->
        <prov:type codeList="codeListLocation#type" codeListValue="inputParameter">inputParameter</prov:type>
    </prov:entity>

    <prov:entity prov:id="outputAggregationSettings">
        <!-- location is the URL of the NetCDF Aggregation settings file for this WPS job. -->
        <prov:location>https://github.com/aodn/geoserver-config/tree/production/${settingsPath}</prov:location>
        <!-- prov:type is Mandatory -->
        <prov:type codeList="codeListLocation#type" codeListValue="outputConfiguration">outputConfiguration</prov:type>
    </prov:entity>

    <prov:entity prov:id="sourceData">
        <!-- location is the URL of the metadata record for the data collection operated on by this WPS job-->
        <prov:location>${sourceMetadataUrl}</prov:location>
        <!-- prov:type is Mandatory -->
        <prov:type codeList="codeListLocation#type" codeListValue="inputData">inputData</prov:type>
    </prov:entity>

    <!-- Agents & Actors - the people, organization and software involved in WPS execution  -->

    <prov:softwareAgent prov:id="JavaCode">
        <!-- location is a github page describing the Java Code -->
        <prov:location>https://github.com/aodn/geoserver-build/blob/master/src/extension/wps/doc/NCDFGENERATOR_README.md</prov:location>
    </prov:softwareAgent>

    <!-- Associations and softwareSystem used  -->

    <prov:wasAssociatedWith>
        <prov:activity prov:ref="WPS-NonGridded-Aggregator-Service-Job:${jobId}"/>
        <prov:agent prov:ref="JavaCode"/>
        <prov:role codeList="codeListLocation#type" codeListValue="softwareSystem">softwareSystem</prov:role>
    </prov:wasAssociatedWith>

    <prov:used>
        <prov:activity prov:ref="WPS-NonGridded-Aggregator-Service-Job:${jobId}"/>
        <prov:entity prov:ref="WPSQuery"/>
        <prov:role codeList="codeListLocation#type" codeListValue="ecqLFilter">ecqlFilter</prov:role>
    </prov:used>

    <prov:used>
        <prov:activity prov:ref="WPS-NonGridded-Aggregator-Service-Job:${jobId}"/>
        <prov:entity prov:ref="layerName"/>
        <prov:role codeList="codeListLocation#type" codeListValue="inputParameter">inputParameter</prov:role>
    </prov:used>

    <prov:used>
        <prov:activity prov:ref="WPS-NonGridded-Aggregator-Service-Job:${jobId}"/>
        <prov:entity prov:ref="outputAggregationSettings"/>
        <prov:role codeList="codeListLocation#type" codeListValue="outputConfiguration">outputConfiguration</prov:role>
    </prov:used>

    <prov:wasGeneratedBy>
        <prov:entity prov:ref="WPS-Aggregator Dataset UID"/>
        <prov:activity prov:ref="WPS-NonGridded-Aggregator-Service-Job:${jobId}"/>
        <prov:time>${endTime}</prov:time>
    </prov:wasGeneratedBy>

    <prov:wasDerivedFrom>
        <prov:generatedEntity prov:ref="WPS-Aggregator Dataset UID"/>
        <prov:usedEntity prov:ref="sourceData"/>
        <prov:time>${endTime}</prov:time>
    </prov:wasDerivedFrom>

    <!-- Document metadata: id, title, description, coverage, keywords (subject), date created -->

    <prov:other>
        <dc:identifier>${jobId}</dc:identifier>
        <dc:title>Provenance document describing a non gridded WPS result</dc:title>
        <dc:description>This non gridded WPS used a ecqlFilter and a layer definition to produce a zip file of NetCDF output</dc:description>
        <dc:subject>WPS</dc:subject>
        <dct:created>${.now?iso_utc}</dct:created>
    </prov:other>

</prov:document>
