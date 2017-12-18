package kam.hazelrigg.readers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

public class PdfTextReader extends TextReader {

    public void readText() {
        try {
            PDDocument pdDocument;
            pdDocument = PDDocument.load(path.toFile());
            PDDocumentInformation info = pdDocument.getDocumentInformation();
            updateStatsFromMetadata(info);

            PDFTextStripper pdfStripper = new PDFTextStripper();
            int numPages = pdDocument.getNumberOfPages();

            for (int pageIndex = 1; pageIndex < numPages; pageIndex++) {
                pdfStripper.setStartPage(pageIndex);
                pdfStripper.setEndPage(pageIndex);
                tagText(pdfStripper.getText(pdDocument));
            }


        } catch (IOException | NullPointerException e) {
            logger.error("IOException parsing PDF at {}", path.getFileName());
        }
    }

    private void updateStatsFromMetadata(PDDocumentInformation information) {
        String title = information.getTitle();
        String author = information.getAuthor();
        if (title != null) {
            book.setTitle(title);
        }
        if (author != null) {
            book.setAuthor(author);
        }
    }

}
