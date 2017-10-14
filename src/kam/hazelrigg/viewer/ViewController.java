package kam.hazelrigg.viewer;

import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kam.hazelrigg.Book;
import kam.hazelrigg.WordCount;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ViewController implements Initializable{

    public ToggleButton writeDocument;
    public ToggleButton posCharts;
    public ToggleButton diffCharts;
    public ScrollPane center;
    public ImageView graph;
    public ImageView difficulty;
    public Label statusLabel;

    private Book book = new Book();
    private Stage chooserPane = new Stage();

    private void updateStatusLabel(String text) {
        statusLabel.setText(text);
    }

    public void openFile() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(chooserPane);
        if (file != null) {
            book.setPath(file);
            book.setTitleFromText(file);
            updateStatusLabel("Opened " + book.getTitle());
        }
    }


    public void openFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File dir = directoryChooser.showDialog(chooserPane);
        if (dir != null) {
            book.setPath(dir);
            updateStatusLabel("Opened " + dir.getName());
        }
    }

    public void run() {
        if (book.getPath().isDirectory()) {
            updateStatusLabel("Analysing dir: " + book.getPath());
            WordCount.openDirectory(book.getPath());
        } else {
            updateStatusLabel("Analysing file: " + book.getTitle());
            book.analyseText();

            if (writeDocument.isSelected()) {
                book.writeFrequencies();
                readFileToCenter(new File("results/txt/" + book.getTitle() + " by " + book.getAuthor() + " Results.txt"));
            }

            if (posCharts.isSelected()) {
                book.makePosGraph();

                System.out.println("results/img/"
                        + book.getTitle() + " by " + book.getAuthor()
                        + " POS Distribution Results.jpeg");

                Image posGraph = new Image("file:results/img/"
                        + book.getTitle() + " by " + book.getAuthor()
                        + " POS Distribution Results.jpeg");

                graph.setImage(posGraph);
            }

            if (diffCharts.isSelected()) {
                book.makeDifficultyGraph();
                Image diffGraph = new Image("file:results/img/"
                        + book.getTitle() + " by " + book.getAuthor()
                        + " Difficulty Results.jpeg");
                difficulty.setImage(diffGraph);
            }

            updateStatusLabel("Displaying results for: " + book.getTitle());
        }
    }

    private void readFileToCenter(File result) {
        Text text = new Text("");
        if (result.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(result))) {
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
        center.setContent(text);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        writeDocument.setSelected(true);
        posCharts.setSelected(true);
        diffCharts.setSelected(true);
    }

}
