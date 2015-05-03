/*
The MIT License (MIT)

Copyright (c) 2013, V. Giacometti, M. Giuriato, B. Petrantuono

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package cz.metaverse.android.bilingualreader.manager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import nl.siegmann.epublib.epub.EpubReader;
import android.content.Context;
import android.util.Log;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.helper.Func;

/**
 *
 * Epub is a class that handless epub files themselves.
 * 	It can read the contents of epub ebooks, read individual pages, write into them, and much more.
 *
 */
public class Epub {

	private static final String LOG = "Epub";

	private Book book;
	private String title;
	private int currentSpineElementIndex;
	private String currentPage;
	private String[] spineElementPaths;
	private int pageCount;	// NOTE: currently, counting the number of XHTML pages
	private int currentLanguage;
	private List<String> availableLanguages;
	private List<Boolean> translations; 	// tells whether a page has a translation available
	private String decompressedFolder;
	private String pathOPF;
	private String metadata;

	private String fileName;
	FileInputStream fileInputStream;
	private String actualCSS = "";
	private String[][] audio;

	// Static variables
	private static Context context;
	private static String tempLocation = Func.Paths.getEpubsUnpackDir();

	/**
	 * Initialize EpubManupulator with a book from given fileName.
	 * @param fileName		Path of the epub to work with
	 * @param destFolder 	Subpath where to unzip the ebook (usually an index 0-2)
	 * @param theContext 	Base context of the encapsulating activity
	 * @throws Exception
	 */
	public Epub(String fileName, String destFolder,
			Context theContext) throws Exception {

		List<String> spineElements;
		List<SpineReference> spineList;

		if (context == null) {
			context = theContext;
		}

		this.fileInputStream = new FileInputStream(fileName);
		this.book = (new EpubReader()).readEpub(fileInputStream);
		this.title = book.getTitle();

		this.fileName = fileName;
		this.decompressedFolder = destFolder;

		Spine spine = book.getSpine();
		spineList = spine.getSpineReferences();

		this.currentSpineElementIndex = 0;
		this.currentLanguage = 0;

		spineElements = new ArrayList<String>();
		pages(spineList, spineElements);
		this.pageCount = spineElements.size();

		this.spineElementPaths = new String[spineElements.size()];

		unzipEpub(fileName, tempLocation + decompressedFolder);

		pathOPF = getPathOPF(tempLocation + decompressedFolder);

		for (int i = 0; i < spineElements.size(); ++i) {
			// TODO: is there a robust path joiner in the java libs?
			this.spineElementPaths[i] = "file://" + tempLocation
					+ decompressedFolder + "/" + pathOPF + "/"
					+ spineElements.get(i);
		}

		if (spineElements.size() > 0) {
			goToPage(0);
		}

		metadata = generateMetadata();
		createTableOfContentsFile();
	}

	/**
	 * Initialize EpubManupulator with a book from given fileName with an already decompressed contents.
	 * @param fileName		Path of the epub to work with
	 * @param folder		Path of the folder where the epub is decompressed
	 * @param spineIndex
	 * @param language
	 * @param theContext
	 * @throws Exception
	 *
	 * TODO unify with the first initializator.
	 */
	public Epub(String fileName, String folder, int spineIndex,
			int language, Context theContext) throws Exception {
		List<String> spineElements;
		List<SpineReference> spineList;

		if (context == null) {
			context = theContext;
		}

		this.fileInputStream = new FileInputStream(fileName);
		this.book = (new EpubReader()).readEpub(fileInputStream);
		this.title = book.getTitle();
		this.fileName = fileName;
		this.decompressedFolder = folder;

		Spine spine = book.getSpine();
		spineList = spine.getSpineReferences();
		this.currentSpineElementIndex = spineIndex;
		this.currentLanguage = language;
		spineElements = new ArrayList<String>();
		pages(spineList, spineElements);
		this.pageCount = spineElements.size();
		this.spineElementPaths = new String[spineElements.size()];

		pathOPF = getPathOPF(tempLocation + folder);

		for (int i = 0; i < spineElements.size(); ++i) {
			// TODO: is there a robust path joiner in the java libs?
			this.spineElementPaths[i] = "file://" + tempLocation + folder + "/"
					+ pathOPF + "/" + spineElements.get(i);
		}
		goToPage(spineIndex);

		metadata = generateMetadata();
		createTableOfContentsFile();
	}

