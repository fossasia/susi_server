package org.loklak.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class IO {

	public static String readFile(String path) throws IOException 
	{
	  byte[] encoded = Files.readAllBytes(Paths.get(path));
	  return new String(encoded);
	}
}
