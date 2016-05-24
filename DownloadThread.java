package hu.ppke.itk.java2016.narkr.bead01;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Először is elkezdjük a megadott kezdőlink tartalmának letöltését (readStoreHtml függvény).
 * A letöltött tartalmat új Content objektumokban tároljuk le. Ezután a getLinks() metódussal kinyerjük a számunkra fontos
 * linkeket, majd ezeket a linkeket, ha nincsenek még a set-ben (mellyel ugye ismétlődést szűrünk), berakjuk a queue-ba.
 * A readStoreHtml processzál egy-egy, a queue elején levő linket, feldolgozza a tartalmát és a tartalomban talált
 * linkekkel is tovább gazdagítja a queuet.
 * Ez a megvalósítás igyekszik kezelni a relatív URL-eket. (a nagy részét abszolút linkekké alakítja).
 * Kezeli a redirectet (300, 301, 302 response code.) 400 és afölötti kódokra errort ír, de fut tovább.
 * Szerver hibakódokra (500 felett) szintén csak errort ír. 
 * Ha mindkét queue megüresedik, akkor a run() függvényből egyszerű return-nel dead állapotba mozgatom a szálakat.
 * Csak annyi linket szűrök ki az elején, amennyit a felhasználó beállított, ahogy az a specifikációban ki 
 * volt kötve. ("legfeljebb ennyi weblapot töltsön le:"
 * (További magyarázat az egyes függvényekhez tartozó kommentekben!)
 */

public class DownloadThread extends Thread{

	static WebCrawler associatedCrawler;
	Integer threadNum;
	/*
	 * A két queue-hoz a linkeket adom hozzá. Az egyikhez hozzáadok linkeket (producerUrlsQueue), míg a másikból kiveszem
	 * a feldolgozásra szánt url-t (consumerUrlsQueue). Két queue a mélység kezelésére/könnyebb láthatóság miatt 
	 * szükséges. Ha az első queue-m kiürült, akkor cserélem a queue-k tartalmát.
	 */
	static Queue <String> consumerUrlsQueue = new LinkedList<>();
	static Queue <String> producerUrlsQueue = new LinkedList<>();
	static HashSet <String> downloadedUrlsSet = new HashSet<>();
	
	/*
	 * currentContent = az éppen feldolgozás alatt levő tartalom. Lásd: Content objektum.
	 */
	static Content currentContent = new Content("startingContent");
	
	/*
	 * Az eddig letöltött Tartalom-objektumok.
	 */
	static ArrayList <Content> contents = new ArrayList<>();
	
	/*
	 * DownloadThread konstruktor
	 */
	public DownloadThread(WebCrawler crawler, Integer num){
		associatedCrawler = crawler;
		this.threadNum = num;
	}
	
	
	public Boolean readStoreHtml(){ 
		
			try {
				/*
				 * Kiveszem a feldolgozásra szánt URL-t a consumerUrlsQueue-ból. Új Content objektumot hozok létre.
				 * A Content objektumok String típusú ArrayList-ben tárolják a tartalmat.
				 */
				String urlName = consumerUrlsQueue.poll();
				currentContent = new Content(urlName);
				
				System.out.println("Starting to process: " + urlName);
				URL url = new URL(urlName);
				
				/*
				 * HttpURLConnection típusú connection szükséges a hibakódok kezelésére.
				 * RequestProperty-t is beállítok, hogy a felkérést a szerverek elfogadják.
				 */
				HttpURLConnection httpConnection = (HttpURLConnection)url.openConnection (); 
				httpConnection.setReadTimeout(5000);
				httpConnection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
				httpConnection.addRequestProperty("User-Agent", "Mozilla");
				httpConnection.addRequestProperty("Referer", "google.com");
				
				Integer code = null;
				Boolean redirect = false;
				try {
					
					code = httpConnection.getResponseCode();
					System.out.println("Code: " + code);
					
					if (code != HttpURLConnection.HTTP_OK){
						
						/*
						 * Redirect esetei
						 */
						if (code == HttpURLConnection.HTTP_MOVED_TEMP
								|| code == HttpURLConnection.HTTP_MOVED_PERM
									|| code == HttpURLConnection.HTTP_SEE_OTHER){
							redirect = true;
						}
						/*
						 * Szerver hibakód kezelése.
						 */
						if (code >= 500){
							System.err.println("Server error code. Reading url content terminate.");
							return false;
						}
						
						/*
						 * Az url-ek bizonyos százalékára 400-as kódot kaptam, megmagyarázhatatlan indokból, így 
						 * kezeltem le egyszerűen. (böngészőben nyithatóak voltak, és nem álltam neki wiresharkkal
						 * próbálkozni kideríteni az okot.
						 */
						if (code >=400){
							System.err.println("Bad request.");
							return false;
						}
					}
					
				}catch (IOException e){
					System.err.println("Host not responding.");
					return false;
				}
				
				if (redirect){
					/*
					 * Megszerzem a redirect által mutatott új URL "location"-jét, majd ez alapján új 
					 * HttpURLConnection-t állítok be.
					 */
					String newUrl = httpConnection.getHeaderField("Location");
					currentContent = new Content(newUrl);
					
					httpConnection = (HttpURLConnection) new URL(newUrl).openConnection();

					httpConnection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
					httpConnection.addRequestProperty("User-Agent", "Mozilla");
					httpConnection.addRequestProperty("Referer", "google.com");
											
					System.out.println("Redirect to URL : " + newUrl);
				}

				InputStream stream = httpConnection.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(stream));
					
				/*
				 * Kiolvasom a fájlt soronként, majd ha a "<body>" részt megtaláltam, kilépek a while ciklusból, hiszen mi
				 * csak a body részben szeretnénk keresni.
				 */
				String str = "";
				Pattern p = Pattern.compile("<body.*>", Pattern.CASE_INSENSITIVE);
				
				while ((str = br.readLine()) != null){
					Matcher m = p.matcher(str);
					if (m.find()){ break; }
				}
				
				/*
				 * Én ezt az értékes szöveget egyben szeretném kezelni.
				 * Beolvasom a fájlt, soronként. Külön egységeket képezek a sorokból, új egységet csak
				 * akkor nyitok, ha a sor '>' -re végződik.
				 * 
				 * Ezt az esetet szeretném ezzel kiküszöbölni:
				 * 
				 * ......................................................... >ez itt egy értékes szöveg, melyet
				 * egységként kéne kezelni kereséskor, de mégis a mondat közepén törtünk meg<..................
				 */
				
				String lineUnit = "";
				while ((str = br.readLine()) != null){
					lineUnit += str;
					if (str.endsWith(">")){
						currentContent.getLinesList().add(lineUnit);
						lineUnit = "";
					}
				}
				
				/*
				 * Ezután továbbtördelem az eddigi egységeimet. Eltávolítom a tageket, helyettesítem őket egy sortörés
				 * kifejezéssel(line break - \n). Ezután az egységet széttördelem (split) a sortörés helyein és tördelten
				 * teszem bele a végső, unitsList nevű arrayslistbe. A keresésnél ezzel fogok dolgozni. 
				 */
				
				for(String line: currentContent.getLinesList()){
					
					line = line.replaceAll("<[^>]*>", "\n");
					line = line.replaceAll("[\n]+", "\n");
					
					line.trim();
					List <String> separateLines = Arrays.asList(line.split("\\n")); 
					
					currentContent.getUnitsList().addAll(separateLines);
				}
				
				currentContent.getUnitsList().removeAll(Arrays.asList(null, ""));
				currentContent.writeToFile();
				
				/*
				 * Az újonnan kinyert Content objektumot a contents listához hozzáadom. 
				 * Így könnyen ellenőrizhető, hogy hány URL HTML tartalmát sikerült letöltenem.
				 */
				synchronized (contents){
					contents.add(currentContent);
				}
				
				br.close();
			} catch (FileNotFoundException e){
				System.out.println("File not found.");
				return false;
				
			} catch (MalformedURLException e){
				System.out.println("Content read - Malformed URL");
				return false;
				
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Content read - IOException");
				return false;
			}
		
		return true;
	}
	
	public void getLinks(){
			
			/*
			 * Megszerzem az éppen processzált Content objektumban tárolt tartalmat, melyet a readStoreHtml-el nyertem ki.
			 */
			List<String> list = currentContent.getLinesList();
			System.out.println("The base URL: " + currentContent.getThreadName());
			
			/*
			 * A baseUrl a relatív linkekből abszolút előállításhoz lesz szükséges.
			 */
			URL baseUrl = null;
			try {
				baseUrl = new URL(currentContent.getThreadName());
			} catch (MalformedURLException e) {
				System.err.println("BaseUrl MalformedException");
			}
			
			/*
			 * A linkek szűrésére használt pattern:
			 */
			Pattern hrefPattern = Pattern.compile("href=\"(.*?)\"");
			
			Boolean found = false;
			for (String line : list){

				Matcher linkMatcher = hrefPattern.matcher(line);
				
				while (linkMatcher.find()){
					found = true;
					String foundUrl = linkMatcher.group(1).trim();
					System.out.println("Found a possible candidate URL: " + foundUrl);
					
					/*
					 * Innentől pedig a relative URL-ek abszolúttá történő változtatása következik.
					 * Több forrás alapján(több gyakorlatvezetőt is megkérdeztek az osztálytársaim), nem volt egyértelmű,
					 * hogy erre a lépésre szükség van-e, vagy elég lenne abszolút linkeket kezelni.
					 * Hát, itt van végül is egy megoldás:
					 */
					
					// Skip empty links.
					if (foundUrl.length() < 1) {continue;}
					
					//Skip links that represent the current directory
					if (foundUrl.startsWith(".")){continue;}
					
					// Skip links that are just page anchors.
					if (foundUrl.charAt(0) == '#') {continue;}
					
					// Skip mailto links.
					if (foundUrl.indexOf("mailto:") != -1) {continue;}
					
					// Skip JavaScript links.
					if (foundUrl.toLowerCase().indexOf("javascript") != -1) {continue;}
					
					// Remove anchors from link.
					int index = foundUrl.indexOf('#');
					if (index != -1) {
						foundUrl = foundUrl.substring(0, index);
					}
					
					/*
					 * Relative to absolute (starting with '//')
					 * Example: //yourdomain.com/images/example.png
					 */
					if (foundUrl.startsWith("//")){
						foundUrl = baseUrl.getProtocol() + ":" + foundUrl;
						//System.out.println("made an absolute url (//): " + foundUrl);
						
					}
					
					/*
					 * Relative to absolute (starting with '/') or not starting with any protocol
					 * Examples:
					 * /images/example.png
					 * images/example.png
					 */
					
					if (foundUrl.startsWith("/")){
						foundUrl = baseUrl.getProtocol() + "://" + baseUrl.getHost() + foundUrl;
						
					}else if (!foundUrl.startsWith("http")){
						foundUrl = baseUrl.getProtocol() + "://" + baseUrl.getHost() + "/" + foundUrl;
						
					}
					
					
					try {
						@SuppressWarnings("unused")
						URL Url = new URL(foundUrl);
					} catch (MalformedURLException e) {
						System.out.println("foundUrl malformed");
						return;
					}
					
					/*
					 * A slash-ek ('/') mentén szétválasztom a talált URL-t, majd veszem az utolsó tömbrészletet, melyben
					 * a kiterjesztés is megtalálható. Ez a pont/dot mentén választom szét. Mindez a kiterjesztések
					 * könnyebb kezelése érdekében szükséges.
					 */
						if (foundUrl.startsWith("http")){
							
							System.out.println("success: " + foundUrl);
							
							String[] trimmedArray = foundUrl.split("/");
							String extString = trimmedArray[trimmedArray.length - 1];
							String[] dotArray = extString.split("\\.");
							
							String extension = dotArray[dotArray.length - 1];
							
							if (dotArray.length == 1 || extension.equals("asp") || extension.equals("html") || extension.equals("htm") || 
									extension.equals("php")){
								
								/*
								 * Ha megengedett kiterjesztések egyikét tartalmazta az URL, vagy nem volt kiterjesztése, akkor
								 * megvizsgálom, hogy a set már tartalmaz-e ilyen bejegyzést. Ha nem, akkor a Queue-hoz hozzáadható.
								 */
								if (!downloadedUrlsSet.contains(foundUrl)){
									downloadedUrlsSet.add(foundUrl);
									producerUrlsQueue.add(foundUrl);
									System.out.println("Added: " + foundUrl.toString());
								}		
							}
						}

					}
				} 
			
			if (!found){
				System.out.println("Linkek kigyűjtése: nincs találat.");
			}
			
		}
	
	/*
	 * Ha az a Queue már üres, amiből fetchelek, akkor megcserélem a két queue tartalmát.
	 */
	private static void switcher(){
		
		if (!producerUrlsQueue.isEmpty() && consumerUrlsQueue.isEmpty()){
			switchArrayElements();
		}
	}
	
	private static void switchArrayElements(){
		Queue <String> temp = new LinkedList<>();
		temp.addAll(consumerUrlsQueue);
		consumerUrlsQueue.clear();
		consumerUrlsQueue.addAll(producerUrlsQueue);
		producerUrlsQueue.clear();
		producerUrlsQueue.addAll(temp);
	}

	
	@Override
	public void run(){
		
			while (contents.size() < associatedCrawler.getDownloadsNum()){
			
				/*
				 * Ha mindkét Queue üres, akkor a run-ból returnt hívva végzek a Thread-del.
				 */
				if (consumerUrlsQueue.size() == 0 && producerUrlsQueue.size() == 0){
					System.out.println("stopped.");
					if (contents.size() >= associatedCrawler.getDownloadsNum()){
						System.out.println("mission completed");
					}
					return;
				}
				
				/*
				 * A readStoreHtml egy Boolean függvény, ha valamilyen okból a tartalom beolvasása sikertelen
				 * (visszakapott hibakód, URL nem hozható létre, a szerveren a lekért tartalmat nem sikerül letölteni),
				 * akkor error kiírása után addig próbálkozik, míg a letöltést végbe nem megy.
				 */
				synchronized (consumerUrlsQueue){
					synchronized(currentContent){
						while (!readStoreHtml() && consumerUrlsQueue.size() > 0){
							System.err.println("ERROR HANDLED. RETRY");
						}
					}
				}
				
				/*
				 * Csak akkor tölt le további linkeket, ha a felhasználó által megadott korlátot még nem értük el.
				 */
				if (downloadedUrlsSet.size() < associatedCrawler.getDownloadsNum()){
					synchronized (producerUrlsQueue){
						synchronized (downloadedUrlsSet) {
							synchronized(currentContent){
								getLinks();
							}
						}
					}
				}
				
				synchronized (producerUrlsQueue){
					synchronized (consumerUrlsQueue) {
						switcher();
					}
				}
				
				synchronized (downloadedUrlsSet){
					System.out.println("---------------------------SET-----------------------");
					System.out.println("set mérete: " + downloadedUrlsSet.size());
					
					System.out.println("--------------------------PRODUCER QUEUE--------------");
					System.out.println("producer mérete: " + producerUrlsQueue.size());
					
					System.out.println("-------------------------CONSUMER QUEUE---------------");
					System.out.println("consumer mérete: " + consumerUrlsQueue.size());
					
					System.out.println("-------------------------CONTENTS ARRAY--------------");
					System.out.println("contents mérete: " + contents.size());
					System.out.println("------------------------------------------------------");
					
					System.out.println(threadNum + ". thread voltam. ");
				}
			
			}
			
			System.out.println("completed mission");
		}

}
