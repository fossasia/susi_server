package de.l3s.boilerpipe.filters.heuristics;

import java.util.regex.Pattern;

import de.l3s.boilerpipe.BoilerpipeFilter;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.labels.DefaultLabels;

public class ArticleMetadataFilter implements BoilerpipeFilter {
	private static final Pattern[] PATTERNS_SHORT = new Pattern[] {
		Pattern
			.compile("^[0-9 \\,\\./]*\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December)?\\b[0-9 \\,\\:apm\\./]*([CPSDMGET]{2,3})?$"),
			Pattern.compile("^[Bb]y ")
			};


	public static final ArticleMetadataFilter INSTANCE = new ArticleMetadataFilter();
	
	private ArticleMetadataFilter() {
	}
	@Override
	public boolean process(TextDocument doc)
			throws BoilerpipeProcessingException {
		boolean changed = false;
		for (TextBlock tb : doc.getTextBlocks()) {
			if (tb.getNumWords() > 10) {
				continue;
			}
			final String text = tb.getText();
			for (Pattern p : PATTERNS_SHORT) {
				if (p.matcher(text).find()) {
					changed = true;
					tb.setIsContent(true);
					tb.addLabel(DefaultLabels.ARTICLE_METADATA);
				}
			}
		}
		return changed;
	}

}
