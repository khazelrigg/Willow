package kam.hazelrigg;

import java.io.File;

class Runner extends Thread {

  private final Book book;
  private final File file;
  final Thread thread;
  public boolean running = false;

  Runner(File file) {
    thread = new Thread(this);
    this.file = file;
    this.book = new Book();
    this.book.setPath(file);
  }

  @Override
  public void run() {
    this.running = true;

    book.setTitleFromText(file);

    if (book.resultsFileExists()) {
      System.out.println("☑ - " + file.getName() + " already has results");
    } else {
      book.analyseText();
      book.writeFrequencies();
      book.makePosGraph();
      book.makeDifficultyMap();
    }

    this.running = false;
  }

}
