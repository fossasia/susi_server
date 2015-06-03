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

import java.util.LinkedList;

/**
 * This class represents a text line.
 *
 * <p>
 * It also provides methods for processing and analyzing a line.
 * </p>
 *
 * @author René Jeschke <rene_jeschke@yahoo.de>
 */
class Line
{
    /** Current cursor position. */
    public int     pos;
    /** Leading and trailing spaces. */
    public int     leading = 0, trailing = 0;
    /** Is this line empty? */
    public boolean isEmpty = true;
    /** This line's value. */
    public String  value   = null;
    /** Previous and next line. */
    public Line    previous = null, next = null;
    /** Is previous/next line empty? */
    public boolean prevEmpty, nextEmpty;
    /** Final line of a XML block. */
    public Line    xmlEndLine;

    /** Constructor. */
    public Line()
    {
        //
    }

    /**
     * Calculates leading and trailing spaces. Also sets empty if needed.
     */
    public void init()
    {
        this.leading = 0;
        while (this.leading < this.value.length() && this.value.charAt(this.leading) == ' ')
        {
            this.leading++;
        }

        if (this.leading == this.value.length())
        {
            this.setEmpty();
        }
        else
        {
            this.isEmpty = false;
            this.trailing = 0;
            while (this.value.charAt(this.value.length() - this.trailing - 1) == ' ')
            {
                this.trailing++;
            }
        }
    }

    /**
     * Recalculate leading spaces.
     */
    public void initLeading()
    {
        this.leading = 0;
        while (this.leading < this.value.length() && this.value.charAt(this.leading) == ' ')
        {
            this.leading++;
        }

        if (this.leading == this.value.length())
        {
            this.setEmpty();
        }
    }

    /**
     * Skips spaces.
     *
     * @return <code>false</code> if end of line is reached
     */
    // TODO use Util#skipSpaces
    public boolean skipSpaces()
    {
        while (this.pos < this.value.length() && this.value.charAt(this.pos) == ' ')
        {
            this.pos++;
        }
        return this.pos < this.value.length();
    }

    /**
     * Reads chars from this line until any 'end' char is reached.
     *
     * @param end
     *            Delimiting character(s)
     * @return The read String or <code>null</code> if no 'end' char was
     *         reached.
     */
    // TODO use Util#readUntil
    public String readUntil(final char... end)
    {
        final StringBuilder sb = new StringBuilder();
        int pos = this.pos;
        while (pos < this.value.length())
        {
            final char ch = this.value.charAt(pos);
            if (ch == '\\' && pos + 1 < this.value.length())
            {
                final char c;
                switch (c = this.value.charAt(pos + 1))
                {
                case '\\':
                case '[':
                case ']':
                case '(':
                case ')':
                case '{':
                case '}':
                case '#':
                case '"':
                case '\'':
                case '.':
                case '>':
                case '*':
                case '+':
                case '-':
                case '_':
                case '!':
                case '`':
                case '~':
                    sb.append(c);
                    pos++;
                    break;
                default:
                    sb.append(ch);
                    break;
                }
            }
            else
            {
                boolean endReached = false;
                for (int n = 0; n < end.length; n++)
                {
                    if (ch == end[n])
                    {
                        endReached = true;
                        break;
                    }
                }
                if (endReached)
                {
                    break;
                }
                sb.append(ch);
            }
            pos++;
        }

        final char ch = pos < this.value.length() ? this.value.charAt(pos) : '\n';
        for (int n = 0; n < end.length; n++)
        {
            if (ch == end[n])
            {
                this.pos = pos;
                return sb.toString();
            }
        }
        return null;
    }

    /**
     * Marks this line empty. Also sets previous/next line's empty attributes.
     */
    public void setEmpty()
    {
        this.value = "";
        this.leading = this.trailing = 0;
        this.isEmpty = true;
        if (this.previous != null)
        {
            this.previous.nextEmpty = true;
        }
        if (this.next != null)
        {
            this.next.prevEmpty = true;
        }
    }

    /**
     * Counts the amount of 'ch' in this line.
     *
     * @param ch
     *            The char to count.
     * @return A value > 0 if this line only consists of 'ch' end spaces.
     */
    private int countChars(final char ch)
    {
        int count = 0;
        for (int i = 0; i < this.value.length(); i++)
        {
            final char c = this.value.charAt(i);
            if (c == ' ')
            {
                continue;
            }
            if (c == ch)
            {
                count++;
                continue;
            }
            count = 0;
            break;
        }
        return count;
    }

