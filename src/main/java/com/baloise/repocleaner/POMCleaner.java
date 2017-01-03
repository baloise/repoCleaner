package com.baloise.repocleaner;

import static java.lang.String.format;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class POMCleaner {

	private Path pom;

	public POMCleaner(Path pom) {
		this.pom = pom;
	}

	public void clean() {
		fixParentRelativePath(pom);
	}

	public void fixParentRelativePath(Path pom) throws TransformerFactoryConfigurationError {
		if (Files.exists(pom)) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			try {
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(Files.newInputStream(pom));
				Element parent = (Element) doc.getElementsByTagName("parent").item(0);
				if (parent.getElementsByTagName("relativePath").getLength() > 0) {
					return;
				}
				String artifactId = parent.getElementsByTagName("artifactId").item(0).getTextContent();
				final String parentRelativePath = "../" + artifactId;
				if (Files.exists(pom.getParent().resolve(parentRelativePath))) {
					LOG.change(format("adding parent relative path %s to %s" , parentRelativePath, pom));
					Element relativePath = doc.createElement("relativePath");
					relativePath.appendChild(doc.createTextNode(parentRelativePath));
					parent.appendChild(relativePath);

					DOMSource source = new DOMSource(doc);
					StreamResult result = new StreamResult(Files.newOutputStream(pom, StandardOpenOption.WRITE));
					TransformerFactory tFactory = TransformerFactory.newInstance();
					Transformer transformer = tFactory.newTransformer();
					transformer.transform(source, result);

				}
			} catch (Exception e) {
				LOG.exception("fixing parent relative path", pom, e);
			}
		}
	}
	
}
