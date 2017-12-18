package kam.hazelrigg.readers;

import java.io.BufferedReader;
import java.io.IOException;

public class PlainTextReader extends TextReader {

    public void readText() {
        boolean gutenberg = bookStats.isGutenberg();
        StringBuilder text = new StringBuilder();
        try (BufferedReader br = getDecodedBufferedReader()) {
            boolean atBook = !gutenberg;

            String line;
            while ((line = br.readLine()) != null) {
                if (!atBook && isGutenbergStart(line)) {
                    atBook = true;
                }

                if (atBook) {
                    if (isGutenbergEnd(line)) {
                        break;
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