    /**
     * Counts the amount of 'ch' at the start of this line optionally ignoring
     * spaces.
     *
     * @param ch
     *            The char to count.
     * @param allowSpaces
     *            Whether to allow spaces or not
     * @return Number of characters found.
     * @since 0.12
     */
    private int countCharsStart(final char ch, final boolean allowSpaces)
    {
        int count = 0;
        for (int i = 0; i < this.value.length(); i++)
        {
            final char c = this.value.charAt(i);
            if (c == ' ' && allowSpaces)
            {
                continue;
            }
            if (c == ch)
            {
                count++;
            }
            else
            {
                break;
            }
        }
        return count;
    }

    /**
     * Gets this line's type.
     *
     * @param configuration
     *            txtmark configuration
     *
     * @return The LineType.
     */
    public LineType getLineType(final Configuration configuration)
    {
        if (this.isEmpty)
        {
            return LineType.EMPTY;
        }

        if (this.leading > 3)
        {
            return LineType.CODE;
        }

        if (this.value.charAt(this.leading) == '#')
        {
            return LineType.HEADLINE;
        }

        if (this.value.charAt(this.leading) == '>')
        {
            return LineType.BQUOTE;
        }

        if (configuration.forceExtendedProfile)
        {
            if (this.value.length() - this.leading - this.trailing > 2)
            {
                if (this.value.charAt(this.leading) == '`'
                        && this.countCharsStart('`', configuration.allowSpacesInFencedDelimiters) >= 3)
                {
                    return LineType.FENCED_CODE;
                }
                if (this.value.charAt(this.leading) == '~'
                        && this.countCharsStart('~', configuration.allowSpacesInFencedDelimiters) >= 3)
                {
                    return LineType.FENCED_CODE;
                }
            }
        }

        if (this.value.length() - this.leading - this.trailing > 2
                && (this.value.charAt(this.leading) == '*' || this.value.charAt(this.leading) == '-' || this.value
                        .charAt(this.leading) == '_'))
        {
            if (this.countChars(this.value.charAt(this.leading)) >= 3)
            {
                return LineType.HR;
            }
        }

        if (this.value.length() - this.leading >= 2 && this.value.charAt(this.leading + 1) == ' ')
        {
            switch (this.value.charAt(this.leading))
            {
            case '*':
            case '-':
            case '+':
                return LineType.ULIST;
            }
        }

        if (this.value.length() - this.leading >= 3 && Character.isDigit(this.value.charAt(this.leading)))
        {
            int i = this.leading + 1;
            while (i < this.value.length() && Character.isDigit(this.value.charAt(i)))
            {
                i++;
            }
            if (i + 1 < this.value.length() && this.value.charAt(i) == '.' && this.value.charAt(i + 1) == ' ')
            {
                return LineType.OLIST;
            }
        }

        if (this.value.charAt(this.leading) == '<')
        {
            if (this.checkHTML())
            {
                return LineType.XML;
            }
        }

        if (this.next != null && !this.next.isEmpty)
        {
            if ((this.next.value.charAt(0) == '-') && (this.next.countChars('-') > 0))
            {
                return LineType.HEADLINE2;
            }
            if ((this.next.value.charAt(0) == '=') && (this.next.countChars('=') > 0))
            {
                return LineType.HEADLINE1;
            }
        }

        return LineType.OTHER;
    }

