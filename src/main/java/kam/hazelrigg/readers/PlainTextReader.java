package kam.hazelrigg.readers;

import java.io.BufferedReader;
import java.io.IOException;

public class PlainTextReader extends TextReader {

    public void readText() {
        boolean gutenberg = bookStats.isGutenberg();

        try (BufferedReader br = getDecodedBufferedReader()) {
            boolean atBook = !gutenberg;
            StringBuilder text = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null) {

                if (!atBook && isGutenbergStart(line)) {
                    atBook = true;
                    line = "";
                }

                if (atBook) {
                    if (isGutenbergEnd(line)) {
                        atBook = false;
                        continue;
                    }
                    text.append(line).append(" ");
                }
            }
            tagText(text.toString());
        } catch (IOException | NullPointerException e) {
            logger.error("Error reading file at {}", path.toString());
        }
    }

}
