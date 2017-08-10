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


    <!-- ${test} -->


    <prov:entity prov:id="WPS-Aggregator Dataset UID">
        <!-- location is the (UID) URL for this output file. Note entities such as this and others
             below must persist for the provenance to be of value -->
        <prov:location>http://www.imos.org/location on amazon S3</prov:location>
        <!-- prov:type is Mandatory -->
        <prov:type codeList="codeListLocation#type" codeListValue="output">output</prov:type>
    </prov:entity>

    <!-- Activities - in this scenario, our single activity is a Web Processing Service
       and it is controlled by some Java software  - see prov:softwareAgent -->

    <prov:activity prov:id="WPS-Gridded-Aggregator-Service-Job:e532d1ef-516c-4664-a3af-a184f9f27">
        <!-- startTime is the time the service was initiated -->
        <prov:startTime>2016-08-31T09:15:00</prov:startTime>
        <!-- endTime is the time the service was completed -->
        <prov:endTime>2016-08-31T15:15:00</prov:endTime>
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
                            <!-- gmd:id needs to be unique to this document, gml:TimePeriod can be
                                 quite sophisticated - see for example
                                 https://geo-ide.noaa.gov/wiki/index.php?title=TimePeriod -->
                            <gml:TimePeriod gml:id="A1234">
                                <gml:beginPosition>2012-03-31T09:21:00</gml:beginPosition>
                                <gml:endPosition>2012-04-01T15:21:00</gml:endPosition>
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
    <prov:entity prov:id="spatialExtent">
        <aodnprov:boundingBox>
            <gex:EX_Extent>
                <gex:geographicElement>
                    <gex:EX_GeographicBoundingBox>
                        <gex:westBoundLongitude>
                            <gco:Decimal>112</gco:Decimal>
                        </gex:westBoundLongitude>
                        <gex:eastBoundLongitude>
                            <gco:Decimal>154</gco:Decimal>
                        </gex:eastBoundLongitude>
                        <gex:southBoundLatitude>
                            <gco:Decimal>-44</gco:Decimal>
                        </gex:southBoundLatitude>
                        <gex:northBoundLatitude>
                            <gco:Decimal>-9</gco:Decimal>
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
        <prov:location>srs_sst_13s_14d_dn_gridded_url</prov:location>
        <!-- prov:type is Mandatory -->
        <prov:type codeList="codeListLocation#type" codeListValue="inputParameter">inputParameter</prov:type>
    </prov:entity>

    <prov:entity prov:id="outputAggregationSettings">
        <!-- location is the URL of the gridded NetCDF Aggregation settings (index reader, download and aggregation templates) file for this WPS job-->
        <prov:location>..wps/gogoduck.xml</prov:location>
        <!-- prov:type is Mandatory -->
        <prov:type codeList="codeListLocation#type" codeListValue="outputConfiguration">outputConfiguration</prov:type>
    </prov:entity>

    <prov:entity prov:id="sourceData">
        <!-- location is the URL of the metadata record for the data collection operated on by this WPS job-->
        <prov:location>https://catalogue-imos.aodn.org.au/geonetwork/srv/eng/metadata.show?uuid=ae86e2f5-eaaf-459e-a405-e654d85adb9c</prov:location>
        <!-- prov:type is Mandatory -->
        <prov:type codeList="codeListLocation#type" codeListValue="inputData">inputData</prov:type>
    </prov:entity>

    <prov:entity prov:id="processingStatistics">
        <!-- location is the URL of the file generated by this WPS job that records: names of files included in the aggregation; file sizes; overall output size etc-->
        <prov:location>http://www.imos.org/location on amazon S3 ?</prov:location>
        <!-- prov:type is Mandatory -->
        <prov:type codeList="codeListLocation#type" codeListValue="processingStatistics">processingStatistics</prov:type>
    </prov:entity>

    <!-- Agents & Actors - the people, organization and software involved in WPS execution  -->

    <prov:softwareAgent prov:id="JavaCode">
        <!-- location is a metadata record describing the Java Code -->
        <prov:location>https://catalogue-imos.aodn.org.au/geonetwork/srv/eng/metadata.show?uuid=ae99e2f5-eaaf-344e-a405-e654d85adb9d</prov:location>
        <dct:hasVersion>1.0</dct:hasVersion>
    </prov:softwareAgent>


    <!-- Associations and softwareSystem used  -->

    <!-- Note from Simon - prov:role for wasAssociatedWith comes from ISO19115 responsibility roles + softwareSystem -->

    <prov:wasAssociatedWith>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:e532d1ef-516c-4664-a3af-a184f9f27"/>
        <prov:agent prov:ref="JavaCode"/>
        <prov:role codeList="codeListLocation#type" codeListValue="softwareSystem">softwareSystem</prov:role>
    </prov:wasAssociatedWith>


    <!-- Followed Simon's convention for entities used and outputs generated. In the prov:used elements, the prov:role corresponds
         to the prov:entity/prov:type - this is a deliberate choice -->

    <prov:used>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:e532d1ef-516c-4664-a3af-a184f9f27"/>
        <prov:entity prov:ref="timeExtent"/>
        <prov:role codeList="codeListLocation#type" codeListValue="timeExtent">timeExtent</prov:role>
    </prov:used>

    <prov:used>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:e532d1ef-516c-4664-a3af-a184f9f27"/>
        <prov:entity prov:ref="spatialExtent"/>
        <prov:role codeList="codeListLocation#type" codeListValue="boundingBox">boundingBox</prov:role>
    </prov:used>

    <prov:used>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:e532d1ef-516c-4664-a3af-a184f9f27"/>
        <prov:entity prov:ref="layerName"/>
        <prov:role codeList="codeListLocation#type" codeListValue="inputParameter">inputParameter</prov:role>
    </prov:used>

    <prov:used>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:e532d1ef-516c-4664-a3af-a184f9f27"/>
        <prov:entity prov:ref="outputAggregationSettings"/>
        <prov:role codeList="codeListLocation#type" codeListValue="outputConfiguration">outputConfiguration</prov:role>
    </prov:used>

    <prov:wasGeneratedBy>
        <prov:entity prov:ref="WPS-Aggregator Dataset UID"/>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:e532d1ef-516c-4664-a3af-a184f9f27"/>
        <prov:time>2016-08-31T15:15:00</prov:time>
    </prov:wasGeneratedBy>

    <prov:wasGeneratedBy>
        <prov:entity prov:ref="processingStatistics"/>
        <prov:activity prov:ref="WPS-Gridded-Aggregator-Service-Job:e532d1ef-516c-4664-a3af-a184f9f27"/>
        <prov:time>2016-08-31T15:15:00</prov:time>
    </prov:wasGeneratedBy>

    <prov:wasDerivedFrom>
        <prov:generatedEntity prov:ref="WPS-Aggregator Dataset UID"/>
        <prov:usedEntity prov:ref="sourceData"/>
        <prov:time>2016-08-31T15:15:00</prov:time>
    </prov:wasDerivedFrom>

    <!-- Document metadata: id, title, description, coverage, keywords (subject), date created -->

    <prov:other>
        <dc:identifier>39ee0ae6-b1ae-472f-a5f2-435e342267ea</dc:identifier>
        <dc:title>Provenance document describing a gridded WPS result</dc:title>
        <dc:description>This gridded WPS used time, space and a layer definition to produce an aggregated NetCDF gridded output file</dc:description>
        <dc:coverage>northlimit=-9;southlimit=-44;eastlimit=154;westlimit=112</dc:coverage>
        <dc:subject>WPS</dc:subject>
        <dct:created>2016-08-31T15:15:00</dct:created>
    </prov:other>

</prov:document>
