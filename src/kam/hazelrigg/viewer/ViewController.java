package kam.hazelrigg.viewer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kam.hazelrigg.Book;
import kam.hazelrigg.Runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ViewController implements Initializable {

    //TODO respond to clicks on file list if a directory is opened

    // Top bars
    public ToggleButton writeDocumentToggle;
    public ToggleButton posChartToggle;
    public ToggleButton diffChartToggle;

    // Content
    public HBox resultsFileListContainer;

    public ScrollPane resultsFileScroll;

    public ImageView posChartImageView;
    public ImageView difficultyImageView;

    // Bottom status
    public Label statusLabel;

    private Book book = new Book();
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
            book.setPath(file);
            book.setTitleFromText(file);
            updateStatusLabel("Opened " + book.getTitle());
        }
    }

    /**
     * Selects a folder using directoryChooser
     */
    public void openFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File dir = directoryChooser.showDialog(chooserPane);
        if (dir != null) {
            book.setPath(dir);
            updateStatusLabel("Opened " + dir.getName());
        }
    }

    /**
     * Runs analysis on file(s) and updates gui
     */
    public void run() {
        if (book.getPath().isDirectory()) {
            updateStatusLabel("Analysing dir: " + book.getPath());
            Runner.openDirectory(book.getPath());

            setListOfFiles(book.getPath().listFiles());

        } else {
            updateStatusLabel("Analysing file: " + book.getTitle());
            book.analyseText();
            updateStatusLabel("Displaying results for: " + book.getTitle());
            if (writeDocumentToggle.isSelected()) {
                book.writeFrequencies();
                readFileToCenter(new File("results/txt/" + book.getTitle() + " by " + book.getAuthor() + " Results.txt"));
            }

            if (posChartToggle.isSelected()) {
                showPosChart();
            }

            if (diffChartToggle.isSelected()) {
                showDifficultyChart();
            }
        }
    }

    /**
     * Creates a list of files in a directory
     * @param fileArray Array of files in a directory
     */
    private void setListOfFiles(File[] fileArray) {
        ListView<String> listView = new ListView<>();
        ObservableList<String> files = FXCollections.observableArrayList();

        for (File file : fileArray) {
            files.add(file.getName().replace(".txt", ""));
        }

        listView.setItems(files);

        resultsFileListContainer.getChildren().add(listView);
    }

    //TODO get rid of absolute path for images
    private void showPosChart() {
        book.makePosGraph();

        Image posGraph = new Image("file:results/img/"
                + book.getTitle() + " by " + book.getAuthor()
                + " POS Distribution Results.jpeg");

        posChartImageView.setImage(posGraph);
    }

    private void showDifficultyChart() {
        book.makeDifficultyGraph();

        Image difficultyChart = new Image("file:results/img/" + book.getTitle() + " by "
                + book.getAuthor() + " Difficulty Results.jpeg");

        difficultyImageView.setImage(difficultyChart);

    }

    /**
     * Reads file into a Text to be displayed
     * @param input file to read in
     */
    private void readFileToCenter(File input) {
        Text text = new Text("");
        if (input.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(input))) {
                String line = br.readLine();
                //System.out.println(line);
                while (line != null) {
                    text.setText(text.getText() + line + "\n");
                    line = br.readLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        resultsFileScroll.setContent(text);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        writeDocumentToggle.setSelected(true);
        posChartToggle.setSelected(true);
        diffChartToggle.setSelected(true);
        resultsFileScroll.setFitToWidth(true);
    }

}
