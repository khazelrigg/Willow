package kam.hazelrigg;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.print.Book;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static kam.hazelrigg.Book.makeParentDirs;

class JsonCreator extends Book {

    static void writeJson(String name, String subdirectory, FreqMap posFreq) {
        File out;
        // Create results directories
        if (!makeParentDirs()) {
            System.out.println("[Error] Failed to create results directories");
            out = new File(name + " Results.json");
        } else {
            if (subdirectory.equals("")) {
                out = new File("results/json/" + name + " Results.json");
            } else {
                out = new File("results/json/" + subdirectory + "/" + name
                        + " Results.json");
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {

            JSONObject json = new JSONObject();
            json.put("name", "Parts of Speech");
            json.put("description", "Parts of speech distribution for " + name);

            JSONObject nouns = new JSONObject();
            nouns.put("name", "Nouns");
            nouns.put("description", "Nouns");

            JSONObject verbs = new JSONObject();
            verbs.put("name", "Verbs");
            verbs.put("description", "Verbs");

            JSONObject adverbs = new JSONObject();
            adverbs.put("name", "Adverbs");
            adverbs.put("description", "Adverbs");

            JSONObject adjective = new JSONObject();
            adjective.put("name", "Adjective");
            adjective.put("description", "Adjective");

            JSONArray nounTypes = new JSONArray();
            JSONArray verbTypes = new JSONArray();
            JSONArray adverbTypes = new JSONArray();
            JSONArray adjectiveTypes = new JSONArray();

            for (String type : posFreq.keySet()) {
                JSONObject parent = new JSONObject();
                parent.put("name", type);
                parent.put("description", type);

                JSONArray array = new JSONArray();
                JSONObject object = new JSONObject();
                object.put("name", type);
                object.put("description", type);
                object.put("size", posFreq.get(type));
                array.add(object);
                parent.put("children", array);

                if (TextTools.getParentType(type).equals("Noun")) {
                    nounTypes.add(parent);
                } else if (TextTools.getParentType(type).equals("Verb")) {
                    verbTypes.add(parent);
                } else if (TextTools.getParentType(type).equals("Adverb")) {
                    adverbTypes.add(parent);
                } else if (TextTools.getParentType(type).equals("Adjective")) {
                    adjectiveTypes.add(parent);
                }
            }

            nouns.put("children", nounTypes);
            verbs.put("children", verbTypes);
            adverbs.put("children", adverbTypes);
            adjective.put("children", adjectiveTypes);


            // Add all parent speech types to root parent
            JSONArray rootParent = new JSONArray();
            rootParent.add(nouns);
            rootParent.add(verbs);
            rootParent.add(adverbs);
            rootParent.add(adjective);
            json.put("children", rootParent);

            bw.write(json.toJSONString());
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("☑ - Finished writing JSON information for " + name);

    }


}
