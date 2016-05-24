package hu.ppke.itk.java2016.narkr.bead01;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;

public class SearchFunctions implements Runnable{
	
	static String currentContent;
	private final String searchWord;
	private final int frameLength;
	
	public SearchFunctions(String searchWord, int wordFrameLength){
		this.searchWord = searchWord;
		this.frameLength = wordFrameLength;
	}
	
	/*
	 * Regexek segítségével elvégzem a szavak kikeresését. 
	 * Két regexet használok: simplePattern és groupPattern.
	 * A simplePatternt az alapján a kifejezés (searchWord) alapján hozom létre, amelyet a felhasználó adott meg.
	 * 
	 * Ha az unitslist egy-egy sorában rábukkanok a kifejezésre, akkor lekérem az adott nagyságú környezetét.
	 * Majd erre egy újabb pattern-t matchelek (groupPattern), hogy könnyen kezelni tudjam
	 * a szavat megelőző karaktereket, magát a szavat és a szó utáni karaktereket is.
	 * Azért gondoltam erre a megoldásra, mert így az az eset is megoldható, ha egy-egy sorban többször is 
	 * előfordul az adott kifejezés.
	 */
	
	public static String findMatches(String term, int frame, List<String> unitsList){
		
		String finalMatch = "";
		StringBuilder stringBuilder = new StringBuilder();
		
		Pattern simplePattern = null;
		try {
			simplePattern = Pattern.compile(term, Pattern.CASE_INSENSITIVE);
		} catch (Exception e) {
			return "Nem megfelelő keresőszó!"; 
		}
		
		Pattern groupPattern = Pattern.compile("(.*)(" + term + ")(.*)", Pattern.CASE_INSENSITIVE);
		
		Boolean found = false;
		int startFrame = 0; 
		int endFrame = 0;
		
		for (String line: unitsList){
			Matcher m = simplePattern.matcher(line);
			
			while (m.find()){
				
				found = true;
				
				startFrame = 0; 
				endFrame = line.length();
				if (m.start() > frame){
					startFrame = m.start() - frame;
				}
				if (m.end() + frame < line.length()){
					endFrame = m.end() + frame;
				}
				
				Matcher limitedMatcher = groupPattern.matcher(line.substring(startFrame, endFrame));
				
				while (limitedMatcher.find()){
					
					stringBuilder.append(currentContent + ": ..." + limitedMatcher.group(1) + limitedMatcher.group(2) + limitedMatcher.group(3) + "...\n");				
				}	
			}
		}
		
		finalMatch = stringBuilder.toString();
		
		if (!found){
			finalMatch = "Nincs találat a(z) " + term + " kifejezésre.\n";
		}
		
		return finalMatch;
	}

	@Override
	public void run() {
		
		StringBuilder matchesText = new StringBuilder();
		for (Content c : DownloadThread.contents){
			currentContent = c.getThreadName().substring(0, 15);
			matchesText.append(findMatches(searchWord, frameLength, c.getUnitsList()));
		}
		
		String finalString = matchesText.toString();
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				GraphicalInterface.changePane(finalString);
			}	
		});
		
	}

}
