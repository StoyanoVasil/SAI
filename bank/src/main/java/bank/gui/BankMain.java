package bank.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BankMain extends Application {

    private static String name;
    private static String queue;

    @Override
    public void start(Stage primaryStage) throws Exception{
        // load FXML file is in bank/src/main/resources/bank.fxml
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/bank.fxml"));
        Parent root = loader.load();

        // give bank info to controller
        BankController controller = loader.getController();
        controller.initData(name, queue);

        primaryStage.setTitle("BANK - " + name);
        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });

        primaryStage.setScene(new Scene(root, 500,300));
        primaryStage.show();
    }


    public static void main(String[] args) {
        name = args[0];
        queue = args[1];
        launch(args);
    }
}
