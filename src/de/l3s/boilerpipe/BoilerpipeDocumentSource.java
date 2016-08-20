package de.l3s.boilerpipe;

import de.l3s.boilerpipe.document.TextDocument;

/**
 * Something that can be represented as a {@link TextDocument}.
 */
public interface BoilerpipeDocumentSource {
    TextDocument toTextDocument() throws BoilerpipeProcessingException;
}
