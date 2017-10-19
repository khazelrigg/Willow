package kam.hazelrigg.viewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Viewer extends Application{

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage stage) throws Exception {
        int width = 900;
        int height = 700;

        stage.setTitle("Word Count");
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("wordCount.fxml")), width, height));
        //TODO set stage icon - https://stackoverflow.com/questions/10121991/javafx-application-icon#10122335
        stage.show();
    }

}
