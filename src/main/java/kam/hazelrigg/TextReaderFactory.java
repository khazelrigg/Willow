package kam.hazelrigg;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Path;

class TextReaderFactory {

    TextReader getTextReader(Path path, boolean economy) throws IOException {
        String fileType = FilenameUtils.getExtension(path.getFileName().toString());
        TextReader textReader;

        switch (fileType) {
            case ("txt"):
                if (economy) {
                    textReader = new EconomyTextReader();
                    textReader.setPath(path);
                } else {
                    textReader = new PlainTextReader();
                    textReader.setPath(path);
                }
                return textReader;
            case ("pdf"):
                textReader = new PdfTextReader();
                textReader.setPath(path);
                return textReader;
            default:
                throw new IOException("Unsupported file format \"" + fileType + "\"");
        }
    }
}
