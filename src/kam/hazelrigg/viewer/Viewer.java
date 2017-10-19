package kam.hazelrigg.viewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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
        stage.getIcons().add(new Image(Viewer.class.getResourceAsStream("icons/bookshelf.png")));
        stage.show();
    }

}
