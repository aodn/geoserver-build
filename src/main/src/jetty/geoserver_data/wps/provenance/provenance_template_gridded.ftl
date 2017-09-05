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
        xsi:schemaLocation="http://www.w3.org/ns/prov# http://www.w3.org/ns/prov.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2008/02/11/dc.xsd http://purl.org/dc/terms/ http://dublincore.org/schemas/xmls/qdc/2008/02/11/dcterms.xsd http://standards.iso.org/iso/19115/-3/gex/1.0 http://standards.iso.org/iso/19115/-3/gex/1.0/gex.xsd http://standards.iso.org/iso/19115/-3/msr/1.0 http://standards.iso.org/iso/19115/-3/msr/1.0/msr.xsd http://standards.iso.org/iso/19115/-3/cit/1.0 http://standards.iso.org/iso/19115/-3/cit/1.0/cit.xsd  http://standards.iso.org/iso/19115/-3/gco/1.0 http://standards.iso.org/iso/19115/-3/gco/1.0/gco.xsd http://geonetwork-opensource.org/prov-xml http://geonetwork-opensource.org/prov-xml/gnprov.xsd http://geonetwork-opensource.org/aodn/prov-xml http://geonetwork-opensource.org/aodn/prov-xml/aodnprov.xsd">


    <prov:entity prov:id="WPS-Aggregator Dataset UID">
        <!-- location is the (UID) URL for this output file. Note entities such as this and others
             below must persist for the provenance to be of value -->
        <prov:location>${downloadUrl?xml}</prov:location>
        <!-- prov:type is Mandatory -->
        <prov:type codeList="codeListLocation#type" codeListValue="output">output</prov:type>
    </prov:entity>

    <prov:activity prov:id="WPS-Gridded-Aggregator-Service-Job:${jobId}">
        <!-- startTime is the time the service was initiated -->
        <prov:startTime>${startTime}</prov:startTime>
        <!-- endTime is unknown at this document creation -->
        <prov:endTime>${endTime}</prov:endTime>
        <dc:description>A general description of the service</dc:description>
    </prov:activity>

    <!-- timeExtent is prov:entity with a gex:EX_TemporalExtent element in it. A user provides this entity to constrain
         the temporal extents of the WPS -->
    <prov:entity prov:id="timeExtent">
        <gnprov:temporalExtent>
            <gex:EX_Extent>
                <gex:temporalElement>
                    <gex:EX_TemporalExtent>
                        <gex:extent>
                            <#--<!-- gmd:id needs to be unique to this document, gml:TimePeriod can be
                                 quite sophisticated - see for example
                                 https://geo-ide.noaa.gov/wiki/index.php?title=TimePeriod &ndash;&gt;-->
                            <gml:TimePeriod gml:id="A1234">
                                <gml:beginPosition>${parameters.timeRange.start}</gml:beginPosition>
                                <gml:endPosition>${parameters.timeRange.end}</gml:endPosition>
                            </gml:TimePeriod>
                        </gex:extent>
                    </gex:EX_TemporalExtent>
                </gex:temporalElement>
            </gex:EX_Extent>
        </gnprov:temporalExtent>
        <!-- prov:type is Mandatory -->
        <prov:type codeList="codeListLocation#type" codeListValue="timeExtent">timeExtent</prov:type>
    </prov:entity>

    <!-- spatialExtent is prov:entity with a gex:EX_GeographicBoundingBox element. A user provides this entity to constrain
         the spatial extents of the WPS -->

