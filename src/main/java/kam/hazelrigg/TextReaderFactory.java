package kam.hazelrigg;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Path;

class TextReaderFactory {

    TextReader getTextReader(Book book) throws IOException {
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

                textReader.setPath(path);
                textReader.setBookStats(bookStats);
                return textReader;

            case ("pdf"):
                textReader = new PdfTextReader();
                textReader.setPath(path);
                textReader.setBookStats(bookStats);
                return textReader;

            default:
                throw new IOException("Unsupported file format \"" + fileType + "\"");
        }
    }

}
