<?xml version="1.0"?>
<filters>
<#list filters as filter>
    <filter>
        <name>${filter.name}</name>
        <type>${filter.type}</type>
        <label>${filter.label}</label>
        <visualised>${filter.visualised?string}</visualised>
        <excludedFromDownload>${filter.excludedFromDownload?string}</excludedFromDownload>
        <#if (filter.getExtras().entrySet().size() > 0) ><#t>
            <#lt><#list filter.getExtras().entrySet() as extra >        <${extra.key}>${extra.value}</${extra.key}>
            </#list>
        </#if><#t>
    </filter>
</#list>
</filters>
