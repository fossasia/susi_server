/*
 * Copyright (C) 2011-2015 René Jeschke <rene_jeschke@yahoo.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// this package was migrated from com.github.rjeschke.txtmark
package org.loklak.visualization.txtmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

/**
 * Markdown processor class.
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * <code>String result = Processor.process("This is ***TXTMARK***");
 * </code>
 * </pre>
 *
 * @author René Jeschke &lt;rene_jeschke@yahoo.de&gt;
 */
public class Processor
{
    /** The reader. */
    private final Reader  reader;
    /** The emitter. */
    private final Emitter emitter;
    /** The Configuration. */
    final Configuration   config;
    /** Extension flag. */
    private boolean       useExtensions = false;

    /**
     * Constructor.
     *
     * @param reader
     *            The input reader.
     */
    private Processor(final Reader reader, final Configuration config)
    {
        this.reader = reader;
        this.config = config;
        this.useExtensions = config.forceExtendedProfile;
        this.emitter = new Emitter(this.config);
    }

    /**
     * Transforms an input stream into HTML using the given Configuration.
     *
     * @param reader
     *            The Reader to process.
     * @param configuration
     *            The Configuration.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @since 0.7
     * @see Configuration
     */
    public final static String process(final Reader reader, final Configuration configuration) throws IOException
    {
        final Processor p = new Processor(!(reader instanceof BufferedReader) ? new BufferedReader(reader) : reader,
                configuration);
        return p.process();
    }

    /**
     * Transforms an input String into HTML using the given Configuration.
     *
     * @param input
     *            The String to process.
     * @param configuration
     *            The Configuration.
     * @return The processed String.
     * @since 0.7
     * @see Configuration
     */
    public final static String process(final String input, final Configuration configuration)
    {
        try
        {
            return process(new StringReader(input), configuration);
        }
        catch (final IOException e)
        {
            // This _can never_ happen
            return null;
        }
    }

    /**
     * Transforms an input file into HTML using the given Configuration.
     *
     * @param file
     *            The File to process.
     * @param configuration
     *            the Configuration
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @since 0.7
     * @see Configuration
     */
    public final static String process(final File file, final Configuration configuration) throws IOException
    {
        final FileInputStream input = new FileInputStream(file);
        final String ret = process(input, configuration);
        input.close();
        return ret;
    }

    /**
     * Transforms an input stream into HTML using the given Configuration.
     *
     * @param input
     *            The InputStream to process.
     * @param configuration
     *            The Configuration.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @since 0.7
     * @see Configuration
     */
    public final static String process(final InputStream input, final Configuration configuration) throws IOException
    {
        final Processor p = new Processor(new BufferedReader(new InputStreamReader(input, configuration.encoding)),
                configuration);
        return p.process();
    }

    /**
     * Transforms an input String into HTML using the default Configuration.
     *
     * @param input
     *            The String to process.
     * @return The processed String.
     * @see Configuration#DEFAULT
     */
    public final static String process(final String input)
    {
        return process(input, Configuration.DEFAULT);
    }

    /**
     * Transforms an input String into HTML.
     *
     * @param input
     *            The String to process.
     * @param safeMode
     *            Set to <code>true</code> to escape unsafe HTML tags.
     * @return The processed String.
     * @see Configuration#DEFAULT
     */
    public final static String process(final String input, final boolean safeMode)
    {
        return process(input, Configuration.builder().setSafeMode(safeMode).build());
    }

    /**
     * Transforms an input String into HTML.
     *
     * @param input
     *            The String to process.
     * @param decorator
     *            The decorator to use.
     * @return The processed String.
     * @see Configuration#DEFAULT
     */
    public final static String process(final String input, final Decorator decorator)
    {
        return process(input, Configuration.builder().setDecorator(decorator).build());
    }

    /**
     * Transforms an input String into HTML.
     *
     * @param input
     *            The String to process.
     * @param decorator
     *            The decorator to use.
     * @param safeMode
     *            Set to <code>true</code> to escape unsafe HTML tags.
     * @return The processed String.
     * @see Configuration#DEFAULT
     */
    public final static String process(final String input, final Decorator decorator, final boolean safeMode)
    {
        return process(input, Configuration.builder().setDecorator(decorator).setSafeMode(safeMode).build());
    }

