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

/**
 * This class represents a block of lines.
 *
 * @author René Jeschke <rene_jeschke@yahoo.de>
 */
class Block
{
    /** This block's type. */
    public BlockType type    = BlockType.NONE;
    /** Head and tail of linked lines. */
    public Line      lines   = null, lineTail = null;
    /** Head and tail of child blocks. */
    public Block     blocks  = null, blockTail = null;
    /** Next block. */
    public Block     next    = null;
    /** Depth of headline BlockType. */
    public int       hlDepth = 0;
    /** ID for headlines and list items */
    public String    id      = null;
    /** Block meta information */
    public String    meta    = "";

    /** Constructor. */
    public Block()
    {
        //
    }

    /**
     * @return <code>true</code> if this block contains lines.
     */
    public boolean hasLines()
    {
        return this.lines != null;
    }

    /**
     * Removes leading and trailing empty lines.
     */
    public void removeSurroundingEmptyLines()
    {
        if (this.lines != null)
        {
            this.removeTrailingEmptyLines();
            this.removeLeadingEmptyLines();
        }
    }

    /**
     * Sets <code>hlDepth</code> and takes care of '#' chars.
     */
    public void transfromHeadline()
    {
        if (this.hlDepth > 0)
        {
            return;
        }
        int level = 0;
        final Line line = this.lines;
        if (line.isEmpty)
        {
            return;
        }
        int start = line.leading;
        while (start < line.value.length() && line.value.charAt(start) == '#')
        {
            level++;
            start++;
        }
        while (start < line.value.length() && line.value.charAt(start) == ' ')
        {
            start++;
        }
        if (start >= line.value.length())
        {
            line.setEmpty();
        }
        else
        {
            int end = line.value.length() - line.trailing - 1;
            while (line.value.charAt(end) == '#')
            {
                end--;
            }
            while (line.value.charAt(end) == ' ')
            {
                end--;
            }
            line.value = line.value.substring(start, end + 1);
            line.leading = line.trailing = 0;
        }
        this.hlDepth = Math.min(level, 6);
    }

    /**
     * Used for nested lists. Removes list markers and up to 4 leading spaces.
     *
     * @param configuration
     *            txtmark configuration
     *
     */
    public void removeListIndent(final Configuration configuration)
    {
        Line line = this.lines;
        while (line != null)
        {
            if (!line.isEmpty)
            {
                switch (line.getLineType(configuration))
                {
                case ULIST:
                    line.value = line.value.substring(line.leading + 2);
                    break;
                case OLIST:
                    line.value = line.value.substring(line.value.indexOf('.') + 2);
                    break;
                default:
                    line.value = line.value.substring(Math.min(line.leading, 4));
                    break;
                }
                line.initLeading();
            }
            line = line.next;
        }
    }

    /**
     * Used for nested block quotes. Removes '>' char.
     */
    public void removeBlockQuotePrefix()
    {
        Line line = this.lines;
        while (line != null)
        {
            if (!line.isEmpty)
            {
                if (line.value.charAt(line.leading) == '>')
                {
                    int rem = line.leading + 1;
                    if (line.leading + 1 < line.value.length() && line.value.charAt(line.leading + 1) == ' ')
                    {
                        rem++;
                    }
                    line.value = line.value.substring(rem);
                    line.initLeading();
                }
            }
            line = line.next;
        }
    }

    /**
     * Removes leading empty lines.
     *
     * @return <code>true</code> if an empty line was removed.
     */
    public boolean removeLeadingEmptyLines()
    {
        boolean wasEmpty = false;
        Line line = this.lines;
        while (line != null && line.isEmpty)
        {
            this.removeLine(line);
            line = this.lines;
            wasEmpty = true;
        }
        return wasEmpty;
    }

    /**
     * Removes trailing empty lines.
     */
    public void removeTrailingEmptyLines()
    {
        Line line = this.lineTail;
        while (line != null && line.isEmpty)
        {
            this.removeLine(line);
            line = this.lineTail;
        }
    }

    /**
     * Splits this block's lines, creating a new child block having 'line' as
     * it's lineTail.
     *
     * @param line
     *            The line to split from.
     * @return The newly created Block.
     */
    public Block split(final Line line)
    {
        final Block block = new Block();

        block.lines = this.lines;
        block.lineTail = line;
        this.lines = line.next;
        line.next = null;
        if (this.lines == null)
        {
            this.lineTail = null;
        }
        else
        {
            this.lines.previous = null;
        }

        if (this.blocks == null)
        {
            this.blocks = this.blockTail = block;
        }
        else
        {
            this.blockTail.next = block;
            this.blockTail = block;
        }

        return block;
    }

    /**
     * Removes the given line from this block.
     *
     * @param line
     *            Line to remove.
     */
    public void removeLine(final Line line)
    {
        if (line.previous == null)
        {
            this.lines = line.next;
        }
        else
        {
            line.previous.next = line.next;
        }
        if (line.next == null)
        {
            this.lineTail = line.previous;
        }
        else
        {
            line.next.previous = line.previous;
        }
        line.previous = line.next = null;
    }

    /**
     * Appends the given line to this block.
     *
     * @param line
     *            Line to append.
     */
    public void appendLine(final Line line)
    {
        if (this.lineTail == null)
        {
            this.lines = this.lineTail = line;
        }
        else
        {
            this.lineTail.nextEmpty = line.isEmpty;
            line.prevEmpty = this.lineTail.isEmpty;
            line.previous = this.lineTail;
            this.lineTail.next = line;
            this.lineTail = line;
        }
    }

    /**
     * Changes all Blocks of type <code>NONE</code> to <code>PARAGRAPH</code> if
     * this Block is a List and any of the ListItems contains a paragraph.
     */
    public void expandListParagraphs()
    {
        if (this.type != BlockType.ORDERED_LIST && this.type != BlockType.UNORDERED_LIST)
        {
            return;
        }
        Block outer = this.blocks, inner;
        boolean hasParagraph = false;
        while (outer != null && !hasParagraph)
        {
            if (outer.type == BlockType.LIST_ITEM)
            {
                inner = outer.blocks;
                while (inner != null && !hasParagraph)
                {
                    if (inner.type == BlockType.PARAGRAPH)
                    {
                        hasParagraph = true;
                    }
                    inner = inner.next;
                }
            }
            outer = outer.next;
        }
        if (hasParagraph)
        {
            outer = this.blocks;
            while (outer != null)
            {
                if (outer.type == BlockType.LIST_ITEM)
                {
                    inner = outer.blocks;
                    while (inner != null)
                    {
                        if (inner.type == BlockType.NONE)
                        {
                            inner.type = BlockType.PARAGRAPH;
                        }
                        inner = inner.next;
                    }
                }
                outer = outer.next;
            }
        }
    }
}
