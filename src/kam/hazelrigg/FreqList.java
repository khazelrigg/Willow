package kam.hazelrigg;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

class FreqMap {
    private Map<String, Integer> frequency = new HashMap<>();

    Map<String, Integer> getFrequency() {
        return sortByValue(this.frequency);
    }

    void increaseFreq(String key) {
        if (frequency.containsKey(key)) {
            frequency.put(key, frequency.get(key) + 1);
        }
        else {
            frequency.put(key, 1);
        }
    }

    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        /* Found on stack overflow:
            https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java#2581754
        */

        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                //                                  Sort descending instead of ascending
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }


}
