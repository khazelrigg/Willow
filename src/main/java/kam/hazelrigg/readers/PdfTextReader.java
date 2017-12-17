package kam.hazelrigg.readers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

public class PdfTextReader extends TextReader {

    public void readText() {
        try {
            PDDocument pdDocument;
            pdDocument = PDDocument.load(path.toFile());
            PDFTextStripper pdfStripper = new PDFTextStripper();

            tagText(pdfStripper.getText(pdDocument));
        } catch (IOException | NullPointerException e) {
            logger.error("IOException parsing PDF at {}", path.getFileName());
        }
    }

}
