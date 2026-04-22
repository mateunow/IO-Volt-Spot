package pl.voltspot.backend.mapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CurrentTypeID {
    public static final Map<Integer, String> CURRENTS;

    static {
        Map<Integer, String> temp = new HashMap<>(3);
        temp.put(10, "SinglePhaseAC");
        temp.put(20, "ThreePhaseAC");
        temp.put(30, "DC");

        CURRENTS = Collections.unmodifiableMap(temp);
    }
}
