package kam.hazelrigg.viewer;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import javafx.application.Platform;
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
import kam.hazelrigg.Book;
import kam.hazelrigg.OutputWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Properties;

public class ViewController {
    private StanfordCoreNLP pipeline;

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
    private Stage chooserPane = new Stage();

    private HashMap<String, Book> bookMap = new HashMap<>();

    @FXML
    public void initialize() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma");
        pipeline = new StanfordCoreNLP(props);

        writeDocumentToggle.setSelected(true);
        writeJsonToggle.setSelected(true);
        posChartToggle.setSelected(true);
        diffChartToggle.setSelected(true);
        runButton.setDisable(true);
    }

    /**
     * Selects a file using fileChooser
     */
    public void openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open file");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Plain Text (txt)", "*.txt"),
                new FileChooser.ExtensionFilter("PDF documents", "*.pdf"));

        File file = fileChooser.showOpenDialog(chooserPane);
        if (file != null) {
            openFile(file);
        }
    }

    /**
     * Selects a folder using directoryChooser
     */
    public void openFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open folder");
        directoryChooser.setInitialDirectory(
                new File(System.getProperty("user.home")));

        File dir = directoryChooser.showDialog(chooserPane);
        if (dir != null) {
            try {
                Files.walk(Paths.get(dir.getAbsolutePath()))
                        .filter(Files::isRegularFile)
                        .forEach(f -> openFile(f.toFile()));
            } catch (IOException e) {
                System.out.println("[Error - openFolder] IOException walking files in "
                        + dir.getName());
                e.printStackTrace();
            }
            runButton.setDisable(false);
        }
    }

    private void openFile(File file) {
        Platform.runLater(() -> {
            if (file != null) {
                book.setSubdirectory(book.getPath().getParentFile().getName());
                book.givePipeline(pipeline);
                book.setTitleFromText(file);
                statusLabel.setText("Opened " + book.getTitle());
                runButton.setDisable(false);
                run();
                addFileToList(file);
                switchActiveBook(book.getName());
            }
        });
    }

    /**
     * Add a single file to sidebar list of files
     *
     * @param file file to add
     */
    private void addFileToList(File file) {
        ObservableList<String> files = resultsFileListView.getItems();

        Book newBook = new Book();
        newBook.setTitleFromText(file);
        newBook.setSubdirectory(newBook.getPath().getParentFile().getName());

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
        Platform.runLater(() -> {
            // Get new book from map using the book name
            Book cur = bookMap.get(listItemText);
            run();
            showPosChart(cur);
            showDifficultyChart(cur);
        });

    }


    /**
     * Runs analysis on file(s) and updates gui
     */
    public void run() {

        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                OutputWriter ow = new OutputWriter(book);
                long startTime = System.currentTimeMillis();
                statusLabel.setText("Analysing file: " + book.getTitle());

                if (book.resultsFileExists((diffChartToggle.isSelected() && posChartToggle.isSelected()), writeJsonToggle.isSelected())) {
                    showPosChart(book);
                    showDifficultyChart(book);
                } else {
                    book.readText(false);

                    statusLabel.setText("Displaying results for: " + book.getTitle());

                    if (writeDocumentToggle.isSelected()) {
                        ow.writeTxt();
                    }

                    if (writeJsonToggle.isSelected()) {
                        ow.writeJson();
                    }

                    if (posChartToggle.isSelected()) {
                        ow.makePosGraph();
                    }

                    if (diffChartToggle.isSelected()) {
                        ow.makeDiffGraph();
                    }

                    long endTime = System.currentTimeMillis();
                    timerLabel.setText((endTime - startTime) / 1000 + " secs.");
                }

                return null;
            }
        };
        task.run();

        Platform.runLater(() -> {

        });
    }



    //TODO get rid of absolute path for images
    private void showPosChart(Book cur) {
        Image posGraph = new Image("file:results/img/" + cur.getSubdirectory() + "/" + cur.getName()
                + " POS Distribution Results.jpeg");

        posChartImageView.setImage(posGraph);
        posTab.getChildren().remove(0);
        posTab.getChildren().add(posChartImageView);
    }

    private void showDifficultyChart(Book cur) {
        Image difficultyChart = new Image("file:results/img/" + cur.getSubdirectory() + "/" + cur.getName()
                + " Difficulty Results.jpeg");

        difficultyImageView.setImage(difficultyChart);
        diffTab.getChildren().remove(0);
        diffTab.getChildren().add(difficultyImageView);
    }

    public void openTextEditor() {
        try {
            java.awt.Desktop.getDesktop().edit(new File("results/txt/" + book.getSubdirectory() + "/" + book.getName()
                    + " Results.jpeg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