	/**
	 * Checks whether there are any problems with this instance, if for example the Android system
	 * didn't close any important fields that would result in NullPointerExceptions.
	 * @return true if everything appears to be sound
	 */
	public boolean selfCheck() {
		boolean ok = title != null && currentPage != null && spineElementPaths != null
				&& spineElementPaths.length > 0 && decompressedFolder != null && decompressedFolder != null
				&& metadata != null && fileName != null;

		Log.d(LOG, "Epub selfCheck - " + ok);
		return ok;
	}

	/**
	 * Set the current language we read in this book.
	 * @param lang	index of the language in the epub
	 * @throws Exception
	 */
	public void setLanguage(int lang) throws Exception {
		if ((lang >= 0) && (lang <= this.availableLanguages.size())) {
			this.currentLanguage = lang;
		}
		goToPage(this.currentSpineElementIndex);
	}

	/**
	 * Set the current language we read in this book
	 * @param lang		an identifier string of the desired language
	 * @throws Exception
	 */
	public void setLanguage(String lang) throws Exception {
		int i = 0;
		while ((i < this.availableLanguages.size())
				&& (!(this.availableLanguages.get(i).equals(lang)))) {
			i++;
		}
		setLanguage(i);
	}

	/**
	 * @return a list of available languages.
	 */
	// TODO: lookup table of language names from language codes
	public String[] getLanguages() {
		String[] lang = new String[availableLanguages.size()];
		for (int i = 0; i < availableLanguages.size(); i++) {
			lang[i] = availableLanguages.get(i);
		}
		return lang;
	}

	/**
	 * Goes through the chapters and finds out if they have parallel translations.
	 * (create parallel text mapping)
	 * @param spineList
	 * @param pages
	 */
	private void pages(List<SpineReference> spineList, List<String> pages) {
		int langIndex;
		String lang;
		String actualPage;

		this.translations = new ArrayList<Boolean>();
		this.availableLanguages = new ArrayList<String>();

		for (int i = 0; i < spineList.size(); ++i) {
			actualPage = (spineList.get(i)).getResource().getHref();
			lang = getPageLanguage(actualPage);
			if (lang != "") {
				// parallel text available
				langIndex = languageIndexFromID(lang);

				if (langIndex == this.availableLanguages.size())
					this.availableLanguages.add(lang);

				if (langIndex == 0) {
					this.translations.add(true);
					pages.add(actualPage);
				}
			} else {
				// parallel text NOT available
				this.translations.add(false);
				pages.add(actualPage);
			}
		}
	}

	//
	/**
	 * Finds whether given language has already been encountered.
	 * 	(language index from language string (id))
	 * @param lang  language string
	 * @return
	 */
	private int languageIndexFromID(String lang) {
		int i = 0;
		while ((i < availableLanguages.size())
				&& (!(availableLanguages.get(i).equals(lang)))) {
			i++;
		}
		return i;
	}

