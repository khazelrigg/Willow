package kam.hazelrigg;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

class PdfTextReader extends TextReader {

    public void readText() {
        try {
            PDDocument pdDocument;
            pdDocument = PDDocument.load(path.toFile());
            PDFTextStripper pdfStripper = new PDFTextStripper();

            tagText(pdfStripper.getText(pdDocument));
        } catch (IOException | NullPointerException e) {
            System.out.println("[Error - readPdf] IOException when opening PDFParser for "
                    + path.getFileName());
            e.printStackTrace();
        }
    }

}
