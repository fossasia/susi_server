package org.loklak.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class IO {
	
	private static Map<Path,String> map;
	private static boolean initialized = false;

	public static String readFile(Path path) throws IOException 
	{
		byte[] encoded = Files.readAllBytes(path);
		return new String(encoded);
	}
	
	public static String readFileCached(Path path) throws IOException 
	{
		Path absPath = path.toAbsolutePath();
		if(!initialized) init();
		if(map.containsKey(absPath)){
			return map.get(absPath);
		}
		else{
			String result = readFile(absPath);
			map.put(absPath, result);
			return result;
		}
	}
	
	private static void init(){
		map = new HashMap<Path,String>();
		initialized = true;
	}
}
