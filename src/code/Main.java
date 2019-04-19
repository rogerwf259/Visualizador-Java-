package code;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import helper.HelperMethods;
import helper.MessageBox;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Main extends Application {

    TextField url, token, refreshField;
    RadioButton rdoTV, rdoTotem;
    Stage stage;
    Scene scene2;
    WebEngine webEngine;
    BufferedImage capturedImage;
    Webcam webcam;
    ImageView camfeed;
    Image image;
    StackPane visualizador;
    Boolean isRunning = false, isCamRecording= false;
    VBox paneWeb;
    HttpURLConnection http;
    URL urlS;
    String oldValue = "";

    @Override
    public void start(Stage primarystage) throws Exception {
        //Obteniendo instancia de escena principal
        stage = primarystage;

        //Creacion de elementos que compondran la primer escena

        Text textHeader = new Text("CSSTI");
        textHeader.setFont(new Font(20));

        HBox paneLogo = new HBox(textHeader);
        paneLogo.setAlignment(Pos.CENTER);

        Label lblUrl = new Label("URL:");
        lblUrl.setPrefWidth(100);
        url = new TextField();
        url.setPrefColumnCount(15);
        url.setPromptText("URL a ingresar aqui");

        HBox paneUrl = new HBox(lblUrl, url);
        paneUrl.setSpacing(10);

        Label lblToken = new Label("Token:");
        lblToken.setPrefWidth(100);
        token = new TextField();
        token.setPrefColumnCount(10);
        token.setPromptText("Token de autenticacion aqui");

        HBox paneToken = new HBox(lblToken, token);
        paneToken.setSpacing(10);

        Label lblScreen = new Label("Tipo de Display:");
        rdoTV = new RadioButton("TV");
        rdoTotem = new RadioButton("Totem");
        rdoTV.setSelected(true);
        ToggleGroup screenSize = new ToggleGroup();
        rdoTV.setToggleGroup(screenSize);
        rdoTotem.setToggleGroup(screenSize);

        HBox paneTamaño = new HBox(lblScreen, rdoTV, rdoTotem);
        paneTamaño.setSpacing(10);


        Button requestButton = new Button();
        requestButton.setText("Send Request");
        requestButton.setOnAction(e -> sendRequest());

        HBox paneButton = new HBox(requestButton);


        //Creacion de los elementos de la segunda escena

        WebView webView = new WebView();
        webEngine = webView.getEngine();

        //Funciones Listener que rastrearan cambios en el Webview
        webEngine.titleProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, final String oldVal, final String newVal) {
                stage.setTitle(newVal);
            }
        });

        webEngine.getLoadWorker().stateProperty().addListener((observableValue, oldVal, newVal) -> {
            if (Worker.State.SUCCEEDED.equals(newVal)) {
                System.out.println("Done Loading");
                try {
                    http = (HttpURLConnection) urlS.openConnection();
                    oldValue = http.getHeaderField("Last-Modified");
                    System.out.println("Old Value:" +oldValue);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                initFetch();
                initFeed();
            }
        });

        paneWeb = new VBox(webView);
        paneWeb.setVgrow(webView, Priority.ALWAYS);
        webView.contextMenuEnabledProperty();
        //Creacion elementos a usar en capa Camara Web
        HBox cam = new HBox();
        cam.setAlignment(Pos.CENTER);
        camfeed = new ImageView();
        cam.setHgrow(camfeed, Priority.ALWAYS);
        camfeed.setFitWidth(cam.getWidth());
        camfeed.setFitHeight(cam.getHeight());
        cam.getChildren().add(camfeed);

        Region left = new Region();
        Region bottom = new Region();
        left.setPrefWidth(130);
        left.setPrefHeight(720);
        left.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        bottom.setPrefWidth(1080);
        bottom.setPrefHeight(120);
        bottom.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));

        BorderPane video = new BorderPane();
        video.setLeft(left);
        video.setBottom(bottom);
        video.setCenter(cam);

        //-------Inicializacion de variables y metodos de camara web
        webcam = Webcam.getDefault();
        webcam.setViewSize(WebcamResolution.VGA.getSize());

        Label refresh = new Label("Refresh:");
        refreshField = new TextField();
        refreshField.setPrefColumnCount(5);
        HBox refreshpane = new HBox(10,refresh, refreshField);


        VBox paneRequest = new VBox(10, paneUrl, paneToken, paneTamaño, refreshpane);

        BorderPane pane = new BorderPane();
        pane.setTop(textHeader);
        pane.setCenter(paneRequest);
        pane.setBottom(paneButton);

        visualizador = new StackPane(paneWeb, video);

        Scene scene = new Scene(pane);
        primarystage.setScene(scene);
        primarystage.setTitle("CSSTI | Visualizador");
        primarystage.show();
        primarystage.setOnCloseRequest(e -> {
            e.consume();
            btnClose_Click();
        });
    }
    public void sendRequest() {
        int width=0, height=0;
        String URLText = "";
        String errorMessage = "";
        if (url.getText().equals(""))
            errorMessage += "URL es un campo requerido\n";
        if (token.getText().equals(""))
            errorMessage += "Token es un campo requerido\n";
        if (!url.getText().equals("") && !token.getText().equals("")){
            if(HelperMethods.isMatch(url.getText())) {
                URLText += url.getText() + token.getText();
                System.out.println("URLText: " + URLText);
                if (rdoTV.isSelected()) {
                    width = 960;
                    height = 600;
                }
                if (rdoTotem.isSelected()) {
                    width = 960;
                    height = 600;

                }
                webEngine.load(URLText);
                webcam.open();
                scene2 = new Scene(visualizador, width, height);
                stage.setScene(scene2);
                stage.centerOnScreen();
                try {
                    urlS = new URL(URLText);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            } else {
                MessageBox.show("URL invalida", "Error");
            }
        } else {
            MessageBox.show(errorMessage,"Datos Faltantes");
        }
    }
    public void btnClose_Click() {
        if (isRunning) {
            isRunning = false;
        }
        if (isCamRecording) {
            isCamRecording = false;
        }
        stage.close();
    }

    public void initFetch() {
        Runnable task = () -> {
            isRunning = true;
            websiteChange();
        };
        Thread backgroundThread = new Thread(task);
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }

    public void websiteChange() {
        String newValue = "";
        while(isRunning) {
            try {
                http = (HttpURLConnection) urlS.openConnection();
                newValue = http.getHeaderField("Last-Modified");
                System.out.println("New Value: "+newValue);
                if (oldValue.equals(newValue)){
                    System.out.println("Same");
                } else {
                    System.out.println("Changed");
                    Platform.runLater(() -> webEngine.reload());
                }
                //int refresh = (Integer) refreshField.getText().;
                Thread.sleep(15000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    public void initFeed() {
        if (!isCamRecording) {
            isCamRecording = true;
            new VideoFeed().start();
        }
    }
    public class VideoFeed extends Thread {
        @Override
        public void run() {
            while(isCamRecording){
                try {
                    super.run();
                    capturedImage = webcam.getImage();
                    image = SwingFXUtils.toFXImage(capturedImage,null);
                    camfeed.setImage(image);
                    Thread.sleep(50);
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