    /**
     * Transforms an input file into HTML using the default Configuration.
     *
     * @param file
     *            The File to process.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final File file) throws IOException
    {
        return process(file, Configuration.DEFAULT);
    }

    /**
     * Transforms an input file into HTML.
     *
     * @param file
     *            The File to process.
     * @param safeMode
     *            Set to <code>true</code> to escape unsafe HTML tags.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final File file, final boolean safeMode) throws IOException
    {
        return process(file, Configuration.builder().setSafeMode(safeMode).build());
    }

    /**
     * Transforms an input file into HTML.
     *
     * @param file
     *            The File to process.
     * @param decorator
     *            The decorator to use.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final File file, final Decorator decorator) throws IOException
    {
        return process(file, Configuration.builder().setDecorator(decorator).build());
    }

    /**
     * Transforms an input file into HTML.
     *
     * @param file
     *            The File to process.
     * @param decorator
     *            The decorator to use.
     * @param safeMode
     *            Set to <code>true</code> to escape unsafe HTML tags.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final File file, final Decorator decorator, final boolean safeMode)
            throws IOException
    {
        return process(file, Configuration.builder().setDecorator(decorator).setSafeMode(safeMode).build());
    }

    /**
     * Transforms an input file into HTML.
     *
     * @param file
     *            The File to process.
     * @param encoding
     *            The encoding to use.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final File file, final String encoding) throws IOException
    {
        return process(file, Configuration.builder().setEncoding(encoding).build());
    }

    /**
     * Transforms an input file into HTML.
     *
     * @param file
     *            The File to process.
     * @param encoding
     *            The encoding to use.
     * @param safeMode
     *            Set to <code>true</code> to escape unsafe HTML tags.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final File file, final String encoding, final boolean safeMode)
            throws IOException
    {
        return process(file, Configuration.builder().setEncoding(encoding).setSafeMode(safeMode).build());
    }

    /**
     * Transforms an input file into HTML.
     *
     * @param file
     *            The File to process.
     * @param encoding
     *            The encoding to use.
     * @param decorator
     *            The decorator to use.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final File file, final String encoding, final Decorator decorator)
            throws IOException
    {
        return process(file, Configuration.builder().setEncoding(encoding).setDecorator(decorator).build());
    }

    /**
     * Transforms an input file into HTML.
     *
     * @param file
     *            The File to process.
     * @param encoding
     *            The encoding to use.
     * @param decorator
     *            The decorator to use.
     * @param safeMode
     *            Set to <code>true</code> to escape unsafe HTML tags.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final File file, final String encoding, final Decorator decorator,
            final boolean safeMode) throws IOException
    {
        return process(file, Configuration.builder().setEncoding(encoding).setSafeMode(safeMode)
                .setDecorator(decorator).build());
    }

    /**
     * Transforms an input stream into HTML.
     *
     * @param input
     *            The InputStream to process.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final InputStream input) throws IOException
    {
        return process(input, Configuration.DEFAULT);
    }

    /**
     * Transforms an input stream into HTML.
     *
     * @param input
     *            The InputStream to process.
     * @param safeMode
     *            Set to <code>true</code> to escape unsafe HTML tags.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final InputStream input, final boolean safeMode) throws IOException
    {
        return process(input, Configuration.builder().setSafeMode(safeMode).build());
    }

    /**
     * Transforms an input stream into HTML.
     *
     * @param input
     *            The InputStream to process.
     * @param decorator
     *            The decorator to use.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final InputStream input, final Decorator decorator) throws IOException
    {
        return process(input, Configuration.builder().setDecorator(decorator).build());
    }

    /**
     * Transforms an input stream into HTML.
     *
     * @param input
     *            The InputStream to process.
     * @param decorator
     *            The decorator to use.
     * @param safeMode
     *            Set to <code>true</code> to escape unsafe HTML tags.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final InputStream input, final Decorator decorator, final boolean safeMode)
            throws IOException
    {
        return process(input, Configuration.builder().setDecorator(decorator).setSafeMode(safeMode).build());
    }

    /**
     * Transforms an input stream into HTML.
     *
     * @param input
     *            The InputStream to process.
     * @param encoding
     *            The encoding to use.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final InputStream input, final String encoding) throws IOException
    {
        return process(input, Configuration.builder().setEncoding(encoding).build());
    }

    /**
     * Transforms an input stream into HTML.
     *
     * @param input
     *            The InputStream to process.
     * @param encoding
     *            The encoding to use.
     * @param safeMode
     *            Set to <code>true</code> to escape unsafe HTML tags.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final InputStream input, final String encoding, final boolean safeMode)
            throws IOException
    {
        return process(input, Configuration.builder().setEncoding(encoding).setSafeMode(safeMode).build());
    }

    /**
     * Transforms an input stream into HTML.
     *
     * @param input
     *            The InputStream to process.
     * @param encoding
     *            The encoding to use.
     * @param decorator
     *            The decorator to use.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final InputStream input, final String encoding, final Decorator decorator)
            throws IOException
    {
        return process(input, Configuration.builder().setEncoding(encoding).setDecorator(decorator).build());
    }

    /**
     * Transforms an input stream into HTML.
     *
     * @param input
     *            The InputStream to process.
     * @param encoding
     *            The encoding to use.
     * @param decorator
     *            The decorator to use.
     * @param safeMode
     *            Set to <code>true</code> to escape unsafe HTML tags.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final InputStream input, final String encoding, final Decorator decorator,
            final boolean safeMode) throws IOException
    {
        return process(input,
                Configuration.builder().setEncoding(encoding).setDecorator(decorator).setSafeMode(safeMode).build());
    }

    /**
     * Transforms an input stream into HTML using the default Configuration.
     *
     * @param reader
     *            The Reader to process.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final Reader reader) throws IOException
    {
        return process(reader, Configuration.DEFAULT);
    }

    /**
     * Transforms an input stream into HTML.
     *
     * @param reader
     *            The Reader to process.
     * @param safeMode
     *            Set to <code>true</code> to escape unsafe HTML tags.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final Reader reader, final boolean safeMode) throws IOException
    {
        return process(reader, Configuration.builder().setSafeMode(safeMode).build());
    }

    /**
     * Transforms an input stream into HTML.
     *
     * @param reader
     *            The Reader to process.
     * @param decorator
     *            The decorator to use.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final Reader reader, final Decorator decorator) throws IOException
    {
        return process(reader, Configuration.builder().setDecorator(decorator).build());
    }

    /**
     * Transforms an input stream into HTML.
     *
     * @param reader
     *            The Reader to process.
     * @param decorator
     *            The decorator to use.
     * @param safeMode
     *            Set to <code>true</code> to escape unsafe HTML tags.
     * @return The processed String.
     * @throws IOException
     *             if an IO error occurs
     * @see Configuration#DEFAULT
     */
    public final static String process(final Reader reader, final Decorator decorator, final boolean safeMode)
            throws IOException
    {
        return process(reader, Configuration.builder().setDecorator(decorator).setSafeMode(safeMode).build());
    }

