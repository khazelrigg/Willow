package kam.hazelrigg.viewer;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kam.hazelrigg.Book;
import kam.hazelrigg.Runner;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class ViewController implements Initializable {
    // Top bars
    public ToggleButton writeDocumentToggle;
    public ToggleButton posChartToggle;
    public ToggleButton diffChartToggle;

    // Content
    public ListView resultsFileListView;

    public ImageView posChartImageView;
    public ImageView difficultyImageView;

    // Bottom status
    public Label statusLabel;
    public Label timerLabel;

    private Book book = new Book();
    private File directory = null;
    private Stage chooserPane = new Stage();

    private void updateStatusLabel(String text) {
        statusLabel.setText(text);
    }

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
            updateStatusLabel("Opened " + book.getTitle());
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
            updateStatusLabel("Opened " + dir.getName());
        }
    }

    /**
     * Runs analysis on file(s) and updates gui
     */
    public void run() {
        if (directory != null) {
            long startTime = System.currentTimeMillis();

            updateStatusLabel("Analysing dir: " + directory);
            Runner.openDirectory(directory);
            setListOfFiles(directory.listFiles());

            long endTime = System.currentTimeMillis();
            timerLabel.setText("Finished in " + (endTime - startTime) / 1000 + " secs.");
        } else {
            long startTime = System.currentTimeMillis();
            updateStatusLabel("Analysing file: " + book.getTitle());

            if (book.resultsFileExists()) {
                showPosChart();
                showDifficultyChart();
            } else {
                book.analyseText();
                updateStatusLabel("Displaying results for: " + book.getTitle());
                if (writeDocumentToggle.isSelected()) {
                    book.writeFrequencies();
                }

                if (posChartToggle.isSelected()) {
                    book.makePosGraph();
                    showPosChart();
                }

                if (diffChartToggle.isSelected()) {
                    book.makeDifficultyGraph();
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
            //TODO add logic to avoid adding items twice if single files were opened beforehand
            addFileToList(file);
        }
    }

    /**
     * Add a single file to sidebar list of files
     * @param file file to add
     */
    private void addFileToList(File file) {
        ObservableList files = resultsFileListView.getItems();
        book.setTitleFromText(file);
        files.add(book.getTitle() + " by " + book.getAuthor());
        resultsFileListView.setItems(files);
    }

    //TODO get rid of absolute path for images
    private void showPosChart() {
        Image posGraph = new Image("file:results/img/"
                + book.getTitle() + " by " + book.getAuthor()
                + " POS Distribution Results.jpeg");

        posChartImageView.setImage(posGraph);
    }

    private void showDifficultyChart() {
        Image difficultyChart = new Image("file:results/img/" + book.getTitle() + " by "
                + book.getAuthor() + " Difficulty Results.jpeg");

        difficultyImageView.setImage(difficultyChart);

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        writeDocumentToggle.setSelected(true);
        posChartToggle.setSelected(true);
        diffChartToggle.setSelected(true);
    }

    public void updateViewer() {
        switchActiveBook(resultsFileListView.getSelectionModel().getSelectedItem().toString());
    }

    private void switchActiveBook(String file) {

        Task task = new Task<Void>() {
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
