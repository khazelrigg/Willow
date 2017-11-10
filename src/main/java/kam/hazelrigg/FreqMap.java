package main.java.kam.hazelrigg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static java.util.Map.Entry.comparingByValue;

public class FreqMap {

    private HashMap<String, Integer> frequency = new HashMap<>();

    /**
     * Increases the value of a key by 1.
     *
     * @param key Key to increase value of
     */
    void increaseFreq(String key) {
        if (frequency.containsKey(key)) {
            frequency.put(key, frequency.get(key) + 1);
        } else {
            frequency.put(key, 1);
        }
    }

    /**
     * Returns the FreqMap as a HashMap.
     *
     * @return HashMap version of FreqMap
     */
    HashMap<String, Integer> getFrequency() {
        sortByValue();
        return frequency;
    }

    /**
     * Gets the value of a key.
     *
     * @param key key to get value of
     * @return value of the key
     */
    int get(String key) {
        return frequency.get(key);
    }

    String getTopThree() {
        sortByValue();
        String[] values = frequency.keySet().toArray(new String[frequency.size()]);
        return values[0] + ", " + values[1] + ", " + values[2];
    }

    void stripFromFreq(File blacklist) {
        try (BufferedReader br = new BufferedReader(new FileReader(blacklist))) {
            for (String line; (line = br.readLine()) != null; ) {
                frequency.remove(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a string that contains keys and values separated with arrows.
     *
     * @return String of FreqMap
     */
    public String toString() {

        StringBuilder result = new StringBuilder();
        sortByValue();
        frequency.forEach((key, value) -> result.append(String.format("%s → %d\n", key, value)));

        return result.toString();
    }

    /**
     * Sorts the FreqMap in descending order by its values.
     */
    private void sortByValue() {
        // https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java

        ArrayList<Entry<String, Integer>> toSort = new ArrayList<>();
        toSort.addAll(frequency.entrySet());
        toSort.sort(comparingByValue(Collections.reverseOrder()));
        frequency = toSort.stream()
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    int getSize() {
        return frequency.entrySet().size();
    }

    Set<String> keySet() {
        return frequency.keySet();
    }

    String[] getSortedByKey() {
        String[] keys = frequency.keySet().toArray(new String[frequency.size()]);
        Arrays.sort(keys);
        return keys;
    }

    public String getSimpleString() {
        StringBuilder result = new StringBuilder();
        sortByValue();
        frequency.forEach((key, value) -> result.append(key).append(":").append(value).append("|"));
        return result.toString();
    }
}
