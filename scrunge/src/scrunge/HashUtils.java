package scrunge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 64-bit hashing function based on FNV-1 algorithm.
 *  
 * @see <a href="https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function">
 *       Wikipedia: Fowler–Noll–Vo hash function</a>
 */
public class HashUtils {

    final static long FNV_OFFSET = 2166136261L;
    final static long FNV_PRIME = 16777619L;
	
	private final static int CHUNK_SIZE = 1024;
	
	static long getHash(File file, long offset, boolean debug) {
		byte[] scratch = new byte[CHUNK_SIZE];

		try {
			FileInputStream fin = new FileInputStream(file);

			if (offset != 0) {
				fin.skip(offset);
			}

			int bytesRead = fin.read(scratch);
			
			if (debug) {
				System.out.println("Read " + bytesRead + " bytes");
				String hex = bytesToHex(scratch);
				System.out.println("[" + hex + "]");
			}
			
			fin.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		long hash = computeHash(scratch);

		return hash;
	}
	
	
	private static long computeHash(byte[] bytes) {
		long hash = FNV_OFFSET;

		for (int i = 0; i < bytes.length; i++) {
			hash = (hash ^ (long) bytes[i]) * FNV_PRIME;
		}

		hash += hash << 13;
		hash ^= hash >> 7;
		hash += hash << 3;
		hash ^= hash >> 17;
		hash += hash << 5;
		return hash;
	}
	
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
}
