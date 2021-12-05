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
	
	static long getHash(File file, long offset) {
		byte[] scratch = new byte[CHUNK_SIZE];

		try {
			FileInputStream fin = new FileInputStream(file);

			if (offset != 0) {
				fin.skip(offset);
			}

			fin.read(scratch);
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
}
