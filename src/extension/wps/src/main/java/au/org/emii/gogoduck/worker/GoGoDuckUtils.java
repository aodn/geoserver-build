package au.org.emii.gogoduck.worker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GoGoDuckUtils {

    public static List<Converter> addFilters(String filters) {
        List<Converter> converters = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            List<String> filterList = new ArrayList<>(Arrays.asList(filters.split(",")));

            for (String filterType : filterList) {
                Converter converter = Converter.newInstance(filterType);
                converters.add(converter);
            }
        }
        return converters;
    }
}
