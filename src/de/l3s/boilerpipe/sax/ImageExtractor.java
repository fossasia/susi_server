package de.l3s.boilerpipe.sax;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xerces.parsers.AbstractSAXParser;
import org.cyberneko.html.HTMLConfiguration;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.Image;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;

/**
 * Extracts the images that are enclosed by extracted content. 
 * 
 * @author Christian Kohlschütter
 */
public final class ImageExtractor {
	public static final ImageExtractor INSTANCE = new ImageExtractor();
	
	/**
	 * Returns the singleton instance of {@link ImageExtractor}.
	 * 
	 * @return
	 */
	public static ImageExtractor getInstance() {
		return INSTANCE;
	}

	private ImageExtractor() {
	}

	/**
	 * Processes the given {@link TextDocument} and the original HTML text (as a
	 * String).
	 * 
	 * @param doc
	 *            The processed {@link TextDocument}.
	 * @param origHTML
	 *            The original HTML document.
	 * @return A List of enclosed {@link Image}s
	 * @throws BoilerpipeProcessingException
	 */
	public List<Image> process(final TextDocument doc,
			final String origHTML) throws BoilerpipeProcessingException {
		return process(doc, new InputSource(
				new StringReader(origHTML)));
	}

	/**
	 * Processes the given {@link TextDocument} and the original HTML text (as an
	 * {@link InputSource}).
	 * 
	 * @param doc
	 *            The processed {@link TextDocument}.
	 * @param origHTML
	 *            The original HTML document.
	 * @return A List of enclosed {@link Image}s
	 * @throws BoilerpipeProcessingException
	 */
	public List<Image> process(final TextDocument doc,
			final InputSource is) throws BoilerpipeProcessingException {
		final Implementation implementation = new Implementation();
		implementation.process(doc, is);
		
		return implementation.linksHighlight;
	}
	
	/**
	 * Fetches the given {@link URL} using {@link HTMLFetcher} and processes the
	 * retrieved HTML using the specified {@link BoilerpipeExtractor}.
	 * 
	 * @param doc
	 *            The processed {@link TextDocument}.
	 * @param is
	 *            The original HTML document.
	 * @return A List of enclosed {@link Image}s
	 * @throws BoilerpipeProcessingException
	 */
	public List<Image> process(final URL url, final BoilerpipeExtractor extractor)
			throws IOException, BoilerpipeProcessingException, SAXException {
		final HTMLDocument htmlDoc = HTMLFetcher.fetch(url);

		final TextDocument doc = new BoilerpipeSAXInput(htmlDoc.toInputSource())
				.getTextDocument();
		extractor.process(doc);

		final InputSource is = htmlDoc.toInputSource();

		return process(doc, is);
	}
	

	private final class Implementation extends AbstractSAXParser implements
			ContentHandler {
		List<Image> linksHighlight = new ArrayList<Image>();
		private List<Image> linksBuffer = new ArrayList<Image>();

		private int inIgnorableElement = 0;
		private int characterElementIdx = 0;
		private final BitSet contentBitSet = new BitSet();
		
		private boolean inHighlight = false;

		Implementation() {
			super(new HTMLConfiguration());
			setContentHandler(this);
		}

		void process(final TextDocument doc, final InputSource is)
				throws BoilerpipeProcessingException {
			for (TextBlock block : doc.getTextBlocks()) {
				if (block.isContent()) {
					final BitSet bs = block.getContainedTextElements();
					if (bs != null) {
						contentBitSet.or(bs);
					}
				}
			}

			try {
				parse(is);
			} catch (SAXException e) {
				throw new BoilerpipeProcessingException(e);
			} catch (IOException e) {
				throw new BoilerpipeProcessingException(e);
			}
		}

		public void endDocument() throws SAXException {
		}

		public void endPrefixMapping(String prefix) throws SAXException {
		}

		public void ignorableWhitespace(char[] ch, int start, int length)
				throws SAXException {
		}

		public void processingInstruction(String target, String data)
				throws SAXException {
		}

		public void setDocumentLocator(Locator locator) {
		}

		public void skippedEntity(String name) throws SAXException {
		}

		public void startDocument() throws SAXException {
		}

		public void startElement(String uri, String localName, String qName,
				Attributes atts) throws SAXException {
			TagAction ta = TAG_ACTIONS.get(localName);
			if (ta != null) {
				ta.beforeStart(this, localName);
			}

			try {
				if (inIgnorableElement == 0) {
					if(inHighlight && "IMG".equalsIgnoreCase(localName)) {
						String src = atts.getValue("src");
						if(src != null && src.length() > 0) {
							linksBuffer.add(new Image(src, atts.getValue("width"), atts.getValue("height"), atts.getValue("alt")));
						}
					}
				}
			} finally {
				if (ta != null) {
					ta.afterStart(this, localName);
				}
			}
		}

		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			TagAction ta = TAG_ACTIONS.get(localName);
			if (ta != null) {
				ta.beforeEnd(this, localName);
			}

			try {
				if (inIgnorableElement == 0) {
					//
				}
			} finally {
				if (ta != null) {
					ta.afterEnd(this, localName);
				}
			}
		}

		public void characters(char[] ch, int start, int length)
				throws SAXException {
			characterElementIdx++;
			if (inIgnorableElement == 0) {

				boolean highlight = contentBitSet.get(characterElementIdx);
				if(!highlight) {
					if(length == 0) {
						return;
					}
					boolean justWhitespace = true;
					for(int i=start;i<start+length;i++) {
						if(!Character.isWhitespace(ch[i])) {
							justWhitespace = false;
							break;
						}
					}
					if(justWhitespace) {
						return;
					}
				}

				inHighlight = highlight;
				if(inHighlight) {
					linksHighlight.addAll(linksBuffer);
					linksBuffer.clear();
				}
			}
		}

		public void startPrefixMapping(String prefix, String uri)
				throws SAXException {
		}

	}
	
	
	private static final TagAction TA_IGNORABLE_ELEMENT = new TagAction() {
		void beforeStart(final Implementation instance, final String localName) {
			instance.inIgnorableElement++;
		}

		void afterEnd(final Implementation instance, final String localName) {
			instance.inIgnorableElement--;
		}
	};

	private static Map<String, TagAction> TAG_ACTIONS = new HashMap<String, TagAction>();
	static {
		TAG_ACTIONS.put("STYLE", TA_IGNORABLE_ELEMENT);
		TAG_ACTIONS.put("SCRIPT", TA_IGNORABLE_ELEMENT);
		TAG_ACTIONS.put("OPTION", TA_IGNORABLE_ELEMENT);
		TAG_ACTIONS.put("NOSCRIPT", TA_IGNORABLE_ELEMENT);
		TAG_ACTIONS.put("EMBED", TA_IGNORABLE_ELEMENT);
		TAG_ACTIONS.put("APPLET", TA_IGNORABLE_ELEMENT);
		TAG_ACTIONS.put("LINK", TA_IGNORABLE_ELEMENT);

		TAG_ACTIONS.put("HEAD", TA_IGNORABLE_ELEMENT);
	}
	
	private abstract static class TagAction {
		void beforeStart(final Implementation instance, final String localName) {
		}

		void afterStart(final Implementation instance, final String localName) {
		}

		void beforeEnd(final Implementation instance, final String localName) {
		}

		void afterEnd(final Implementation instance, final String localName) {
		}
	}
}