    /**
     * Reads all lines from our reader.
     * <p>
     * Takes care of markdown link references.
     * </p>
     *
     * @return A Block containing all lines.
     * @throws IOException
     *             If an IO error occurred.
     */
    private Block readLines() throws IOException
    {
        final Block block = new Block();
        final StringBuilder sb = new StringBuilder(80);
        int c = this.reader.read();
        LinkRef lastLinkRef = null;
        while (c != -1)
        {
            sb.setLength(0);
            int pos = 0;
            boolean eol = false;
            while (!eol)
            {
                switch (c)
                {
                case -1:
                    eol = true;
                    break;
                case '\n':
                    c = this.reader.read();
                    if (c == '\r')
                    {
                        c = this.reader.read();
                    }
                    eol = true;
                    break;
                case '\r':
                    c = this.reader.read();
                    if (c == '\n')
                    {
                        c = this.reader.read();
                    }
                    eol = true;
                    break;
                case '\t':
                {
                    final int np = pos + (4 - (pos & 3));
                    while (pos < np)
                    {
                        sb.append(' ');
                        pos++;
                    }
                    c = this.reader.read();
                    break;
                }
                default:
                    if (c != '<' || !this.config.panicMode)
                    {
                        pos++;
                        sb.append((char)c);
                    }
                    else
                    {
                        pos += 4;
                        sb.append("&lt;");
                    }
                    c = this.reader.read();
                    break;
                }
            }

            final Line line = new Line();
            line.value = sb.toString();
            line.init();

            // Check for link definitions
            boolean isLinkRef = false;
            String id = null, link = null, comment = null;
            if (!line.isEmpty && line.leading < 4 && line.value.charAt(line.leading) == '[')
            {
                line.pos = line.leading + 1;
                // Read ID up to ']'
                id = line.readUntil(']');
                // Is ID valid and are there any more characters?
                if (id != null && line.pos + 2 < line.value.length())
                {
                    // Check for ':' ([...]:...)
                    if (line.value.charAt(line.pos + 1) == ':')
                    {
                        line.pos += 2;
                        line.skipSpaces();
                        // Check for link syntax
                        if (line.value.charAt(line.pos) == '<')
                        {
                            line.pos++;
                            link = line.readUntil('>');
                            line.pos++;
                        }
                        else
                        {
                            link = line.readUntil(' ', '\n');
                        }

                        // Is link valid?
                        if (link != null)
                        {
                            // Any non-whitespace characters following?
                            if (line.skipSpaces())
                            {
                                final char ch = line.value.charAt(line.pos);
                                // Read comment
                                if (ch == '\"' || ch == '\'' || ch == '(')
                                {
                                    line.pos++;
                                    comment = line.readUntil(ch == '(' ? ')' : ch);
                                    // Valid linkRef only if comment is valid
                                    if (comment != null)
                                    {
                                        isLinkRef = true;
                                    }
                                }
                            }
                            else
                            {
                                isLinkRef = true;
                            }
                        }
                    }
                }
            }

            // To make compiler happy: add != null checks
            if (isLinkRef && id != null && link != null)
            {
                if (id.toLowerCase().equals("$profile$"))
                {
                    this.emitter.useExtensions = this.useExtensions = link.toLowerCase().equals("extended");
                    lastLinkRef = null;
                }
                else
                {
                    // Store linkRef and skip line
                    final LinkRef lr = new LinkRef(link, comment, comment != null
                            && (link.length() == 1 && link.charAt(0) == '*'));
                    this.emitter.addLinkRef(id, lr);
                    if (comment == null)
                    {
                        lastLinkRef = lr;
                    }
                }
            }
            else
            {
                comment = null;
                // Check for multi-line linkRef
                if (!line.isEmpty && lastLinkRef != null)
                {
                    line.pos = line.leading;
                    final char ch = line.value.charAt(line.pos);
                    if (ch == '\"' || ch == '\'' || ch == '(')
                    {
                        line.pos++;
                        comment = line.readUntil(ch == '(' ? ')' : ch);
                    }
                    if (comment != null)
                    {
                        lastLinkRef.title = comment;
                    }

                    lastLinkRef = null;
                }

                // No multi-line linkRef, store line
                if (comment == null)
                {
                    line.pos = 0;
                    block.appendLine(line);
                }
            }
        }

        return block;
    }

