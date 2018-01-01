package kam.hazelrigg.readers;

import org.apache.poi.hwpf.HWPFDocument;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class DocTextReader extends TextReader {

    public void readText() {
        try (InputStream inputStream = Files.newInputStream(path)) {
            HWPFDocument hwpfDocument = new HWPFDocument(inputStream);
            tagText(hwpfDocument.getText().toString());
        } catch (IOException e) {
            logger.error("Error opening doc file for reading");
        }
    }
}
