package kam.hazelrigg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import static java.util.Map.Entry.comparingByValue;

public class FreqMap<K, V> extends HashMap<K, V> {
    private HashMap<String, Integer> frequency;

    FreqMap() {
        frequency = new HashMap<>();
    }

    /**
     * Increases the value of a key by 1.
     *
     * @param key Key to increase value of
     */
    void increaseFreq(String key) {
        if (key != null) {
            if (frequency.containsKey(key)) {
                frequency.put(key, frequency.get(key) + 1);
            } else {
                frequency.put(key, 1);
            }
        }
    }

    /**
     * Removes all stopwords using the list defined in stopwords-english.txt
     */
    void stripStopWords() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/stopwords-english.txt")))) {
            for (String line; (line = br.readLine()) != null; ) {
                frequency.remove(line);
            }
            br.close();
        } catch (IOException e) {
            System.out.println("[Error - stripStopWords] Error opening stop words file");
            e.printStackTrace();
        }
    }

    private void sortByValue() {
        // https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java

        ArrayList<Entry<String, Integer>> toSort = new ArrayList<>();
        toSort.addAll(frequency.entrySet());
        toSort.sort(comparingByValue(Collections.reverseOrder()));
        frequency = toSort.stream()
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    String[] getSortedKeys() {
        String[] keys = frequency.keySet().toArray(new String[frequency.size()]);
        Arrays.sort(keys);
        return keys;
    }

    HashMap<String, Integer> toHashMap() {
        sortByValue();
        return frequency;
    }

    String getTopThreeValues() {
        sortByValue();
        String[] values = frequency.keySet().toArray(new String[frequency.size()]);
        return values[0] + ", " + values[1] + ", " + values[2];
    }

    public String toString() {

        StringBuilder result = new StringBuilder();
        sortByValue();
        frequency.forEach((key, value) -> result.append(String.format("%s, %d\n", key, value)));

        return result.toString();
    }

    String toSimpleString() {
        StringBuilder result = new StringBuilder();
        sortByValue();
        frequency.forEach((key, value) -> result.append(key).append(":").append(value).append("|"));
        return result.toString();
    }

    String toCsvString() {
        StringBuilder result = new StringBuilder();
        sortByValue();
        frequency.forEach((key, value) ->
                result.append("\"").append(key).append("\", ").append(value).append("\n"));
        return result.toString();
    }

    int get(String key) {
        return frequency.get(key);
    }

    public int size() {
        return frequency.keySet().size();
    }
}
