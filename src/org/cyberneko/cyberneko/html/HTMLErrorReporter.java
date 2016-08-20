/* 
 * Copyright 2002-2009 Andy Clark, Marc Guillemot
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

package org.cyberneko.html;

import org.apache.xerces.xni.parser.XMLParseException;

/**
 * Defines an error reporter for reporting HTML errors. There is no such 
 * thing as a fatal error in parsing HTML. I/O errors are fatal but should 
 * throw an <code>IOException</code> directly instead of reporting an error.
 * <p>
 * When used in a configuration, the error reporter instance should be
 * set as a property with the following property identifier:
 * <pre>
 * "http://cyberneko.org/html/internal/error-reporter" in the
 * </pre>
 * Components in the configuration can query the error reporter using this
 * property identifier.
 * <p>
 * <strong>Note:</strong>
 * All reported errors are within the domain "http://cyberneko.org/html". 
 *
 * @author Andy Clark
 *
 * @version $Id: HTMLErrorReporter.java,v 1.4 2005/02/14 03:56:54 andyc Exp $
 */
public interface HTMLErrorReporter {
    
    //
    // HTMLErrorReporter methods
    //

    /** Format message without reporting error. */
    public String formatMessage(String key, Object[] args);

    /** Reports a warning. */
    public void reportWarning(String key, Object[] args) throws XMLParseException;

    /** Reports an error. */
    public void reportError(String key, Object[] args) throws XMLParseException;

} // interface HTMLErrorReporter