<#assign eastBL = parameters.bbox.lonMin>
<#assign westBL = parameters.bbox.lonMax>
<#assign southBL = parameters.bbox.latMin>
<#assign northBL = parameters.bbox.latMax>

    <prov:entity prov:id="spatialExtent">
        <aodnprov:boundingBox>
            <gex:EX_Extent>
                <gex:geographicElement>
                    <gex:EX_GeographicBoundingBox>
                        <gex:westBoundLongitude>
                            <gco:Decimal>${westBL}</gco:Decimal>
                        </gex:westBoundLongitude>
                        <gex:eastBoundLongitude>
                            <gco:Decimal>${eastBL}</gco:Decimal>
                        </gex:eastBoundLongitude>
                        <gex:southBoundLatitude>
                            <gco:Decimal>${southBL}</gco:Decimal>
                        </gex:southBoundLatitude>
                        <gex:northBoundLatitude>
                            <gco:Decimal>${northBL}</gco:Decimal>
                        </gex:northBoundLatitude>
                    </gex:EX_GeographicBoundingBox>
                </gex:geographicElement>
            </gex:EX_Extent>
        </aodnprov:boundingBox>
        <!-- prov:type is Mandatory -->
        <prov:type codeList="codeListLocation#type" codeListValue="boundingBox">boundingBox</prov:type> <!-- helper provides this -->
    </prov:entity>

    <!-- Layer name used to query database for list of files included in temporal range -->
    <prov:entity prov:id="layerName">
        <!-- location is the name/URL of the selected Layer -->
        <prov:location>${layer}</prov:location>
        <!-- prov:type is Mandatory -->
        <prov:type codeList="codeListLocation#type" codeListValue="inputParameter">inputParameter</prov:type>
    </prov:entity>

    <prov:entity prov:id="outputAggregationSettings">
        <!-- location is the URL of the gridded NetCDF Aggregation settings (index reader, download and aggregation templates) file for this WPS job-->
        <prov:location>https://github.com/aodn/geoserver-config/tree/production/wps/gogoduck.xml</prov:location>
        <#if settingsPath?has_content>
        <prov:location>https://github.com/aodn/geoserver-config/tree/production/${settingsPath}</prov:location>
        </#if>
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
        <!-- location is a metadata record describing the Java Code -->
        <prov:location>https://github.com/aodn/geoserver-build/blob/master/src/extension/wps/doc/GOGODUCK_README.md</prov:location>
    </prov:softwareAgent>

    <!-- Associations and softwareSystem used  -->
    <prov:wasAssociatedWith>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:${jobId}"/>
        <prov:agent prov:ref="JavaCode"/>
        <prov:role codeList="codeListLocation#type" codeListValue="softwareSystem">softwareSystem</prov:role>
    </prov:wasAssociatedWith>

    <prov:used>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:${jobId}"/>
        <prov:entity prov:ref="timeExtent"/>
        <prov:role codeList="codeListLocation#type" codeListValue="timeExtent">timeExtent</prov:role>
    </prov:used>

    <prov:used>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:${jobId}"/>
        <prov:entity prov:ref="spatialExtent"/>
        <prov:role codeList="codeListLocation#type" codeListValue="boundingBox">boundingBox</prov:role>
    </prov:used>

    <prov:used>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:${jobId}"/>
        <prov:entity prov:ref="layerName"/>
        <prov:role codeList="codeListLocation#type" codeListValue="inputParameter">inputParameter</prov:role>
    </prov:used>

    <prov:used>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:${jobId}"/>
        <prov:entity prov:ref="outputAggregationSettings"/>
        <prov:role codeList="codeListLocation#type" codeListValue="outputConfiguration">outputConfiguration</prov:role>
    </prov:used>

    <prov:wasGeneratedBy>
        <prov:entity prov:ref="WPS-Aggregator Dataset UID"/>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:${jobId}"/>
        <prov:time>${endTime}</prov:time>
    </prov:wasGeneratedBy>

    <prov:wasDerivedFrom>
        <prov:generatedEntity prov:ref="WPS-Aggregator Dataset UID"/>
        <prov:usedEntity prov:ref="sourceData"/>
        <prov:time>${endTime}</prov:time>
    </prov:wasDerivedFrom>

    <!-- Document metadata: id, title, description, coverage, keywords (subject), date created -->x
    <prov:other>
        <dc:identifier>${jobId}</dc:identifier>
        <dc:title>Provenance document describing a gridded WPS result</dc:title>
        <dc:description>This gridded WPS used time, space and a layer definition to produce an aggregated NetCDF gridded output file</dc:description>
        <dc:coverage>northlimit=${northBL};southlimit=${southBL};eastlimit=${eastBL};westlimit=${westBL}</dc:coverage>
        <dc:subject>WPS</dc:subject>
        <dct:created>${.now?iso_utc}</dct:created>
    </prov:other>

</prov:document>
