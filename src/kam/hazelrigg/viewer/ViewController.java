package kam.hazelrigg.viewer;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kam.hazelrigg.BatchRunner;
import kam.hazelrigg.Book;
import kam.hazelrigg.OutputWriter;
import kam.hazelrigg.Runner;

import java.io.File;

public class ViewController {
    // Top bars
    public ToggleButton writeDocumentToggle;
    public ToggleButton posChartToggle;
    public ToggleButton diffChartToggle;

    // Content
    public ListView<String> resultsFileListView;

    public ImageView posChartImageView;
    public ImageView difficultyImageView;

    // Bottom status
    public Label statusLabel;
    public Label timerLabel;

    private Book book = new Book();
    private File directory = null;
    private Stage chooserPane = new Stage();

    /**
     * Selects a file using fileChooser
     */
    public void openFile() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(chooserPane);

        if (file != null) {
            directory = null;
            book.setPath(file);
            book.setTitleFromText(file);
            addFileToList(file);
            statusLabel.setText("Opened " + book.getTitle());
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
            directory = dir;
            statusLabel.setText("Opened " + dir.getName());
        }
    }

    /**
     * Runs analysis on file(s) and updates gui
     */
    public void run() {
        OutputWriter ow = new OutputWriter(book);
        if (directory != null) {
            long startTime = System.currentTimeMillis();

            statusLabel.setText("Analysing dir: " + directory);
            BatchRunner.startRunners(directory);
            setListOfFiles(directory.listFiles());

            long endTime = System.currentTimeMillis();
            timerLabel.setText("Finished in " + (endTime - startTime) / 1000 + " secs.");
        } else {
            long startTime = System.currentTimeMillis();
            statusLabel.setText("Analysing file: " + book.getTitle());

            if (book.resultsFileExists()) {
                showPosChart();
                showDifficultyChart();
            } else {
                book.analyseText();
                statusLabel.setText("Displaying results for: " + book.getTitle());

                if (writeDocumentToggle.isSelected()) {
                    ow.writeTxt();
                }

                if (posChartToggle.isSelected()) {
                    ow.makePosGraph();
                    showPosChart();
                }

                if (diffChartToggle.isSelected()) {
                    ow.makeDifficultyGraph();
                    showDifficultyChart();
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
     * Creates a list of files in a directory
     * @param fileArray Array of files in a directory
     */
    private void setListOfFiles(File[] fileArray) {
        for (File file : fileArray) {
            if (file.isDirectory()) {
                setListOfFiles(file.listFiles()); 
            }
            addFileToList(file);
        }
    }

    /**
     * Add a single file to sidebar list of files
     * @param file file to add
     */
    private void addFileToList(File file) {
        ObservableList<String> files = resultsFileListView.getItems();
        //TODO add logic to avoid adding items twice if single files were opened beforehand
        book.setTitleFromText(file);

        if (!files.contains(book.getName())) {
            files.add(book.getName());
            resultsFileListView.setItems(files);
        }
    }

    //TODO get rid of absolute path for images
    private void showPosChart() {
        Image posGraph = new Image("file:results/img/" + book.getName()
                + " POS Distribution Results.jpeg");

        posChartImageView.setImage(posGraph);
    }

    private void showDifficultyChart() {
        Image difficultyChart = new Image("file:results/img/" + book.getName()
                + " Difficulty Results.jpeg");

        difficultyImageView.setImage(difficultyChart);

    }

    @FXML
    public void initialize() {
        writeDocumentToggle.setSelected(true);
        posChartToggle.setSelected(true);
        diffChartToggle.setSelected(true);
    }

    public void updateViewer() {
        switchActiveBook(resultsFileListView.getSelectionModel().getSelectedItem());
    }

    /**
     * Changes the current book by updating images
     * @param file File to switch to
     */
    private void switchActiveBook(String file) {

        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() {
                book.setTitle(file.split(" by ")[0]);
                book.setAuthor(file.split(" by ")[1]);
                showPosChart();
                showDifficultyChart();
                return null;
            }
        };

        new Thread(task).start();
    }
}
