package it.dmi.mail.pec.util;

import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import it.dmi.mail.pec.exception.PECParserException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author biagio.tozzi
 *
 */
@Slf4j
public class XMLDocumentUtils {

	public static String getAttribute(Document doc, String path, String attributeName, boolean nullable)
			throws PECParserException {

		NodeList nodes = getNodes(doc, path);
		if (nodes != null && nodes.getLength() == 1 && nodes.item(0) != null) {
			NamedNodeMap attributes = nodes.item(0).getAttributes();
			for (int i = 0; i < attributes.getLength(); i++) {
				if (attributes.item(i).getNodeName() != null
						&& attributes.item(i).getNodeName().equalsIgnoreCase(attributeName)) {
					return attributes.item(i).getNodeValue();
				}
			}
		}

		if (!nullable)
			throw new IllegalArgumentException(
					"L'attributo " + attributeName + " del nodo " + path + " non può essere nullo");

		return null;
	}

	public static Map<String, String> getTextAndAttribute(Document document, String path, String attribute,
			boolean nullable) throws PECParserException {
		Map<String, String> res = new HashMap<>();
		NodeList nodes = getNodes(document, path);

		for (int i = 0; i < nodes.getLength(); i++) {
			String value = nodes.item(i).getTextContent();
			String attr = null;

			NamedNodeMap attributes = nodes.item(i).getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				if (attributes.item(j).getNodeName().equalsIgnoreCase(attribute)) {
					attr = attributes.item(j).getNodeValue();
					break;
				}
			}

			res.put(value, attr);
		}

		if (!nullable && res.isEmpty())
			throw new IllegalArgumentException("Il nodo " + path + " non può essere vuoto o nullo");

		return res;
	}

	public static String getTextContent(Document document, String path, boolean nullable) throws PECParserException {
		NodeList nodes = getNodes(document, path);

		if (nodes != null && nodes.getLength() == 1 && nodes.item(0) != null) {
			return nodes.item(0).getTextContent();
		}

		if (!nullable)
			throw new IllegalArgumentException("Il nodo " + path + " non può essere nullo");

		return null;
	}

	private static NodeList getNodes(Document doc, String path) throws PECParserException {

		try {
			XPathFactory xpathfactory = XPathFactory.newInstance();
			XPath xpath = xpathfactory.newXPath();
			XPathExpression expr = xpath.compile(path);
			Object result = expr.evaluate(doc, XPathConstants.NODESET);
			NodeList nodes = (NodeList) result;
			return nodes;

		} catch (XPathExpressionException e) {
			log.error("Errore durante la lettura del nodo {}", path, e);
			throw new PECParserException("Errore durante la lettura del nodo " + path, e);
		}
	}

}
