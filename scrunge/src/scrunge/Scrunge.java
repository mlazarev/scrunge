package scrunge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * "Scrunge" is a duplicate file detector, but it doesn't use name, date, or size to detect them as duplicates.
 * Instead it compares the unique hash of the data at the header of each file and also somewhere in the middle.
 * It doesn't care about the format, and it's incredibly fast and surprisingly precise. Especially when applied
 * to media files which could have different prepended headers, water marks, or appended data at the very end.
 * 
 * Some Special Features:
 * -- Can persist and pre-load previously scanned files in a map. This is useful for comparing multiple paths
 * -- Can output the duplicate list to a VLC playlist, so that you can manually check content before deletion
 * 
 * @author Mike
 * @date December 2021
 *
 */
public class Scrunge {

	private static String ROOT_PATH = null;

	private static String MAP_PATH;

	private final static boolean RECURSIVE = true;

	private static Map<Long, File> scanMap = new HashMap<Long, File>();
	private static List<File[]> dupeList = new ArrayList<File[]>();
	private static List<File[]> suspectList = new ArrayList<File[]>();

	private static int filesProcessed;
	private static long timeTaken;

	public static void main(String[] args) {
		try {

			Scanner reader = new Scanner(System.in);
			String response;

			// ----- PROMPT FOR SETUP -------------------------------------------------
			
			System.out.print("Enter Root Path : ");
			response = reader.nextLine();

			if (response != null) {

				if (response.trim() == "")
				{
					System.out.println("We need a root path. Can't continue. Aborting.");
					reader.close();
					return;
				}
				
				ROOT_PATH = response;
				System.out.println("Setting Path to [" + ROOT_PATH  + "]");
			}
			
			MAP_PATH = ROOT_PATH + "persist.scrunge";
			System.out.print("Preload Map from " + MAP_PATH + "? <Yes/[No]> : ");
			response = reader.nextLine();

			if (response != null & response.equalsIgnoreCase("Yes")) {
				Map<Long, File> mapInFile = MapUtils.preloadMap(MAP_PATH);
				if (mapInFile != null)
					scanMap = mapInFile;
			}

			
			// ----- SCAN ------------------------------------------------------------			

			long scanStart = System.currentTimeMillis();

			File rootFolder = new File(ROOT_PATH);

			processFolder(rootFolder);

			secondPass();

			timeTaken = System.currentTimeMillis() - scanStart;

			printSummary();
			printDetails(dupeList, "DUPE", true);
			printDetails(suspectList, "SUSPECT", false);
			printDone();

			// ----- PROMPT FOR FOLLOW UPS -------------------------------------------------

			if (dupeList.size() > 0) {
				System.out.print("Delete dupes? <Yes/[No]> : ");
				response = reader.nextLine();

				if (response != null & response.equalsIgnoreCase("Yes")) {
					deleteDupes();
				}

				if (response != null & !response.equalsIgnoreCase("Yes")) {
					System.out.print("Write Dupe Playlist? <Yes/[No]> : ");
					response = reader.nextLine();
					if (response != null & response.equalsIgnoreCase("Yes")) {
						writeVLCPlaylist(dupeList, "dupes");
					}
				}
			}

			if (suspectList.size() > 0) {
				System.out.print("Show suspects? <Yes/[No]> : ");
				response = reader.nextLine();
				if (response != null & response.equalsIgnoreCase("Yes")) {
					printDetails(suspectList, "SUSPECT", true);
				}

				System.out.print("Write Suspect Playlist? <Yes/[No]> : ");
				response = reader.nextLine();
				if (response != null & response.equalsIgnoreCase("Yes")) {
					writeVLCPlaylist(suspectList, "suspects");
				}
			}

			System.out.print("Persist Map? <Yes/No> : ");
			response = reader.nextLine();
			if (response != null & response.equalsIgnoreCase("Yes")) {
				MapUtils.persistMap(MAP_PATH, scanMap);
			}

			reader.close();
			System.out.println("ALL DONE!");

		} catch (Exception e) {
			System.out.println("Early Termination");
		}
	}

	
	private static void processFolder(File rootFolder) {

		File[] files = rootFolder.listFiles();

		if (files == null) {
			System.out.println("No Files found in [" + rootFolder.getAbsolutePath() + "]");
			return;
		} else {
			System.out.println("Processing " + files.length + " files in " + rootFolder.getAbsolutePath());
		}

		for (int i = 0; i < files.length; i++) {
			File file = files[i];

			if (file.isDirectory()) {
				if (RECURSIVE) {
					processFolder(file);
				}
			} else {
				processFile(file);
			}

		}
	}

