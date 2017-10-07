package kam.hazelrigg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static java.util.Map.Entry.comparingByValue;

class FreqMap {
    static String stopWords = "|you|us|we|which|where|were|with|was|what|her|him|had|has|have|" +
            "this|that|the|there|their|of|to|my|me|mine|if|or|and|a|an|as|are|on|i|in|is|it|so|" +
            "for|be|been|by|but|from|";
    private HashMap<String, Integer> frequency = new HashMap<>();

    void increaseFreq(String key) {
        if (frequency.containsKey(key)) {
            frequency.put(key, frequency.get(key) + 1);
        } else {
            frequency.put(key, 1);
        }
    }

    int getSize() {
        return frequency.entrySet().size();
    }

    public String toString() {

        StringBuilder result = new StringBuilder();
        sortByValue();
        frequency.forEach((key, value) -> result.append(String.format("%s → %d\n", key, value)));

        return result.toString();
    }

    private void sortByValue() {

        // https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java

        List<Entry<String, Integer>> toSort = new ArrayList<>();
        toSort.addAll(frequency.entrySet());
        toSort.sort(comparingByValue(Collections.reverseOrder()));
        frequency = toSort.stream().collect
                (Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new));

    }

}
