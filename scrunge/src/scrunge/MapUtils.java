package scrunge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

public class MapUtils {

	static void persistMap(String mapPath, Map<Long, File> map) {
		File outFile = new File(mapPath);

		System.out.println("Persisting the map to " + outFile.getAbsolutePath());

		try {
			FileOutputStream fos = new FileOutputStream(outFile);
			ObjectOutputStream out = new ObjectOutputStream(fos);

			out.writeObject(map);
			out.flush();
			out.close();
			fos.close();

			System.out.println("Saved map with " + map.size() + " values.");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	static Map<Long, File> preloadMap(String mapPath) {
		File inFile = new File(mapPath);

		if (!inFile.exists()) {
			System.out.println("Sorry, can't find the file [" + inFile.getAbsolutePath() + "] Skipping this step.");
			return null;
		}

		try {

			System.out.println("Loading the map from " + inFile.getAbsolutePath());

			FileInputStream fis = new FileInputStream(inFile);
			ObjectInputStream in = new ObjectInputStream(fis);

			Map<Long, File> mapInFile = (Map<Long, File>) in.readObject();

			in.close();
			fis.close();

			System.out.println("Loaded map with " + mapInFile.size() + " values.");

			return mapInFile;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}
}
