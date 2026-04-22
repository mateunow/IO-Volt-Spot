package pl.voltspot.backend.mapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConnectionTypeID {
    public static final Map<Integer, String> CONNECTORS;

    static {
        Map<Integer, String> temp = new HashMap<>(22);
        temp.put(0, "Unknown");
        temp.put(1, "J1772");
        temp.put(2, "CHAdeMO");
        temp.put(1044, "CHAOJI");
        temp.put(3, "BS1363TypeG");
        temp.put(25, "MennekesType2");
        temp.put(1036, "MennekesType2Tethered");
        temp.put(28, "Schuko");
        temp.put(32, "CCSComboType1");
        temp.put(33, "CCSComboType2");
        temp.put(22, "Nema5_15");
        temp.put(9, "Nema5_20");
        temp.put(11, "Nema14_50");
        temp.put(8, "TeslaRoadster");
        temp.put(30, "TeslaProprietary");
        temp.put(27, "TeslaSupercharger");
        temp.put(26, "Type3");
        temp.put(13, "Europlug");
        temp.put(29, "AS3112");
        temp.put(1041, "ThreePhaseAU");
        temp.put(1038, "GB_T_AC");
        temp.put(1040, "GB_T_DC");

        CONNECTORS = Collections.unmodifiableMap(temp);
    }
}
