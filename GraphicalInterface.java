package hu.ppke.itk.java2016.narkr.bead01;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import java.net.MalformedURLException;
import java.net.URL;


/*
 * Bővebb magyarázat : WebCrawler osztály elején.
 */
public class GraphicalInterface extends Application{
	
	GridPane UpperGridLayout;
	static StackPane TextAreaLayout;
	
	private Integer wordFrameLength = null;
	static Thread searchThread = null;
	

	public static void main(String[] args) {
		launch(args);
		
	}
	
	/*
	 * Egy BorderPane osztott panel fog a GUI kinézetének alapjául szolgálni.
	 * A GridPane rácsokra osztható panel pedig a gombokat(buttons), szövegmezőket (text fields)
	 * és a Spinnereket fogja tárolni. A Spinner egy nyilakkal állítható mező, mely egy szám értékét változtathatja.
	 * Az eredmény-szöveg, mellyel majd visszatérünk egy StackPane-ben, azon belül egy label-ben lesz kezelve.
	 */
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Kereső - [Preview]");
	
		BorderPane borderPane = new BorderPane();
		borderPane.setPadding(new Insets(10,10,20,10));
		
		UpperGridLayout = new GridPane();
        UpperGridLayout.setVgap(8); UpperGridLayout.setHgap(10);
        setGridElements(UpperGridLayout);
		
		TextAreaLayout = new StackPane();
		TextAreaLayout.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 10;");
		TextAreaLayout.getChildren().add(new Pane());
		TextAreaLayout.setAlignment(Pos.BASELINE_LEFT);
		
		borderPane.setTop(UpperGridLayout);
		borderPane.setCenter(TextAreaLayout);
		BorderPane.setMargin(TextAreaLayout, new Insets(10, 10, 0, 10));
		        
		Scene scene = new Scene(borderPane, 760, 500);
		primaryStage.setScene(scene);
		primaryStage.centerOnScreen();
		primaryStage.show();
	}

	private void setGridElements(GridPane grid){
		       
        /*
         * Egyszerű szöveges mezők - label használatával.
         * Minden objektumot a GridPane panelnek adok át.
         */
		Label urlLabel = new Label("Kezdeti URL:");
        GridPane.setConstraints(urlLabel, 0, 0);
        Label searchWordLabel = new Label("Keresendő szó:");
        GridPane.setConstraints(searchWordLabel, 0, 2);
        
        /*
         * Ún. prompt field-ek, melyekkel majd bekérem a felhasználó által megadott adatokat.
         */
        TextField urlTextPromptField = new TextField();
        urlTextPromptField.setPromptText("http://gui.example");
        urlTextPromptField.setOnAction(e -> {

        });
        GridPane.setConstraints(urlTextPromptField, 1, 0);
        
        TextField searchWordPromptField = new TextField();
        searchWordPromptField.setPromptText("Java");
        searchWordPromptField.setOnAction(e -> {
        	//searchWord = searchWordPromptField.getText();
        });
        GridPane.setConstraints(searchWordPromptField, 1, 2);
        
        /*
         * Egyszerű szöveges mezők - label használatával.
         */
        Label downloadLabel = new Label("Legfeljebb ennyi weblapot töltsön le:");
        GridPane.setConstraints(downloadLabel, 2, 0);
        Label threadNumberLabel = new Label("Ennyi szálon fusson a letöltés:");
        GridPane.setConstraints(threadNumberLabel, 2, 1);
        Label wordFrameLabel = new Label("Ekkora környezetét írja ki a találatnak:");
        GridPane.setConstraints(wordFrameLabel, 2, 2);
        
        /*
         * A számszerű mezők értékének megadásához Spinnereket hoztam létre.
         * Eventhandler helyett Listenert használok az eseménykezelésre.
         */
        Spinner<Integer> downloadSpinner = new Spinner<Integer>();
        downloadSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 200, 50));
        downloadSpinner.setMaxSize(80, 50);
        GridPane.setConstraints(downloadSpinner, 3, 0);
        
        Spinner<Integer> threadSpinner = new Spinner<Integer>();
        threadSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 4));
        threadSpinner.setMaxSize(80, 50);
        GridPane.setConstraints(threadSpinner, 3, 1);
        
        Spinner<Integer> wordFrameSpinner = new Spinner<Integer>();
        wordFrameSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 40, 10));
        wordFrameLength = wordFrameSpinner.getValue();
        wordFrameSpinner.setMaxSize(80, 50);
        wordFrameSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
        	setWordFrameLength(newValue);
        });
        GridPane.setConstraints(wordFrameSpinner, 3, 2);
        
        /*
         * A letöltés és a keresés funkciókat kezelő gombok:
         */
        Button downloadButton = new Button("Letöltés");
        downloadButton.setMinWidth(90);
        downloadButton.setOnAction(e -> {
        	
        	if (!urlTextPromptField.getText().equals("") && isValidUrl(urlTextPromptField.getText())){
        		Thread crawler = new Thread(new WebCrawler(urlTextPromptField.getText(), downloadSpinner.getValue()
        				, threadSpinner.getValue()));
        		crawler.start();
        	}
        });
        GridPane.setConstraints(downloadButton, 4, 1);
        
        Button searchButton = new Button("Keresés");
        searchButton.setMinWidth(90);
        searchButton.setOnAction(e -> {
        	
				if (!searchWordPromptField.getText().equals("")  && wordFrameLength != null){
					
					System.out.println("text: " + searchWordPromptField.getText());
					searchThread = new Thread(new SearchFunctions(searchWordPromptField.getText(), wordFrameLength));
					searchThread.setDaemon(true);
					searchThread.start();
				}     	
        });
        GridPane.setConstraints(searchButton, 4, 2);
        
        /*
         * Végül az összes kisebb objektum hozzáadása a gridpanelhez.
         */
        grid.getChildren().addAll(urlLabel, searchWordLabel, urlTextPromptField, searchWordPromptField, 
        		downloadLabel, threadNumberLabel, wordFrameLabel, 
        		downloadSpinner, threadSpinner, wordFrameSpinner,
        		downloadButton, searchButton);

	}

	public static void changePane(String results){
		Label searchResults = new Label(results);
		TextAreaLayout.getChildren().clear();
		TextAreaLayout.getChildren().add(searchResults);
	}
	
	public void setWordFrameLength(Integer wordFrameLength) {
		this.wordFrameLength = wordFrameLength;
	}
	
	public Boolean isValidUrl (String urlCandidate){

		try {
			@SuppressWarnings("unused")
			URL url = new URL(urlCandidate);
		} catch (MalformedURLException e) {
			changePane("A megadott URl nem érvényes. Próbáld újra.");
			return false;
		}
		return true;
	}

}