    /**
     * Initializes a list block by separating it into list item blocks.
     *
     * @param root
     *            The Block to process.
     */
    private void initListBlock(final Block root)
    {
        Line line = root.lines;
        line = line.next;
        while (line != null)
        {
            final LineType t = line.getLineType(this.config);
            if ((t == LineType.OLIST || t == LineType.ULIST)
                    || (!line.isEmpty && (line.prevEmpty && line.leading == 0 && !(t == LineType.OLIST || t == LineType.ULIST))))
            {
                root.split(line.previous).type = BlockType.LIST_ITEM;
            }
            line = line.next;
        }
        root.split(root.lineTail).type = BlockType.LIST_ITEM;
    }

    /**
     * Recursively process the given Block.
     *
     * @param root
     *            The Block to process.
     * @param listMode
     *            Flag indicating that we're in a list item block.
     */
    private void recurse(final Block root, final boolean listMode)
    {
        Block block, list;
        Line line = root.lines;

        if (listMode)
        {
            root.removeListIndent(this.config);
            if (this.useExtensions && root.lines != null && root.lines.getLineType(this.config) != LineType.CODE)
            {
                root.id = root.lines.stripID();
            }
        }

        while (line != null && line.isEmpty)
        {
            line = line.next;
        }
        if (line == null)
        {
            return;
        }

        while (line != null)
        {
            final LineType type = line.getLineType(this.config);
            switch (type)
            {
            case OTHER:
            {
                final boolean wasEmpty = line.prevEmpty;
                while (line != null && !line.isEmpty)
                {
                    final LineType t = line.getLineType(this.config);
                    if ((listMode || this.useExtensions) && (t == LineType.OLIST || t == LineType.ULIST))
                    {
                        break;
                    }
                    if (this.useExtensions && (t == LineType.CODE || t == LineType.FENCED_CODE))
                    {
                        break;
                    }
                    if (t == LineType.HEADLINE || t == LineType.HEADLINE1 || t == LineType.HEADLINE2
                            || t == LineType.HR
                            || t == LineType.BQUOTE || t == LineType.XML)
                    {
                        break;
                    }
                    line = line.next;
                }
                final BlockType bt;
                if (line != null && !line.isEmpty)
                {
                    bt = (listMode && !wasEmpty) ? BlockType.NONE : BlockType.PARAGRAPH;
                    root.split(line.previous).type = bt;
                    root.removeLeadingEmptyLines();
                }
                else
                {
                    bt = (listMode && (line == null || !line.isEmpty) && !wasEmpty) ? BlockType.NONE
                            : BlockType.PARAGRAPH;
                    root.split(line == null ? root.lineTail : line).type = bt;
                    root.removeLeadingEmptyLines();
                }
                line = root.lines;
                break;
            }
            case CODE:
                while (line != null && (line.isEmpty || line.leading > 3))
                {
                    line = line.next;
                }
                block = root.split(line != null ? line.previous : root.lineTail);
                block.type = BlockType.CODE;
                block.removeSurroundingEmptyLines();
                break;
            case XML:
                if (line.previous != null)
                {
                    // FIXME ... this looks wrong
                    root.split(line.previous);
                }
                root.split(line.xmlEndLine).type = BlockType.XML;
                root.removeLeadingEmptyLines();
                line = root.lines;
                break;
            case BQUOTE:
                while (line != null)
                {
                    if (!line.isEmpty
                            && (line.prevEmpty && line.leading == 0 && line.getLineType(this.config) != LineType.BQUOTE))
                    {
                        break;
                    }
                    line = line.next;
                }
                block = root.split(line != null ? line.previous : root.lineTail);
                block.type = BlockType.BLOCKQUOTE;
                block.removeSurroundingEmptyLines();
                block.removeBlockQuotePrefix();
                this.recurse(block, false);
                line = root.lines;
                break;
            case HR:
                if (line.previous != null)
                {
                    // FIXME ... this looks wrong
                    root.split(line.previous);
                }
                root.split(line).type = BlockType.RULER;
                root.removeLeadingEmptyLines();
                line = root.lines;
                break;
            case FENCED_CODE:
                line = line.next;
                while (line != null)
                {
                    if (line.getLineType(this.config) == LineType.FENCED_CODE)
                    {
                        break;
                    }
                    // TODO ... is this really necessary? Maybe add a special
                    // flag?
                    line = line.next;
                }
                if (line != null)
                {
                    line = line.next;
                }
                block = root.split(line != null ? line.previous : root.lineTail);
                block.type = BlockType.FENCED_CODE;
                block.meta = Utils.getMetaFromFence(block.lines.value);
                block.lines.setEmpty();
                if (block.lineTail.getLineType(this.config) == LineType.FENCED_CODE)
                {
                    block.lineTail.setEmpty();
                }
                block.removeSurroundingEmptyLines();
                break;
            case HEADLINE:
            case HEADLINE1:
            case HEADLINE2:
                if (line.previous != null)
                {
                    root.split(line.previous);
                }
                if (type != LineType.HEADLINE)
                {
                    line.next.setEmpty();
                }
                block = root.split(line);
                block.type = BlockType.HEADLINE;
                if (type != LineType.HEADLINE)
                {
                    block.hlDepth = type == LineType.HEADLINE1 ? 1 : 2;
                }
                if (this.useExtensions)
                {
                    block.id = block.lines.stripID();
                }
                block.transfromHeadline();
                root.removeLeadingEmptyLines();
                line = root.lines;
                break;
            case OLIST:
            case ULIST:
                while (line != null)
                {
                    final LineType t = line.getLineType(this.config);
                    if (!line.isEmpty
                            && (line.prevEmpty && line.leading == 0 && !(t == LineType.OLIST || t == LineType.ULIST)))
                    {
                        break;
                    }
                    line = line.next;
                }
                list = root.split(line != null ? line.previous : root.lineTail);
                list.type = type == LineType.OLIST ? BlockType.ORDERED_LIST : BlockType.UNORDERED_LIST;
                list.lines.prevEmpty = false;
                list.lineTail.nextEmpty = false;
                list.removeSurroundingEmptyLines();
                list.lines.prevEmpty = list.lineTail.nextEmpty = false;
                this.initListBlock(list);
                block = list.blocks;
                while (block != null)
                {
                    this.recurse(block, true);
                    block = block.next;
                }
                list.expandListParagraphs();
                break;
            default:
                line = line.next;
                break;
            }
        }
    }

    /**
     * Does all the processing.
     *
     * @return The processed String.
     * @throws IOException
     *             If an IO error occurred.
     */
    private String process() throws IOException
    {
        final StringBuilder out = new StringBuilder();
        final Block parent = this.readLines();
        parent.removeSurroundingEmptyLines();

        this.recurse(parent, false);
        Block block = parent.blocks;
        while (block != null)
        {
            this.emitter.emit(out, block);
            block = block.next;
        }

        return out.toString();
    }
}
