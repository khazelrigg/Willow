package kam.hazelrigg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

class TextTools {
    static HashMap<String, String> posAbbrev = nonAbbreviate();

    static int getSyllableCount(String s) {
        s = s.trim();
        if (s.length() <= 3) {
            return 1;
        }
        s = s.toLowerCase();
        s = s.replaceAll("[aeiouy]+", "a");
        s = "x" + s + "x";
        return s.split("a").length - 1;
    }

    static int getReadingTimeInMinutes(long wordCount) {
        return (int) (wordCount / 275);
    }

    static int getSpeakingTimeInMinutes(long wordCount) {
        return (int) (wordCount / 180);
    }

    /**
     * Formats a string into a 100 character wide box.
     *
     * @param text Text to be placed into box
     * @return Formatted String
     */
    static String wrapInBox(String text) {
        //                   TL   TC   TR   LL   LC   LR   COL
        String[] boxParts = {"╒", "═", "╕", "└", "─", "┘", "│"};
        StringBuilder wrapped = new StringBuilder();

        // Start with TL corner
        wrapped.append(boxParts[0]);
        for (int i = 0; i <= 98; i++) {
            if (i == 98) {
                // Add TR corner
                wrapped.append(boxParts[2]).append("\n");
            } else {
                wrapped.append(boxParts[1]);
            }
        }

        // Add Column and text
        wrapped.append(boxParts[6]).append(" ").append(text);
        for (int i = 0; i <= 97 - text.length(); i++) {
            if (i == 97 - text.length()) {
                wrapped.append(boxParts[6]).append("\n");
            } else {
                wrapped.append(" ");
            }
        }

        // Draw bottom row
        wrapped.append(boxParts[3]);
        for (int i = 0; i <= 98; i++) {
            if (i == 98) {
                wrapped.append(boxParts[5]).append("\n");
            } else {
                wrapped.append(boxParts[4]);
            }
        }
        wrapped.append("\n");
        return wrapped.toString();
    }

    static String getParentType(String type) {

        if (type.equals("Noun, singular or mass") || type.equals("Noun, plural")
                || type.equals("Proper noun, singular")
                || type.equals("Proper noun, plural")) {
            return "Noun";
        }

        if (type.equals("Verb, base form") || type.equals("Verb, past tense")
                || type.equals("Verb, gerund or present participle")
                || type.equals("Verb, past participle")
                || type.equals("Verb, non-3rd person singular present")
                || type.equals("Verb, 3rd person singular present")) {
            return "Verb";
        }

        if (type.equals("Adverb") || type.equals("Adverb, comparative")
                || type.equals("Adverb, superlative") || type.equals("Wh-adverb")) {
            return "Adverb";
        }

        if (type.equals("Adjective") || type.equals("Adjective, comparative")
                || type.equals("Adjective, superlative")) {
            return "Adjective";
        }

        if (type.equals("Personal pronoun") || type.equals("Possessive pronoun")
                || type.equals("Possessive wh pronoun") || type.equals("Wh-pronoun")) {
            return "Pronoun";
        }

        return "Other";

    }

    private static HashMap<String, String> nonAbbreviate() {

        HashMap<String, String> posNoAbbrev = new HashMap<>();

        try {
            InputStreamReader inputStreamReader =
                    new InputStreamReader(TextTools.class.getClass()
                            .getResourceAsStream("/posAbbreviations.txt"));

            BufferedReader br =
                    new BufferedReader(inputStreamReader);

            String line = br.readLine();
            while (line != null) {
                String[] words = line.split(":");
                // Set key to abbreviation and value to non abbreviated
                posNoAbbrev.put(words[0].trim(),
                        words[1].substring(0, words[1].lastIndexOf(">")).trim());
                line = br.readLine();
            }

            br.close();
            return posNoAbbrev;
        } catch (IOException ioe) {
            System.out.println("[Error - nonAbbreviate] " + ioe);
        }

        return posNoAbbrev;
    }
}
