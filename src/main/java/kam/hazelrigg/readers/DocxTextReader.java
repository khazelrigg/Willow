package kam.hazelrigg.readers;


import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class DocxTextReader extends TextReader {

    public void readText() {
        try (InputStream inputStream = Files.newInputStream(path)) {
            XWPFDocument xwpfDocument = new XWPFDocument(inputStream);
            XWPFWordExtractor wordExtractor = new XWPFWordExtractor(xwpfDocument);
            tagText(wordExtractor.getText());
        } catch (IOException e) {
            logger.error("Error opening doc file for reading");
        }
    }

}
