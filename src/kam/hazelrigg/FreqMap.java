package kam.hazelrigg;

import java.util.HashMap;

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

        frequency.forEach((key, value) -> result.append(String.format("%s → %d\n", key, value)));

        return result.toString();
    }

    void sortMaps() {

    }

}