    /**
     * Reads an XML comment. Sets <code>xmlEndLine</code>.
     *
     * @param firstLine
     *            The Line to start reading from.
     * @param start
     *            The starting position.
     * @return The new position or -1 if it is no valid comment.
     */
    private int readXMLComment(final Line firstLine, final int start)
    {
        Line line = firstLine;
        if (start + 3 < line.value.length())
        {
            if (line.value.charAt(2) == '-' && line.value.charAt(3) == '-')
            {
                int pos = start + 4;
                while (line != null)
                {
                    while (pos < line.value.length() && line.value.charAt(pos) != '-')
                    {
                        pos++;
                    }
                    if (pos == line.value.length())
                    {
                        line = line.next;
                        pos = 0;
                    }
                    else
                    {
                        if (pos + 2 < line.value.length())
                        {
                            if (line.value.charAt(pos + 1) == '-' && line.value.charAt(pos + 2) == '>')
                            {
                                this.xmlEndLine = line;
                                return pos + 3;
                            }
                        }
                        pos++;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Checks if this line contains an ID at it's end and removes it from the
     * line.
     *
     * @return The ID or <code>null</code> if no valid ID exists.
     */
    // FIXME ... hack
    public String stripID()
    {
        if (this.isEmpty || this.value.charAt(this.value.length() - this.trailing - 1) != '}')
        {
            return null;
        }
        int p = this.leading;
        boolean found = false;
        while (p < this.value.length() && !found)
        {
            switch (this.value.charAt(p))
            {
            case '\\':
                if (p + 1 < this.value.length())
                {
                    switch (this.value.charAt(p + 1))
                    {
                    case '{':
                        p++;
                        break;
                    }
                }
                p++;
                break;
            case '{':
                found = true;
                break;
            default:
                p++;
                break;
            }
        }

        if (found)
        {
            if (p + 1 < this.value.length() && this.value.charAt(p + 1) == '#')
            {
                final int start = p + 2;
                p = start;
                found = false;
                while (p < this.value.length() && !found)
                {
                    switch (this.value.charAt(p))
                    {
                    case '\\':
                        if (p + 1 < this.value.length())
                        {
                            switch (this.value.charAt(p + 1))
                            {
                            case '}':
                                p++;
                                break;
                            }
                        }
                        p++;
                        break;
                    case '}':
                        found = true;
                        break;
                    default:
                        p++;
                        break;
                    }
                }
                if (found)
                {
                    final String id = this.value.substring(start, p).trim();
                    if (this.leading != 0)
                    {
                        this.value = this.value.substring(0, this.leading)
                                + this.value.substring(this.leading, start - 2).trim();
                    }
                    else
                    {
                        this.value = this.value.substring(this.leading, start - 2).trim();
                    }
                    this.trailing = 0;
                    return id.length() > 0 ? id : null;
                }
            }
        }
        return null;
    }

    /**
     * Checks for a valid HTML block. Sets <code>xmlEndLine</code>.
     *
     * @return <code>true</code> if it is a valid block.
     */
    private boolean checkHTML()
    {
        final LinkedList<String> tags = new LinkedList<String>();
        final StringBuilder temp = new StringBuilder();
        int pos = this.leading;
        if (this.value.charAt(this.leading + 1) == '!')
        {
            if (this.readXMLComment(this, this.leading) > 0)
            {
                return true;
            }
        }
        pos = Utils.readXML(temp, this.value, this.leading, false);
        String element, tag;
        if (pos > -1)
        {
            element = temp.toString();
            temp.setLength(0);
            Utils.getXMLTag(temp, element);
            tag = temp.toString().toLowerCase();
            if (!HTML.isHtmlBlockElement(tag))
            {
                return false;
            }
            if (tag.equals("hr"))
            {
                this.xmlEndLine = this;
                return true;
            }
            tags.add(tag);

            Line line = this;
            while (line != null)
            {
                while (pos < line.value.length() && line.value.charAt(pos) != '<')
                {
                    pos++;
                }
                if (pos >= line.value.length())
                {
                    line = line.next;
                    pos = 0;
                }
                else
                {
                    temp.setLength(0);
                    final int newPos = Utils.readXML(temp, line.value, pos, false);
                    if (newPos > 0)
                    {
                        element = temp.toString();
                        temp.setLength(0);
                        Utils.getXMLTag(temp, element);
                        tag = temp.toString().toLowerCase();
                        if (HTML.isHtmlBlockElement(tag) && !tag.equals("hr"))
                        {
                            if (element.charAt(1) == '/')
                            {
                                if (!tags.getLast().equals(tag))
                                {
                                    return false;
                                }
                                tags.removeLast();
                            }
                            else
                            {
                                tags.addLast(tag);
                            }
                        }
                        if (tags.size() == 0)
                        {
                            this.xmlEndLine = line;
                            break;
                        }
                        pos = newPos;
                    }
                    else
                    {
                        pos++;
                    }
                }
            }
            return tags.size() == 0;
        }
        return false;
    }
}