	/**
	 * Returns path of the directory where is the .opf file inside the unzipped epub directory.
	 * @param unzipDir	Path of the directory where the epub is unzipped
	 * @return			Path to the DIRECTORY where the .opf file resides.
	 * @throws IOException
	 */
	// TODO: better parsing
	private static String getPathOPF(String unzipDir) throws IOException {
		String pathOPF = "";
		// get the OPF path, directly from container.xml
		BufferedReader br = new BufferedReader(new FileReader(unzipDir + "/META-INF/container.xml"));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.indexOf(getS(R.string.full_path)) > -1) {
				// Finds the index of the words "full-path", then finds the position of the following
				//	two " characters and extracts the text between them into pathOPF.
				int start = line.indexOf(getS(R.string.full_path));
				int start2 = line.indexOf("\"", start);
				int stop2 = line.indexOf("\"", start2 + 1);
				if (start2 > -1 && stop2 > start2) {
					pathOPF = line.substring(start2 + 1, stop2).trim();
					break;
				}
			}
		}
		br.close();

		// in case the OPF file is in the root directory
		if (!pathOPF.contains("/"))
			pathOPF = ""; // we only need the directory, not the file name

		// remove the OPF file name and the preceding '/'
		int last = pathOPF.lastIndexOf('/');
		if (last > -1) {
			pathOPF = pathOPF.substring(0, last); // we only need the directory, not the file name
		}

		return pathOPF;
	}

	/**
	 * Unzips the contents of the epub file into a temp directory.
	 * @param inputZip					What file to unzip
	 * @param destinationDirectory		Where to unzip
	 * @throws IOException
	 */
	// TODO: more efficient unzipping
	public void unzipEpub(String inputZip, String destinationDirectory)
			throws IOException {
		int BUFFER = 2048;
		List<String> zipFiles = new ArrayList<String>();
		File sourceZipFile = new File(inputZip);
		File unzipDestinationDirectory = new File(destinationDirectory);
		unzipDestinationDirectory.mkdir();

		ZipFile zipFile;
		zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
		Enumeration<? extends ZipEntry> zipFileEntries = zipFile.entries();

		// Process each entry
		while (zipFileEntries.hasMoreElements()) {

			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
			String currentEntry = entry.getName();
			File destFile = new File(unzipDestinationDirectory, currentEntry);

			if (currentEntry.endsWith(getS(R.string.zip))) {
				zipFiles.add(destFile.getAbsolutePath());
			}

			File destinationParent = destFile.getParentFile();
			destinationParent.mkdirs();

			if (!entry.isDirectory()) {
				BufferedInputStream is = new BufferedInputStream(
						zipFile.getInputStream(entry));
				int currentByte;
				// buffer for writing file
				byte data[] = new byte[BUFFER];

				FileOutputStream fos = new FileOutputStream(destFile);
				BufferedOutputStream dest = new BufferedOutputStream(fos,
						BUFFER);

				while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, currentByte);
				}
				dest.flush();
				dest.close();
				is.close();

			}

		}
		zipFile.close();

		for (Iterator<String> iter = zipFiles.iterator(); iter.hasNext();) {
			String zipName = (String) iter.next();
			unzipEpub(zipName,
					destinationDirectory
							+ File.separatorChar
							+ zipName.substring(0,
									zipName.lastIndexOf(getS(R.string.zip))));
		}
	}

	/**
	 * Close the file input stream.
	 * @throws IOException
	 */
	public void closeFileInputStream() throws IOException {
		fileInputStream.close();
		book = null;
	}

	/**
	 * Close the stream and delete the extraction folder.
	 * @throws IOException
	 */
	public void destroy() throws IOException {
		closeFileInputStream();
		File c = new File(tempLocation + decompressedFolder);
		deleteDir(c);
	}

	/**
	 * Recursively delete a directory
	 * @param f	Directory to delete
	 */
	private void deleteDir(File f) {
		if (f.isDirectory()) {
			for (File child : f.listFiles()) {
				deleteDir(child);
			}
		}
		f.delete();
	}

	/**
	 * Move the book to a different directory.
	 * @param newName	new name
	 */
	public void changeDirName(String newName) throws Exception {
		Log.d(LOG, "EpubManipulator changeDirName");

		// Rename the directory
		File dir = new File(tempLocation + decompressedFolder);
		File newDir = new File(tempLocation + newName);
		dir.renameTo(newDir);

		// Readress the spineElementPaths
		for (int i = 0; i < spineElementPaths.length; ++i)
			// TODO: is there a robust path joiner in the java libs?
			spineElementPaths[i] = spineElementPaths[i].replace("file://"
					+ tempLocation + decompressedFolder, "file://" + tempLocation
					+ newName);

		decompressedFolder = newName;

		// Recreate ToC because its links are now pointing to the old file locations.
		if (book == null) {
			this.fileInputStream = new FileInputStream(fileName);
			this.book = (new EpubReader()).readEpub(fileInputStream);
		}
		createTableOfContentsFile();

		// Reopen the current page
		try {
			goToPage(currentSpineElementIndex);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Obtain a page in the current language
	 * @param page	page to obtain
	 * @return		page in the current language
	 * @throws Exception
	 */
	public String goToPage(int page) throws Exception {
		return goToPage(page, this.currentLanguage);
	}

	/**
	 * Obtain a page in the given language.
	 * @param page	page to obtain
	 * @param lang	language to obtain the page in
	 * @return		page in the given language
	 * @throws Exception
	 */
	public String goToPage(int page, int lang) throws Exception {
		String spineElement;
		String fileExtension;
		if (page < 0) {
			page = 0;
		}
		if (page >= this.pageCount) {
			page = this.pageCount - 1;
		}
		this.currentSpineElementIndex = page;

		spineElement = this.spineElementPaths[currentSpineElementIndex];

		// If this page has alternative translations
		if (this.translations.get(page)) {
			// TODO: better parsing
			fileExtension = spineElement.substring(spineElement.lastIndexOf("."));
			// Get the first part of the filename (without the language code and extension
			//	- e.g. (first_part.)EN.epub)
			spineElement = spineElement.substring(0,
					spineElement.lastIndexOf(this.availableLanguages.get(0)));
			// Add the desired language and extension
			spineElement = spineElement + this.availableLanguages.get(lang)
					+ fileExtension;
		}

		this.currentPage = spineElement;

		audioExtractor(currentPage);

		return spineElement;
	}

	/**
	 * Go to next chapter
	 */
	public String goToNextChapter() throws Exception {
		return goToPage(this.currentSpineElementIndex + 1);
	}

	/**
	 * Go to previous chapter
	 */
	public String goToPreviousChapter() throws Exception {
		return goToPage(this.currentSpineElementIndex - 1);
	}

	public String metadata() {
		return metadata;
	}

	/**
	 * Create an HTML page with book metadata
	 * @return html page
	 */
	public String generateMetadata() {
		// TODO: style it and escape metadata values
		// TODO: use StringBuilder
		List<String> tmp;
		Metadata metadata = book.getMetadata();
		String html = getS(R.string.htmlBodyTableOpen);

		// Titles
		tmp = metadata.getTitles();
		if (tmp.size() > 0) {
			html += getS(R.string.titlesMeta);
			html += "<td>" + tmp.get(0) + "</td></tr>";
			for (int i = 1; i < tmp.size(); i++)
				html += "<tr><td></td><td>" + tmp.get(i) + "</td></tr>";
		}

		// Authors
		List<Author> authors = metadata.getAuthors();
		if (authors.size() > 0) {
			html += getS(R.string.authorsMeta);
			html += "<td>" + authors.get(0).getFirstname() + " "
					+ authors.get(0).getLastname() + "</td></tr>";
			for (int i = 1; i < authors.size(); i++)
				html += "<tr><td></td><td>" + authors.get(i).getFirstname()
						+ " " + authors.get(i).getLastname() + "</td></tr>";
		}

		// Contributors
		authors = metadata.getContributors();
		if (authors.size() > 0) {
			html += getS(R.string.contributorsMeta);
			html += "<td>" + authors.get(0).getFirstname() + " "
					+ authors.get(0).getLastname() + "</td></tr>";
			for (int i = 1; i < authors.size(); i++) {
				html += "<tr><td></td><td>" + authors.get(i).getFirstname()
						+ " " + authors.get(i).getLastname() + "</td></tr>";
			}
		}

		// TODO: extend lib to get multiple languages?
		// Language
		html += getS(R.string.languageMeta) + metadata.getLanguage()
				+ "</td></tr>";

		// Publishers
		tmp = metadata.getPublishers();
		if (tmp.size() > 0) {
			html += getS(R.string.publishersMeta);
			html += "<td>" + tmp.get(0) + "</td></tr>";
			for (int i = 1; i < tmp.size(); i++)
				html += "<tr><td></td><td>" + tmp.get(i) + "</td></tr>";
		}

		// Types
		tmp = metadata.getTypes();
		if (tmp.size() > 0) {
			html += getS(R.string.typesMeta);
			html += "<td>" + tmp.get(0) + "</td></tr>";
			for (int i = 1; i < tmp.size(); i++)
				html += "<tr><td></td><td>" + tmp.get(i) + "</td></tr>";
		}

		// Descriptions
		tmp = metadata.getDescriptions();
		if (tmp.size() > 0) {
			html += getS(R.string.descriptionsMeta);
			html += "<td>" + tmp.get(0) + "</td></tr>";
			for (int i = 1; i < tmp.size(); i++)
				html += "<tr><td></td><td>" + tmp.get(i) + "</td></tr>";
		}

		// Rights
		tmp = metadata.getRights();
		if (tmp.size() > 0) {
			html += getS(R.string.rightsMeta);
			html += "<td>" + tmp.get(0) + "</td></tr>";
			for (int i = 1; i < tmp.size(); i++)
				html += "<tr><td></td><td>" + tmp.get(i) + "</td></tr>";
		}

		html += getS(R.string.tablebodyhtmlClose);
		return html;
	}

	/**
	 * Helper function to createTableOfContentsFile()
	 */
	public String r_createTableOfContentsFile(TOCReference e) {

		String childrenPath = "file://" + tempLocation + decompressedFolder + "/"
				+ pathOPF + "/" + e.getCompleteHref();

		String html = "<ul><li>" + "<a href=\"" + childrenPath + "\">"
				+ e.getTitle() + "</a>" + "</li></ul>";

		List<TOCReference> children = e.getChildren();

		for (int j = 0; j < children.size(); j++)
			html += r_createTableOfContentsFile(children.get(j));

		return html;
	}

	/**
	 * Create an html file, which contain the Table of Contents, in the EPUB folder.
	 */
	public void createTableOfContentsFile() {
		List<TOCReference> tmp;
		TableOfContents toc = book.getTableOfContents();
		String html = "<html><body><ul>";

		tmp = toc.getTocReferences();

		if (tmp.size() > 0) {
			html += getS(R.string.tocReference);
			for (int i = 0; i < tmp.size(); i++) {
				String path = "file://" + tempLocation + decompressedFolder + "/"
						+ pathOPF + "/" + tmp.get(i).getCompleteHref();

				html += "<li>" + "<a href=\"" + path + "\">"
						+ tmp.get(i).getTitle() + "</a>" + "</li>";

				// pre-order traversal?
				List<TOCReference> children = tmp.get(i).getChildren();

				for (int j = 0; j < children.size(); j++)
					html += r_createTableOfContentsFile(children.get(j));

			}
		}

		html += getS(R.string.tablebodyhtmlClose);

		// write down the html file
		String filePath = tempLocation + decompressedFolder + "/Toc.html";
		try {
			File file = new File(filePath);
			FileWriter fw = new FileWriter(file);
			fw.write(html);
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return the path of the Toc.html file
	 */
	public String tableOfContents() {
		Log.d(LOG, "tableOfContents [" + decompressedFolder + "] ");
		return "File://" + tempLocation + decompressedFolder + "/Toc.html";
	}

	/**
	 * Determine whether a book has the requested page
	 * 	if so, return its index; return -1 otherwise
	 * @return -1 if page not found, otherwise index of said page
	 */
	public int getPageIndex(String page) {
		int result = -1;
		String lang;

		lang = getPageLanguage(page);
		if ((this.availableLanguages.size() > 0) && (lang != "")) {
			page = page.substring(0, page.lastIndexOf(lang))
					+ this.availableLanguages.get(0)
					+ page.substring(page.lastIndexOf("."));
		}
		for (int i = 0; i < this.spineElementPaths.length && result == -1; i++) {
			if (page.equals(this.spineElementPaths[i])) {
				result = i;
			}
		}

		return result;
	}

	/**
	 * Go to specified page (and set its language as current language)
	 * @param page 	to go to
	 * @return 		success
	 */
	public boolean goToPage(String page) {
		int index = getPageIndex(page);
		boolean res = false;
		if (index >= 0) {
			String newLang = getPageLanguage(page);
			try {
				goToPage(index);
				if (newLang != "") {
					setLanguage(newLang);
				}
				res = true;
			} catch (Exception e) {
				res = false;
				Log.e(getS(R.string.error_goToPage), e.getMessage());
			}
		}
		return res;
	}

	/**
	 * Return the language of the page according to the
	 * 	ISO 639-1 naming convention:
	 *  foo.XX.html where X \in [a-z]
	 *  or an empty string if language not found.
	 *
	 * @param page 	to get the language of
	 * @return 		language code ISO 639-1 or empty string
	 */
	public String getPageLanguage(String page) {
		String[] tmp = page.split("\\.");
		// Language XY is present if the string format is "pagename.XY.xhtml",
		// where XY are 2 non-numeric characters that identify the language
		if (tmp.length > 2) {
			String secondFromLastItem = tmp[tmp.length - 2];
			if (secondFromLastItem.matches("[a-z][a-z]")) {
				return secondFromLastItem;
			}
		}
		return "";
	}

	/**
	 * Set the CSS according to the settings
	 * @param settings
	 */
	// TODO work in progress
	public void addCSS(String[] settings) {
		// CSS
		String css = "<style type=\"text/css\">\n";

		if (!settings[0].isEmpty()) {
			css = css + "body{color:" + settings[0] + ";}";
			css = css + "a:link{color:" + settings[0] + ";}";
		}

		if (!settings[1].isEmpty())
			css = css + "body {background-color:" + settings[1] + ";}";

		if (!settings[2].isEmpty())
			css = css + "p{font-family:" + settings[2] + ";}";

		if (!settings[3].isEmpty())
			css = css + "p{\n\tfont-size:" + settings[3] + "%\n}\n";

		if (!settings[4].isEmpty())
			css = css + "p{line-height:" + settings[4] + "em;}";

		if (!settings[5].isEmpty())
			css = css + "p{text-align:" + settings[5] + ";}";

		if (!settings[6].isEmpty())
			css = css + "body{margin-left:" + settings[6] + "%;}";

		if (!settings[7].isEmpty())
			css = css + "body{margin-right:" + settings[7] + "%;}";

		css = css + "</style>";

		for (int i = 0; i < spineElementPaths.length; i++) {
			String path = spineElementPaths[i].replace("file:///", "");
			String source = readPage(path);

			source = source.replace(actualCSS + "</head>", css + "</head>");

			writePage(path, source);
		}
		actualCSS = css;

	}

	/**
	 * Audio: Adjust audio links:
	 * 	change from relative path (that begin with ./ or ../) to absolute path
	 */
	private void adjustAudioLinks() {
		for (int i = 0; i < audio.length; i++)
			for (int j = 0; j < audio[i].length; j++) {
				if (audio[i][j].startsWith("./"))
					audio[i][j] = currentPage.substring(0,
							currentPage.lastIndexOf("/"))
							+ audio[i][j].substring(1);

				if (audio[i][j].startsWith("../")) {
					String temp = currentPage.substring(0,
							currentPage.lastIndexOf("/"));
					audio[i][j] = temp.substring(0, temp.lastIndexOf("/"))
							+ audio[i][j].substring(2);
				}
			}
	}

	/**
	 * Audio: Extract all the src field of an audio tag
	 */
	private ArrayList<String> getAudioSources(String audioTag) {
		ArrayList<String> srcs = new ArrayList<String>();
		Pattern p = Pattern.compile("src=\"[^\"]*\"");
		Matcher m = p.matcher(audioTag);
		while (m.find())
			srcs.add(m.group().replace("src=\"", "").replace("\"", ""));

		return srcs;
	}

	/**
	 * Extract all audio tags from an xhtml page
	 */
	private ArrayList<String> getAudioTags(String page) {
		ArrayList<String> res = new ArrayList<String>();

		String source = readPage(page);

		Pattern p = Pattern.compile("<audio(?s).*?</audio>|<audio(?s).*?/>");
		Matcher m = p.matcher(source);
		while (m.find())
			res.add(m.group(0));

		return res;
	}

	/**
	 * Gets called each time a new page is opened.
	 * @param page	The newly opened page
	 */
	private void audioExtractor(String page) {
		ArrayList<String> tags = getAudioTags(page.replace("file:///", ""));
		ArrayList<String> srcs;
		audio = new String[tags.size()][];

		for (int i = 0; i < tags.size(); i++) {
			srcs = getAudioSources(tags.get(i));
			audio[i] = new String[srcs.size()];
			for (int j = 0; j < srcs.size(); j++)
				audio[i][j] = srcs.get(j);
		}
		adjustAudioLinks();
	}

	public String[][] getAudio() {
		return audio;
	}

	/* TODO don't work properly, forse non necessario
	 public boolean deleteCSS(String path) {
 		path = path.replace("file:///", "");
 		String source = readPage(path);
  		source = source.replace("<style type=\"text/css\">.</style></head>", "</head>");
  		return writePage(path, source);
	 }
	 */

	/**
	 * Reads given page, returns string with text/data
	 */
	private String readPage(String path) {
		// TODO work in progress
		try {
			FileInputStream input = new FileInputStream(path);
			byte[] fileData = new byte[input.available()];

			input.read(fileData);
			input.close();

			String xhtml = new String(fileData);
			return xhtml;
		} catch (IOException e) {
			return "";
		}
	}

	/**
	 * Writes given string onto a given page.
	 *
	 * @param path	of the page to be written to
	 * @param xhtml	String of the text/data to be written
	 * @return		success
	 */
	private boolean writePage(String path, String xhtml) {
		// TODO work in progress
		try {
			File file = new File(path);
			FileWriter fw = new FileWriter(file);
			fw.write(xhtml);
			fw.flush();
			fw.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public String getTitle() {
		return title;
	}

	public int getCurrentSpineElementIndex() {
		return currentSpineElementIndex;
	}

	public String getSpineElementPath(int elementIndex) {
		return spineElementPaths[elementIndex];
	}

	public String getCurrentPageURL() {
		return currentPage;
	}

	public int getCurrentLanguage() {
		return currentLanguage;
	}

	public String getFileName() {
		return fileName;
	}

	public String getDecompressedFolder() {
		return decompressedFolder;
	}

	public static String getS(int id) {
		return context.getResources().getString(id);
	}
}
