package com.baloise.repocleaner;

import static java.lang.String.format;
import static java.nio.charset.Charset.forName;
import static java.nio.file.Files.readAllLines;
import static java.nio.file.Files.write;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class UTF8tr {
	Charset defaultCharset = forName("ISO-8859-1");
	final static Charset UTF8 = forName("UTF-8");
	Map<String, Charset> path2Charset = new TreeMap<>(new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			int ret = Integer.compare(o1.length(), o2.length());
			if (ret != 0)
				return -ret;
			return o1.compareTo(o2);
		}
	});
	private Path projectRootDirectory;

	public UTF8tr(Path projectRootDirectory) {
		this.projectRootDirectory = projectRootDirectory;
		try {
			List<String> resources_prefs = readAllLines(
					projectRootDirectory.resolve(".settings/org.eclipse.core.resources.prefs"));
			for (String pref : resources_prefs) {
				if (pref.startsWith("encoding/")) {
					String[] tokens = pref.replaceFirst("encoding/", "").split("=", 2);
					if ("<project>".equalsIgnoreCase(tokens[0])) {
						defaultCharset = forName(tokens[1]);
					} else {
						path2Charset.put(tokens[0], forName(tokens[1]));
					}
				}
			}
		} catch (Exception e) {
		}
	}

	public Charset getCharset(Path path) {
		for (Entry<String, Charset> entry : path2Charset.entrySet()) {
			if (('/' + norm(path)).startsWith(entry.getKey())) {
				return entry.getValue();
			}
		}
		return defaultCharset;
	}

	private static String norm(Path path) {
		String ret = path.normalize().toString().replace('\\', '/');
		return ret.startsWith("./") ? ret.replaceFirst("./", "/") : ret;
	}

	public void fixEncoding() throws IOException {
		Files.walkFileTree(projectRootDirectory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (!ignore(file)) {
					final Charset charset = getCharset(file);
					System.out.println(format("%s \t %s", charset, norm(file)));
					convertToUTF8(file, charset);
				}
				return super.visitFile(file, attrs);
			}
		});
	}

	public void fixPOM() {
		Path pom = projectRootDirectory.resolve("pom.xml");
		if (Files.exists(pom)) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			try {
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(Files.newInputStream(pom));
				Element project = (Element) doc.getElementsByTagName("project").item(0);
				
				Element properties = getOrCreate(doc, project, "properties");
				Element encoding = getOrCreate(doc, properties, "project.build.sourceEncoding");

				String encodingTxt = encoding.getTextContent().trim();
				if (UTF8.name().equalsIgnoreCase(encodingTxt)) {
					System.out.println("already " + UTF8);
					return;
				}
				encoding.setTextContent(UTF8.name());

				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(Files.newOutputStream(pom, StandardOpenOption.WRITE));
				TransformerFactory tFactory = TransformerFactory.newInstance();
				Transformer transformer = tFactory.newTransformer();
				transformer.transform(source, result);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private Element getOrCreate(Document doc, Element parent, final String tagName) {
		Element ret;
		final NodeList list = parent.getElementsByTagName(tagName);
		if (list.getLength() > 0) {
			ret = (Element) list.item(0);
		} else {
			ret = doc.createElement(tagName);
			parent.appendChild(ret);
		}
		return ret;
	}

	protected Set<String> extensionWhitelist = new TreeSet<>(asList(
			"java", "xml", "properties", 
			"sql", "txt", "md", "json", "js",
			"html" , "css", "LICENSE", "confluence", "bsml", "groovy",
			"xtend"
			));

	protected boolean ignore(Path file) {
		String norm = norm(file);
		if (norm.startsWith("."))
			return true;
		if (norm.startsWith("target"))
			return true;
		if (norm.startsWith("bin"))
			return true;
		if (norm.startsWith("classes"))
			return true;
		//TODO detect text vs binary
		if (!extensionWhitelist.contains(getFileExtension(file)))
			return true;
		return false;
	}

	private String getFileExtension(Path file) {
		String name = file.getFileName().toString();
		try {
			return name.substring(name.lastIndexOf(".") + 1);
		} catch (Exception e) {
			return "";
		}
	}

	public static void main(String[] args) throws Exception {
		final Path projectRootDirectory = Paths.get(".").toAbsolutePath();
		final UTF8tr utf8tr = new UTF8tr(projectRootDirectory);
		utf8tr.fixPOM();
		utf8tr.fixEncoding();
	}

	private static void convertToUTF8(final Path path, Charset fromCharset) throws IOException {
		if (!UTF8.equals(fromCharset)) {
			write(path, readAllLines(path, fromCharset), UTF8);
		}
	}

}
