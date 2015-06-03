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
 * Block type enum.
 *
 * @author René Jeschke <rene_jeschke@yahoo.de>
 */
enum BlockType
{
    /** Unspecified. Used for root block and list items without paragraphs. */
    NONE,
    /** A block quote. */
    BLOCKQUOTE,
    /** A code block. */
    CODE,
    /** A fenced code block. */
    FENCED_CODE,
    /** A headline. */
    HEADLINE,
    /** A list item. */
    LIST_ITEM,
    /** An ordered list. */
    ORDERED_LIST,
    /** A paragraph. */
    PARAGRAPH,
    /** A horizontal ruler. */
    RULER,
    /** An unordered list. */
    UNORDERED_LIST,
    /** A XML block. */
    XML
}