	private static void processFile(File file) {
		filesProcessed++;

		long hash = HashUtils.getHash(file, 0);

		File previous = scanMap.get(hash);

		if (previous != null) {
			// Identical Hashes are possible...
			// TODO may want to compare byte for byte

			// It's possible that we're comparing identical files to themselves
			// as previously loaded from a persisted map. In this case, ignore.
			if (previous.equals(file)) {
				return;
			}

			File[] pair = new File[2];
			pair[0] = previous;
			pair[1] = file;

			dupeList.add(pair);
		} else {
			scanMap.put(hash, file);
		}

	}

	private static void secondPass() {
		Iterator<File[]> it = dupeList.iterator();

		while (it.hasNext()) {
			File[] file = it.next();
			File file1 = file[0];
			File file2 = file[1];

			// Need to compute at identical offset, in case sizes are slightly different.
			// Let's test somewhere in the middle
			long offset = file1.length() / 2;

			long hash1 = HashUtils.getHash(file1, offset);
			long hash2 = HashUtils.getHash(file2, offset);

			if (hash1 != hash2) {
				it.remove();
				suspectList.add(file);
			}
		}
	}



	private static void printSummary() {
		System.out.println("------------------- SUMMARY ------------------- ");
		System.out.println("Processed " + filesProcessed + " files in " + timeTaken + "ms.");
	}

	private static void printDetails(List<File[]> list, String header, boolean printDetails) {
		int fileCount = list.size();
		long fileSize = 0L;

		if (printDetails) {
			System.out.println("------------------- " + header + " DETAILS ------------------- ");
		}

		Iterator<File[]> it = list.iterator();
		while (it.hasNext()) {
			File[] pair = it.next();
			File fileOne = pair[0];
			File fileTwo = pair[1];

			if (printDetails) {
				System.out.println("  [" + fileOne.getAbsolutePath() + "] --> [" + fileTwo.getAbsolutePath() + "]");
			}

			fileSize += fileTwo.length();
		}

		if (list.size() == 0) {
			System.out.println("No " + header + " files were found.");

		} else {

			DecimalFormat df = new DecimalFormat("#.00");

			double gigSize = ((double) fileSize) / 1024 / 1024 / 1024;

			System.out.println("------------------- " + header + " SUMMARY ------------------- ");
			System.out.println(
					"Found " + fileCount + " files in " + header + " list adding up to " + df.format(gigSize) + " GB.");
		}

	}

	private static void printDone() {
		System.out.println("------------------- DONE ------------------- ");
	}

	private static void deleteDupes() {
		Iterator<File[]> it = dupeList.iterator();
		while (it.hasNext()) {

			File[] pair = it.next();
			File file1 = pair[0];
			File file2 = pair[1];

			// Delete newest file first, because, presumably, the oldest has been properly
			// sorted
			try {
				BasicFileAttributes attr1 = Files.readAttributes(file1.toPath(), BasicFileAttributes.class);
				BasicFileAttributes attr2 = Files.readAttributes(file2.toPath(), BasicFileAttributes.class);

				File toDelete = (attr1.creationTime().compareTo(attr2.creationTime()) > 0) ? file1 : file2;

				System.out.print("Deleting newest [" + toDelete.getAbsolutePath() + "] ");
				if (toDelete.delete())
					System.out.println(" <GREAT SUCCESS!>");

			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	private static void writeVLCPlaylist(List<File[]> list, String filename) {
		File outFile = new File(ROOT_PATH + filename + ".xspf");

		System.out.println("Creating the playlist as " + outFile.getAbsolutePath());

		try {
			FileOutputStream out = new FileOutputStream(outFile);
			out.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
					+ "<playlist xmlns=\"http://xspf.org/ns/0/\" xmlns:vlc=\"http://www.videolan.org/vlc/playlist/ns/0/\" version=\"1\">\r\n"
					+ "	<title>Playlist</title>\r\n" + "	<trackList>\r\n").getBytes());

			int id = 0;

			Iterator<File[]> it = list.iterator();
			while (it.hasNext()) {
				File[] files = it.next();

				for (int i = 0; i < files.length; i++) {
					out.write(("		<track>\r\n" + "			<location>file:///" + files[i].getAbsolutePath()
							+ "</location>\r\n"
							+ "			<extension application=\"http://www.videolan.org/vlc/playlist/0\">\r\n"
							+ "				<vlc:id>" + id++ + "</vlc:id>\r\n" + "			</extension>\r\n"
							+ "		</track>\r\n").getBytes());
				}
			}

			out.write(("	</trackList>\r\n" + "</playlist>\r\n").getBytes());

			out.flush();
			out.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
