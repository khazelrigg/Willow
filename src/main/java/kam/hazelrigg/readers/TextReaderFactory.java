package kam.hazelrigg.readers;

import kam.hazelrigg.Book;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Path;

public class TextReaderFactory {

    public TextReader getTextReader(Book book) throws IOException {
        Path path = book.getPath();
        boolean economy = book.getEconomy();
        String fileType = FilenameUtils.getExtension(path.getFileName().toString());
        TextReader textReader;

        switch (fileType) {
            case "txt":
                if (economy) {
                    textReader = new EconomyTextReader();
                } else {
                    textReader = new PlainTextReader();
                }
                break;
            case "pdf":
                textReader = new PdfTextReader();
                break;
            case "doc":
                textReader = new DocTextReader();
                break;
            case "docx":
                textReader = new DocxTextReader();
                break;
            default:
                throw new IOException("Unsupported file format \"" + fileType + "\"");
        }

        textReader.setBook(book);
        textReader.setPath(path);
        return textReader;
    }
}
