package hu.ppke.itk.java2016.narkr.bead01;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Content {
	String threadName;
	List <String> linesList = new ArrayList<>(); 
	List <String> unitsList = new ArrayList<>();
	
	static String filename = "DOCUMENTS.txt";
	
	public Content(String name){
		this.threadName = name;
	}

	public String getThreadName() {
		return threadName;
	}

	public List<String> getLinesList() {
		return linesList;
	}

	public List<String> getUnitsList() {
		return unitsList;
	}

	/*
	 * Ez a függvény inkább debugolásra született.
	 * Egy darab dokumentumhoz toldozgatom hozzá a kinyert tartalmakat. Főleg azért, hogy ellenőrizni lehessen
	 * a tartalom kiszűrését.
	 */
	public void writeToFile(){
		
		try
		{
		    FileWriter fw = new FileWriter(filename,true);
		    fw.write("THREADNAME: " + threadName + "\n");
		    fw.write("unitslist sorainak száma: " + unitsList.size() + "\n");
		    for (String u: unitsList){
		    	fw.write(u);
		    }
		    fw.write("\n\n-----------------------------------------------------------------------------------");
		    fw.close();
		}
		catch(IOException ioe)
		{
		    System.err.println("IOException: " + ioe.getMessage());
		}
	}

}
