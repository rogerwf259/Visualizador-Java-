package code;

import borderless.BorderlessScene;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import helper.Config;
import helper.HelperMethods;
import helper.MessageBox;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
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
import javafx.stage.StageStyle;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Main extends Application {

    TextField url, width, height;
    PasswordField token;
    Button back;
    BorderlessScene sc;
    BorderPane video, pane, glass;
    Stage stage;
    WebEngine webEngine;
    BufferedImage capturedImage;
    Webcam webcam;
    ImageView camfeed;
    Image image;
    StackPane visualizador;
    Boolean isRunning = false, isCamRecording= false, isMeasuring=true;
    VBox paneWeb;
    HttpURLConnection http;
    URL urlS;
    String oldValue = "", uText="", tText="";
    Double wP = 400.0, hP=400.0;
    Config config = new Config();
    CheckBox checkBox;

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
        url = new TextField(uText);
        url.setPrefColumnCount(15);
        url.setPromptText("URL a ingresar aqui");

        HBox paneUrl = new HBox(lblUrl, url);
        paneUrl.setSpacing(10);

        Label lblToken = new Label("Token:");
        lblToken.setPrefWidth(100);
        token = new PasswordField();
        token.setText(tText);
        token.setPrefColumnCount(10);
        token.setPromptText("Token de autenticacion aqui");

        HBox paneToken = new HBox(lblToken, token);
        paneToken.setSpacing(10);


        Label lblTamaño = new Label("Medidas:");
        lblTamaño.setPrefWidth(50);
        width = new TextField();
        width.setPrefColumnCount(10);
        height = new TextField();
        height.setPrefColumnCount(10);
        HBox paneTamaño = new HBox(lblTamaño, width, height);
        paneTamaño.setSpacing(10);

        Label lblRecuerda = new Label("Guardar datos?");
        checkBox = new CheckBox();
        HBox paneGuarda = new HBox(10, lblRecuerda, checkBox);


        Button requestButton = new Button();
        requestButton.setText("Send Request");
        requestButton.setOnAction(e -> sendRequest());

        Button cancelButton = new Button();
        cancelButton.setText("Cancelar");
        cancelButton.setOnAction(e -> cancelButton_Click());

        Region buttonSpace = new Region();
        buttonSpace.setPrefWidth(250);

        HBox paneButton = new HBox(10, requestButton, buttonSpace, cancelButton);


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

        video = new BorderPane();
        video.setLeft(left);
        video.setBottom(bottom);
        video.setCenter(cam);

        //-------Inicializacion de variables y metodos de camara web
        webcam = Webcam.getDefault();
        webcam.setViewSize(WebcamResolution.VGA.getSize());

        glass = new BorderPane();
        back = new Button("Back");
        back.setAlignment(Pos.TOP_LEFT);
        back.setOnAction(e -> back_click());
        glass.setTop(back);


        visualizador = new StackPane(paneWeb, video, glass);
        



        VBox paneRequest = new VBox(10, paneUrl, paneToken, paneTamaño, paneGuarda);

        pane = new BorderPane();
        pane.setPrefWidth(wP);
        pane.setPrefHeight(hP);
        pane.setTop(textHeader);
        pane.setCenter(paneRequest);
        pane.setBottom(paneButton);
        pane.setPadding(new Insets(10));

        sc = new BorderlessScene(primarystage, pane);
        //Scene scene = new Scene(pane);
        primarystage.setScene(sc);
        sc.setMoveControl(textHeader);
        primarystage.setTitle("CSSTI | Visualizador");
        primarystage.setOnCloseRequest(e -> {
            e.consume();
            btnClose_Click();
        });
        primarystage.setOnShowing(windowEvent -> {
            measuresUpdate();
        });
        pane.sceneProperty().addListener((observableValue, oldScene, newScene) -> {
            if (newScene == null){
                isMeasuring = false;
            }
            else {
                measuresUpdate();
            }
        });
        primarystage.show();
    }

    @Override
    public void init() throws Exception {
        super.init();
        File file = new File("config.ini");
        if (file.exists()){
            uText = config.readProperties("URL");
            tText = config.readProperties("Token");
            wP = Double.valueOf(config.readProperties("Width"));
            hP = Double.valueOf(config.readProperties("Height"));
        }
        System.out.println("CONFIG: "+file.exists());
    }

    public void sendRequest() {
        String URLText = "";
        String errorMessage = "";
        if (url.getText().equals(""))
            errorMessage += "URL es un campo requerido\n";
        if (token.getText().equals(""))
            errorMessage += "Token es un campo requerido\n";
        if (!url.getText().equals("") && !token.getText().equals("")){
            if(HelperMethods.isMatch(url.getText())) {
                URLText += url.getText()+token.getText();
                //URLText = insertToken(url.getText(), token.getText());
                System.out.println("URLText: " + URLText);
                if (checkBox.isSelected()){
                    saveProperties(url.getText(), token.getText(), sc.getWidth(), sc.getHeight());
                }
                webEngine.load(URLText);
                webcam.open();
                sc.setContent(visualizador);
                sc.setMoveControl(glass);
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
    public void saveProperties(String url, String token, Double width, Double height){
        config.saveProperty("URL", url);
        config.saveProperty("Token", token);
        if (width != null && height != null){
            config.saveProperty("Width", width.toString());
            config.saveProperty("Height", height.toString());
        }
    }

    public String insertToken(String base, String tkn){
        return (base.substring(0, base.indexOf('*'))+tkn+base.substring(base.lastIndexOf('*')+1));
    }

    public void cancelButton_Click() {
        isMeasuring = false;
        stage.close();
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

    public void measuresUpdate() {
        Runnable mUpdate = () -> measure();
        Thread measuresBackground = new Thread(mUpdate);
        measuresBackground.setDaemon(true);
        measuresBackground.start();
    }

    public void measure() {
        while (isMeasuring){
            Platform.runLater(() -> {
                width.setText(String.valueOf(pane.getWidth()));
                height.setText(String.valueOf(pane.getHeight()));
            });
            try {
                Thread.sleep(100);
            }catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void back_click() {
        webcam.close();
        isRunning = false;
        isCamRecording = false;
        sc.setContent(pane);
        isMeasuring = true;
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

