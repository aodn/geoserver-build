/*
 * Copyright 2014 IMOS
 *
 * The AODN/IMOS Portal is distributed under the terms of the GNU General Public License
 *
 */

package au.org.emii.geoserver.wfs.response;

import org.geotools.feature.type.DateUtil;
import org.opengis.feature.type.PropertyDescriptor;

import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Set;

public abstract class RestrictedColumnCsvOutputFormatSection {

    // by RFC each line is terminated by CRLF
    private static final String NEW_LINE = "\r\n";

    protected Set<String> excludedFilterNames;
    protected NumberFormat coordFormatter;

    public RestrictedColumnCsvOutputFormatSection(Set<String> excludedFilterNames, NumberFormat coordFormatter) {
        this.excludedFilterNames = excludedFilterNames;
        this.coordFormatter = coordFormatter;
    }

    public void writeNewLine(Writer writer) throws IOException {
        writer.write(getNewLine());
    }

    public String getNewLine() {
        return NEW_LINE;
    }

    protected String formatToString(Object att, NumberFormat coordFormatter) {
        if (att == null) {
            return "";
        }

        String value;
        if (att instanceof Number) {
            // don't allow scientific notation in the output, as OpenOffice won't
            // recognize that as a number
            value = coordFormatter.format(att);
        }
        else if (att instanceof Date) {
            value = formatDateToString(att);
        }
        else {
            // everything else we just "toString"
            value = att.toString();
        }
        return value;
    }

    private String formatDateToString(Object att) {
        String value;
        // serialize dates in ISO format
        if (att instanceof java.sql.Date) {
            value = DateUtil.serializeSqlDate((java.sql.Date)att);
        }
        else if (att instanceof java.sql.Time) {
            value = DateUtil.serializeSqlTime((java.sql.Time)att);
        }
        else {
            value = DateUtil.serializeDateTime((Date)att);
        }

        return value;
    }

    /*
     * The CSV "spec" explains that fields with certain properties must be
     * delimited by double quotes, and also that double quotes within fields
     * must be escaped.  This method takes a field and returns one that
     * obeys the CSV spec.
     */
    protected String prepCSVField(String field) {
        // "embedded double-quote characters must be represented by a pair of double-quote characters."
        String mod = field.replaceAll("\"", "\"\"");

        /*
         * Enclose string in double quotes if it contains double quotes, commas, or newlines
         */
        if(mod.matches(".*(\"|\n|,).*")) {
            mod = "\"" + mod + "\"";
        }

        return mod;
    }

    protected boolean isTemporaryAttribute(PropertyDescriptor descriptor) {
        return descriptor.getName().getLocalPart().startsWith("FEATURE_LINK");
    }

    protected boolean isNotTemporaryAttribute(PropertyDescriptor descriptor) {
        return !isTemporaryAttribute(descriptor);
    }

    protected boolean exclude(String item) {
        return excludedFilterNames.contains(item);
    }

    protected boolean include(String item) {
        return !exclude(item);
    }
}
