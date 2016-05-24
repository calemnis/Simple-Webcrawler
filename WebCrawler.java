package hu.ppke.itk.java2016.narkr.bead01;

public class WebCrawler extends Thread{
	
	String startingUrl;
	Integer downloadsNum, threadsNum;
	
	public WebCrawler(String startingUrl, Integer downloadsNum, Integer threadsNum){
		this.startingUrl = startingUrl;
		this.downloadsNum = downloadsNum;
		this.threadsNum = threadsNum;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 * 
	 * Két queue-val és egy hashsettel implementáltam a weboldalak látogatását és onnan a címek letöltését.
	 * A kezdő url-t hozzáadom a downloadedUrlsSet nevű hashset-hez. Ez azért szükséges, hogy ne legyenek ismétlődő 
	 * weboldal címek és ezáltal tartalmak.
	 * Ezután létrehozom a megfelelő számú DownloadThread threadet, ami a weboldalak tartalmának letöltését és
	 * onnan a linkek kinyerését valósítja meg.
	 */
	@Override
	public void run(){
		
		System.out.println("added starting url: " + startingUrl);
		DownloadThread.downloadedUrlsSet.add(startingUrl);
		DownloadThread.consumerUrlsQueue.add(startingUrl);
			
		Integer currentThreadNum = 0;
			
		while (currentThreadNum < threadsNum){
			System.out.println("current num: " + currentThreadNum);
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				System.out.println(currentThreadNum + ". thread was interrupted.");
			}
			Thread downloadThread = new Thread(new DownloadThread(this, currentThreadNum + 1));
			downloadThread.start();
			currentThreadNum++;
		}
	}
	
	public Integer getDownloadsNum() {
		return downloadsNum;
	}
	
	public Integer getThreadsNum() {
		return threadsNum;
	}
}
