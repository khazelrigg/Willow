package kam.hazelrigg.viewer;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kam.hazelrigg.BatchRunner;
import kam.hazelrigg.Book;
import kam.hazelrigg.OutputWriter;
import kam.hazelrigg.Runner;

import java.io.File;
import java.util.HashMap;

public class ViewController {
    // Top bars
    public ToggleButton writeDocumentToggle;
    public ToggleButton posChartToggle;
    public ToggleButton diffChartToggle;
    public ToggleButton writeJsonToggle;
    public Button runButton;

    // Content
    public ListView<String> resultsFileListView;

    private ImageView posChartImageView = new ImageView();
    private ImageView difficultyImageView = new ImageView();

    // Bottom status
    public Label statusLabel;
    public Label timerLabel;
    public VBox posTab;
    public VBox diffTab;

    private Book book = new Book();
    private File seedDirectory = null;
    private Stage chooserPane = new Stage();

    private HashMap<String, Book> bookMap = new HashMap<>();

    @FXML
    public void initialize() {
        writeDocumentToggle.setSelected(true);
        writeJsonToggle.setSelected(true);
        posChartToggle.setSelected(true);
        diffChartToggle.setSelected(true);
        runButton.setDisable(true);
    }

    /**
     * Selects a file using fileChooser
     */
    public void openFile() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(chooserPane);

        if (file != null) {
            seedDirectory = null;
            book.setPath(file);
            book.setTitleFromText(file);
            addFileToList(file);
            statusLabel.setText("Opened " + book.getTitle());
            runButton.setDisable(false);
        }
    }

    /**
     * Selects a folder using directoryChooser
     */
    public void openFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open folder");
        directoryChooser.setInitialDirectory(
                new File(System.getProperty("user.home"))
        );


        File dir = directoryChooser.showDialog(chooserPane);
        if (dir != null) {
            seedDirectory = dir;
            statusLabel.setText("Opened " + dir.getName());
            runButton.setDisable(false);
        }
    }

    /**
     * Runs analysis on file(s) and updates gui
     */
    public void run() {
        OutputWriter ow = new OutputWriter(book);
        // Add image viewers
        posTab.getChildren().remove(0);
        posTab.getChildren().add(posChartImageView);
        diffTab.getChildren().remove(0);
        diffTab.getChildren().add(difficultyImageView);
        if (seedDirectory != null) {
            long startTime = System.currentTimeMillis();

            statusLabel.setText("Analysing dir: " + seedDirectory);
            BatchRunner.startRunners(seedDirectory);
            setListOfFiles(seedDirectory.listFiles());

            long endTime = System.currentTimeMillis();
            timerLabel.setText("Finished in " + (endTime - startTime) / 1000 + " secs.");
        } else {
            long startTime = System.currentTimeMillis();
            statusLabel.setText("Analysing file: " + book.getTitle());

            if (book.resultsFileExists()) {
                showPosChart(book);
                showDifficultyChart(book);
            } else {
                book.analyseText();
                statusLabel.setText("Displaying results for: " + book.getTitle());

                if (writeDocumentToggle.isSelected()) {
                    ow.writeTxt();
                }

                if (writeJsonToggle.isSelected()) {
                    ow.writeJson();
                }

                if (posChartToggle.isSelected()) {
                    ow.makePosGraph();
                    showPosChart(book);
                }

                if (diffChartToggle.isSelected()) {
                    ow.makeDifficultyGraph();
                    showDifficultyChart(book);
                }

                while (Runner.running) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                long endTime = System.currentTimeMillis();
                timerLabel.setText((endTime - startTime) / 1000 + " secs.");
            }
        }
    }

    /**
     * Creates a list of files in a seedDirectory
     * @param fileArray Array of files in a seedDirectory
     */
    private void setListOfFiles(File[] fileArray) {
        for (File file : fileArray) {
            if (file.isDirectory()) {
                setListOfFiles(file.listFiles());
            } else {
                addFileToList(file);
            }
        }
    }

    /**
     * Add a single file to sidebar list of files
     * @param file file to add
     */
    private void addFileToList(File file) {
        ObservableList<String> files = resultsFileListView.getItems();
        //TODO add logic to avoid adding items twice if single files were opened beforehand
        Book newBook = new Book();
        newBook.setTitleFromText(file);
        newBook.setPath(file);

        if (!files.contains(newBook.getName())) {
            files.add(newBook.getName());
            bookMap.put(newBook.getName(), newBook);
            resultsFileListView.setItems(files);
        }
    }

    public void updateViewer() {
        switchActiveBook(resultsFileListView.getSelectionModel().getSelectedItem());
    }

    /**
     * Changes the current book by updating images
     */
    private void switchActiveBook(String listItemText) {
        //TODO test more use cases     (File not in subdir)
        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() {
                // Get new book from map using the book name
                Book cur = bookMap.get(listItemText);
                //Set any subdirectories
                cur.subdirectory = cur.getPath().getParentFile().getName();
                showPosChart(cur);
                showDifficultyChart(cur);
                return null;
            }
        };
        new Thread(task).start();
    }


    //TODO get rid of absolute path for images
    private void showPosChart(Book cur) {
        Image posGraph = new Image("file:results/img/" + cur.subdirectory + "/" + cur.getName()
                + " POS Distribution Results.jpeg");

        posChartImageView.setImage(posGraph);
    }

    private void showDifficultyChart(Book cur) {
        Image difficultyChart = new Image("file:results/img/" + cur.subdirectory + "/" + cur.getName()
                + " Difficulty Results.jpeg");

        difficultyImageView.setImage(difficultyChart);
    }
}
