package main.java.kam.hazelrigg;

import java.io.File;

public class Runner extends Thread {

    private final Book book;
    private final File file;
    public static boolean running = false;

    Runner(File file, File sub, String start) {
        new Thread(this);
        this.file = file;
        this.book = new Book();
        this.book.setPath(file);
        try {
            String parentOfSub = sub.getParentFile().toString().substring(start.length() + 1);
            this.book.setSubdirectory(parentOfSub);
            System.out.println("┌══════════[ NEW BOOK ]══════════╾\n│ ┌╾ " + parentOfSub
                    + "\n│ └──╾ " + file.getPath() + "\n└════════════════════════════════╾\n");

        } catch (StringIndexOutOfBoundsException e) {
            //If theres is no subdirectory parent we must be in the parent directory
            this.book.setSubdirectory("");
            System.out.println("┌══════════[ NEW BOOK ]══════════╾\n| ┌╾ ROOT\n│ └──╾ "
                    + file.getPath() + "\n└════════════════════════════════╾\n");
        }
    }

    /**
     * Actions to perform with each book
     */
    private void runBook() {
        book.readText();
        OutputWriter ow = new OutputWriter(book);
        ow.writeTxt();
        ow.writeJson();
        ow.makeDiffGraph();
        ow.makePosGraph();
        long endTime = System.currentTimeMillis();
        System.out.println("[FINISHED] Completely finished " + book.getName() + " in "
                + (endTime - WordCount.startTime) / 1000 + "s.");
    }

    @Override
    public void run() {
        running = true;
        book.setTitleFromText(file);

        if (book.resultsFileExists()) {
            System.out.println("☑ - " + file.getName() + " already has results");
        } else {
            runBook();
        }

        running = false;
    }

}