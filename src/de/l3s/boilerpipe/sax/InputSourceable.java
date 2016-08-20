package de.l3s.boilerpipe.sax;

import org.xml.sax.InputSource;

/**
 * An InputSourceable can return an arbitrary number of new {@link InputSource}s for a given document.
 * 
 * @author Christian Kohlschütter
 */
public interface InputSourceable {
	InputSource toInputSource();
}
