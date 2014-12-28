<?xml version="1.0"?>
<filters>
<#list filters as filter>
    <filter>
        <name>${filter.name}</name>
        <type>${filter.type}</type>
        <label>${filter.label}</label>
        <visualised>${filter.visualised?string}</visualised>
        <excludedFromDownload>${filter.excludedFromDownload?string}</excludedFromDownload>
    </filter>
</#list>
</filters>