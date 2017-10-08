package kam.hazelrigg;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class Viewer extends Application{

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage primaryStage) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open file to analyse");

        ProgressBar bar = new ProgressBar();
        bar.setVisible(false);

        Book chosen = new Book();

        Text titleOfBook = new Text("Nothing has been selected...");

        Button openButton = new Button("Open file");

        Button analyseButton = new Button("Analyse file");
        analyseButton.setDisable(true);

        openButton.setOnAction((ActionEvent actionEvent) -> {
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                analyseButton.setDisable(false);
                chosen.setPath(file);
                chosen.setTitleFromText(file);
                titleOfBook.setText(chosen.getTitle() + " by " + chosen.getAuthor());
            }
        });

        analyseButton.setOnAction(actionEvent -> {
            if (chosen.getPath() != null) {
                chosen.analyseText();
                chosen.writeFrequencies();
                chosen.makePosGraph();
                chosen.makeDifficultyGraph();
                chosen.writeConclusion();
            }
        });

        HBox buttons = new HBox(6);
        buttons.getChildren().addAll(openButton, analyseButton);

        GridPane inputGridPane = new GridPane();

        inputGridPane.setHgap(6);
        inputGridPane.setVgap(6);

        GridPane.setConstraints(titleOfBook, 0, 0);
        GridPane.setConstraints(buttons, 0, 1);

        inputGridPane.getChildren().addAll(titleOfBook, buttons);
        inputGridPane.setPadding(new Insets(12, 12, 12, 12));


        primaryStage.setTitle("Word Count");
        primaryStage.setScene(new Scene(inputGridPane));
        primaryStage.show();

    }

}
