package edu.brandeis.tgist.tagger;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {

		String CORPUS = "/Users/marc/Desktop/fuse/code/tgist-features/zzz-ipa180104";

		//processFile();
		processCorpus(CORPUS, 2);
	}

	private static void processFile() throws IOException {
		StanfordTagger tagger = new StanfordTagger();
		for (int i = 0; i < 100; i++) {
			System.out.println(i);
			tagger.tagFile(
					"src/resources/US20180000001A1.xml",
					"src/resources/US20180000001A1.tag");
			break;
		}
	}

	private static void processCorpus(String corpus, int filesToProcess) throws IOException {
		StanfordTagger tagger = new StanfordTagger();
		tagger.tagCorpus(corpus, filesToProcess);
	}


}
