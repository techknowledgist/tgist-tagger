package edu.brandeis.tgist.tagger;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class StanfordTagger {

	private final StanfordCoreNLP pipeline;
	private String corpus, txtDir, tagDir, configDir, stateDir;
	private String listFile, logFile, processedFile, processingHistoryFile;
			
	/**
	 * Create a StanfordCoreNLP object with a tokenizer, splitter, tagger and
	 * lemmatizer. Currently the lemmatizer results are ignored, but they could
	 * be added easily.
	 */
	public StanfordTagger() {
		Properties props = new Properties();
	    props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
		this.pipeline = new StanfordCoreNLP(props);
	}

	/**
	 * Tag files in a corpus. Files that were already tagged will be skipped and
	 * no more than filesToProcess will be tagged.
	 * 
	 * @param corpus
	 * @param filesToProcess
	 * @throws IOException 
	 */
	public void tagCorpus(String corpus, int filesToProcess) throws IOException {

		// TODO: add config (well, maybe not, there is not a lot to say)
		// TODO: add housekeeping and time elapsed
		// TODO: gzip result file

		configureTaggerForCorpus(corpus);
		int skip = Utils.readProcessed(this.processedFile);
		List<String> files = Utils.readLines(this.listFile);
		if (skip >= files.size()) {
			System.out.println("No files left to process");
			return; }

		try (BufferedWriter log = Utils.getWriter(this.logFile)) {
			long startTime = System.nanoTime();
			int filesProcessed = 0;
			for (int i = skip; i < files.size(); i++) {
				String line = files.get(i);
				String fname = line.split("\t")[2];
				String infile = txtDir + "/" + fname + ".gz";
				String outfile = tagDir + "/" + fname;
				Utils.ensureDirectory(outfile);
				filesProcessed++;
				try {
					System.out.println(fname);
					tagFile(infile, outfile);
				} catch (Exception e) {
					String stackTrace = Utils.getStackTrace(e);
					log.write(fname + "\n" + stackTrace + "\n");
				}
				if (filesProcessed % 100 == 0)
					updateProcessed(processedFile, 100);
				if (filesProcessed >= filesToProcess)
					break;
			}

			long elapsedTime = System.nanoTime() - startTime;
			updateProcessed(processedFile, filesProcessed % 100);
			updateProcessingHistory(
					processingHistoryFile, filesProcessed, elapsedTime);
		}
	}

	private void configureTaggerForCorpus(String corpus) {
		this.corpus = corpus;
		this.txtDir = this.corpus + "/data/d1_txt/01/files";
		this.tagDir = this.corpus + "/data/d2_tag/01/files";
		this.configDir = this.corpus + "/data/d2_tag/01/config";
		this.stateDir = this.corpus + "/data/d2_tag/01/state";
		new File(this.configDir).mkdirs();
		new File(this.stateDir).mkdirs();
		this.listFile = this.corpus + "/config/files.txt";
		this.logFile = this.stateDir + "/errors.txt";
		this.processedFile = this.stateDir + "/processed.txt";
		this.processingHistoryFile = this.stateDir + "/processing-history.txt";
	}
	
	/**
	 * Run the tagger over a file and create a tagged file.
	 *
	 * @param input Name of the file to be tagged
	 * @param output Name of the output file
	 * @throws IOException
	 */
	public void tagFile(String input, String output) throws IOException {

		try (BufferedReader reader = Utils.getReader(input);
				BufferedWriter writer = Utils.getWriter(output)) {

			StringBuilder sb = new StringBuilder();
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					tagBufferAndSave(sb, writer);
					break;
				} else if (line.startsWith("FH_") && line.endsWith(":")) {
					tagBufferAndSave(sb, writer);
					writer.write(line + "\n");
				} else {
					sb.append(line).append("\n"); }}
		}
	}

	private void tagBufferAndSave(StringBuilder sb, BufferedWriter writer)
			throws IOException {
		String input = sb.toString().trim();
		sb.setLength(0);
		Annotation annotation = new Annotation(input);
		this.pipeline.annotate(annotation);
		writeAnnotation(annotation, writer);
	}

	/**
	 * Show the Annotation object.
	 * @param annotation
	 */
	private void show(Annotation annotation) {
		// CoreMap: a Map that uses class objects as keys and has values with custom types
		// CoreLabel: a CoreMap with additional token-specific methods
		for (CoreMap sentence : annotation.get(SentencesAnnotation.class)) {
			System.out.println(sentence);
			for (CoreLabel token : sentence.get(TokensAnnotation.class))
				System.out.println(new StanfordToken(token)); }
	}

	/**
	 * Write the contents of the Stanford Annotation to the output buffer.
	 *
	 * @param annotation
	 * @param writer
	 * @throws IOException
	 */
	private void writeAnnotation(Annotation annotation, BufferedWriter writer)
			throws IOException {
		
		for (CoreMap sentence : annotation.get(SentencesAnnotation.class)) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				StanfordToken tok = new StanfordToken(token);
				writer.write(tok.asPair()); }
			writer.write("\n");
		}
	}

	private void updateProcessed(String processedFile, int increment)
			throws FileNotFoundException, IOException {
		
		int oldProcessed = Utils.readProcessed(processedFile);
		int newProcessed = oldProcessed + increment;
		try (BufferedWriter writer = Utils.getWriter(processedFile)) {
			writer.write(String.format("%d\n", newProcessed)); }
	}


	private void updateProcessingHistory(
			String processingHistoryFile, int filesProcessed, long elapsedTime)
			throws IOException {

		try (BufferedWriter writer = Utils.getWriter(processingHistoryFile, true)) {
			String commit = Utils.shellCommand("git rev-parse HEAD");
			commit = commit.substring(0, 7);
			//2018:04:06-09:56:52
			String time = new SimpleDateFormat("yyyy:MM:dd-HH:mm:ss").format(new Date());
			float f = elapsedTime;
			f = f /1000000000;
			String line = String.format(
					"--txt2tag\t%d\t%s\t%s\t%s\n", filesProcessed, time, commit, f);
			writer.write(line);
		}
	}

}


/**
 * Class that provides a simple wrapper around Stanford's CoreLabel.
 */
class StanfordToken {

	public int start, end, index;
	public String word, lemma, pos;

	StanfordToken(CoreLabel token) {
		this.index = token.index();
		this.start = token.beginPosition();
		this.end = token.endPosition();
		this.word = token.word();
		this.lemma = token.lemma();
		this.pos = token.tag();
	}

	@Override
	public String toString() {
		return String.format(
				"<Token %d %d-%d %s %s %s>",
				this.index, this.start, this.end, this.word, this.pos, this.lemma);
	}

	String asPair() {
		return String.format("%s/%s ", this.word, this.pos);
	}
}
