package kam.hazelrigg.readers;

import kam.hazelrigg.Book;
import kam.hazelrigg.BookStats;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Path;

public class TextReaderFactory {

    public TextReader getTextReader(Book book) throws IOException {
        Path path = book.getPath();
        boolean economy = book.getEconomy();
        BookStats bookStats = book.getStats();
        String fileType = FilenameUtils.getExtension(path.getFileName().toString());
        TextReader textReader;

        switch (fileType) {
            case ("txt"):
                if (economy) {
                    textReader = new EconomyTextReader();
                } else {
                    textReader = new PlainTextReader();
                }

                textReader.setBookStats(bookStats);
                textReader.setPath(path);
                return textReader;

            case ("pdf"):
                textReader = new PdfTextReader();
                textReader.setBookStats(bookStats);
                textReader.setPath(path);
                return textReader;

            default:
                throw new IOException("Unsupported file format \"" + fileType + "\"");
        }
    }
}
