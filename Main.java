import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.io.*;
import java.lang.reflect.Type;
import java.net.*;

import javax.xml.ws.Response;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

public class Main {
	
	private static final String devKey = "";
	private static final String authKey = "";
	private static String urlPrefix = "http://api.smitegame.com/smiteapi.svc/";
	private static String timeStamp;
	private static String signature;
	private static String session;
	private static Gson gson = new Gson();
	private static Map<String, ArrayList<String[]>> godItems = new HashMap<String, ArrayList<String[]>>();
	private static Map<String, String[]> topBuilds = new HashMap<String, String[]>();
	
	private static String createTimeStamp() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyMMddHHmmss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String formattedDate = sdf.format(date);
		return formattedDate;
	}
	
	private static String getMD5Hash(String input) throws Exception {
		byte[] byteArray = input.getBytes("UTF-8");
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] theDigest = md.digest(byteArray);
		StringBuilder sb = new StringBuilder(theDigest.length * 2);
		
		for(byte b: theDigest) {
			sb.append(String.format("%02x", b));
		}
		
		return sb.toString().toLowerCase();
	}
	
	private static String createSession(String input) throws Exception {
		StringBuilder content = new StringBuilder();
		URL oracle = new URL(input);
		URLConnection yc = oracle.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
		String inputLine;
		while((inputLine = in.readLine()) != null)
				content.append(inputLine);
		in.close();
		String finalStr = content.toString();
		//System.out.println(finalStr);
		Type stringStringMap = new TypeToken<Map<String, String>>(){}.getType();
		Map<String, String> map = gson.fromJson(finalStr, stringStringMap);
		//System.out.println(map);
		return(map.get("session_id"));
	}
	
	
	private static void makeRequest(String input) throws Exception {
		StringBuilder content = new StringBuilder();
		URL oracle = new URL(input);
		URLConnection yc = oracle.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream(), "UTF-8"));
		String inputLine;
		while((inputLine = in.readLine()) != null) {
				content.append(inputLine);
		}
		in.close();
		String finalStr = content.toString();
		
		Type type = new TypeToken<ArrayList<HashMap<String, String>>>(){}.getType();
		ArrayList<HashMap<String, String>> map = gson.fromJson(finalStr, type);
		
		for (HashMap<String, String> entry : map) {
			if(entry.get("Queue").equals("Leagues: Conquest")) {
				String[] items = new String[8];
				for(int i = 0; i < 8; i++) {
					switch(i) {
						case 0: items[i] = entry.get("Item_1");
					        	break;
						case 1: items[i] = entry.get("Item_2");
								break;
						case 2: items[i] = entry.get("Item_3");
								break;
						case 3: items[i] = entry.get("Item_4");	
								break;
						case 4: items[i] = entry.get("Item_5");
								break;
						case 5: items[i] = entry.get("Item_6");
								break;
						case 6: items[i] = entry.get("Active_1");
								break;
						case 7: items[i] = entry.get("Active_2");
								break;
					}//switch
					if(items[i].isEmpty()) {
						items[i] = "No Item";
					}
				}//for each item
				ArrayList<String[]> list = godItems.get(entry.get("God"));
				if(list == null) {
					list = new ArrayList<String[]>();
					list.add(items);
					godItems.put(entry.get("God"), list);
				} else {
					list.add(items);
				}
			}
	    }//for each God
		
		Map<String, ArrayList<String[]>> sortedGods = new TreeMap<String, ArrayList<String[]>>(godItems);
		godItems = sortedGods;
	}
	
	public static void calculateBuilds() {
		for (Map.Entry<String, ArrayList<String[]>> entry : godItems.entrySet()) {
			Map<Integer, ArrayList<String>> itemSlots = new HashMap<Integer, ArrayList<String>>(); //Items for each slot for this one specific god
			for(String[] itemList : entry.getValue()) {
				for(int itemIndex = 0; itemIndex < 8; itemIndex++) {
					if(itemSlots.get(itemIndex) == null) {
						itemSlots.put(itemIndex, new ArrayList<String>());
					}
					if(!itemList[itemIndex].equals("No Item")) {
						itemSlots.get(itemIndex).add(itemList[itemIndex]);
					}
					if(itemList[itemIndex].equals("No Item")) {
						itemSlots.get(itemIndex).add("No buys");
					}
				}
				//Sort items then add for this god's build
			}
			
			String fileName = entry.getKey()+".txt";
			String fileContent = "";
			//Counts the number of times an item has been bought for each slot.
			//Chooses most-bought item for each slot to create the build
			ArrayList<String> alreadyBought = new ArrayList<String>();
			for(int i = 0; i < 8; i++) {
				Map<String, Integer> itemsInSlot = new HashMap<String, Integer>();
				for(int x = 0; x < itemSlots.get(i).size(); x++) {
					String itemName = itemSlots.get(i).get(x);
					Integer count = itemsInSlot.get(itemName);
					if(count == null)
						itemsInSlot.put(itemName, 1);
					else
						itemsInSlot.put(itemName, count+1);
				}
				//Find the most-bought item for each slot
				int boughtCount = 0;
				String mostBought = "No buys";
				for (Map.Entry<String, Integer> item : itemsInSlot.entrySet()) {
					String key = item.getKey();
					int count = item.getValue();
					if(entry.getKey().equals("Mercury")) {
						System.out.println("Item Number: "+i);
						System.out.println(key);
						System.out.println(count);
					}
					if(!key.equals("No buys")) {
						if(!alreadyBought.contains(key)) {
							if(boughtCount < count) {
								boughtCount = count;
								mostBought = key;
							}
						}
					}
				}
				alreadyBought.add(mostBought);
				if(entry.getKey().equals("Ullr"))
					System.out.println();
				fileContent += mostBought + "\r\n";
			}
			//System.out.println(fileContent);
			
			File dir = new File("God Builds");
			dir.mkdir();
			
			PrintWriter writer;
			try {
				writer = new PrintWriter("God Builds/"+fileName, "UTF-8");
				writer.print(fileContent);
				writer.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	//session_id
	public static void main(String[] args) throws Exception {
		timeStamp = createTimeStamp();
		signature = getMD5Hash(devKey + "createsession" + authKey + timeStamp);
		session = createSession(urlPrefix + "createsessionjson/" + devKey + "/" + signature + "/" + timeStamp);
		signature = getMD5Hash(devKey + "getmatchhistory" + authKey + timeStamp);
		String[] players = {"Andinster", "Barraccudda", "Mlcst3alth", "Omegatron", "Jeffhindla", "lassiz", "zapman", "gnaw", "shing", "daretocare", "weak3n", "allied", "jerbie",
				"kikiornah", "incontinentia", "dagarz", "snoopy", "theboosh", "divios", "eonic", "ukneek", "samshrewornah", "hurriwind", "cyclonespin", "aror", "Ninjabobat",
				"youngbae", "optixx", "emilitoo", "hyrrok", "zyhroes", "realzx", "captaintwig", "maniakk", "badgah", "qvofred", "smek", "lawbster", "gamehunter", "trixtank",
				"frostiak", "funballer", "shadownightmare", "xaliea", "halfdevil", "iraffer", "moex", "fexez", "psiyo", "dfrezzyy"};
		
		for (String player : players) {
			try {
				makeRequest(urlPrefix + "getmatchhistoryjson/" + devKey + "/" + signature + "/" + session + "/" + timeStamp + "/" + player);
			}
			catch(Exception e) {
			}
		}
		
		calculateBuilds();
	}
}
