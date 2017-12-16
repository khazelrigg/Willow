package kam.hazelrigg;

import java.nio.file.Path;

public interface TextReader {
    void readText();

    void setPath(Path path);

    BookStats getStats();
}