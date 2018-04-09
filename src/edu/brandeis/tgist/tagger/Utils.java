
package edu.brandeis.tgist.tagger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class Utils {


	public static BufferedReader getReader(String f) throws FileNotFoundException, IOException {
		if (f.endsWith(".gz")) {
			return Utils.getGZipReader(f);
		} else {
			return Utils.getFileReader(f); }
	}

	public static BufferedWriter getWriter(String f) throws FileNotFoundException, IOException {
		if (f.endsWith(".gz")) {
			return Utils.getGZipWriter(f);
		} else {
			return Utils.getFileWriter(f); }
	}

	static BufferedWriter getWriter(String processingHistoryFile, boolean append)
			throws FileNotFoundException {
		// not to be used for gzipped files
		return getFileWriter(processingHistoryFile, append);		
	}

	private static BufferedReader getFileReader(String f) throws FileNotFoundException {
		return new BufferedReader(
					new InputStreamReader(
						new FileInputStream(new File(f)),
						StandardCharsets.UTF_8));
	}

	private static BufferedWriter getFileWriter(String f) throws FileNotFoundException {
		return new BufferedWriter(
					new OutputStreamWriter(
						new FileOutputStream(new File(f)),
						StandardCharsets.UTF_8));
	}

	private static BufferedWriter getFileWriter(String f, boolean append)
			throws FileNotFoundException {
		return new BufferedWriter(
					new OutputStreamWriter(
						new FileOutputStream(new File(f), append),
						StandardCharsets.UTF_8));
	}

	private static BufferedReader getGZipReader(String f) throws IOException {
		return new BufferedReader(
					new InputStreamReader(
						new GZIPInputStream(new FileInputStream(f)),
						StandardCharsets.UTF_8));
	}

	// TODO: there is something wrong with this
	private static BufferedWriter getGZipWriter(String f) throws IOException {
		return new BufferedWriter(
					new OutputStreamWriter(
						new GZIPOutputStream(new FileOutputStream(f)),
						StandardCharsets.UTF_8));
	}

	public static void ensureDirectory(String outfile) {
		File parentDir = (new File(outfile)).getParentFile();
		if (! parentDir.exists())
			parentDir.mkdirs();
	}

	public static List<String> readLines(String file) throws IOException {
		return Files.readAllLines(Paths.get(file), Charset.forName("UTF-8"));
	}

	public static int readProcessed(String processedFile) throws IOException {
		File file = new File(processedFile);
		if (! file.exists())
			file.createNewFile();
		int processed;
		try (Scanner sc = new Scanner(file)) {
			try {
				processed = sc.nextInt();
			} catch (NoSuchElementException e) {
				processed = 0; }
		}
		return processed;
	}

	public static String getStackTrace(Throwable aThrowable) {
	    final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}

	/**
	 * Run a shell command and return what the command would have printed to
	 * standard output. 
	 * 
	 * @param command The command to execute.
	 * @return String, or null if error during execution.
	 */
	public static String shellCommand(String command) {

		try {
			Process p = Runtime.getRuntime().exec(command);
			BufferedReader stdInput = new BufferedReader(
					new InputStreamReader(p.getInputStream()));
			String s;
			StringBuilder sb = new StringBuilder();
			while ((s = stdInput.readLine()) != null)
				sb.append(s);
			return sb.toString();
		} catch (IOException ex) {
			return null;
		}
	}

}

