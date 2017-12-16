package kam.hazelrigg;

import java.io.BufferedReader;
import java.io.IOException;

class PlainTextReader extends TextReader {

    void readText() {
        boolean gutenberg = bookStats.isGutenberg();

        try (BufferedReader br = getDecodedBufferedReader()) {
            boolean atBook = !gutenberg;
            StringBuilder text = new StringBuilder();

            for (String line; (line = br.readLine()) != null; ) {
                if (line.isEmpty()) {
                    continue;
                }

                if (gutenberg) {
                    if (isGutenbergStart(line)) {
                        atBook = true;
                    } else if (isGutenbergEnd(line)) {
                        atBook = false;
                    }
                }

                if (atBook) {
                    text.append(line).append(" ");
                }
            }
            tagText(text.toString());
        } catch (IOException | NullPointerException e) {
            System.out.println("Error opening path");
            e.printStackTrace();
        }
    }

}
