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

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;

/**
 * <font color="red">EXPERIMENTAL: may change in next release</font><br/>
 * {@link XMLDocumentHandler} implementing this interface will get notified of elements discarded
 * by the tag balancer when they:
 * <ul>
 * <li>are configured using {@link HTMLConfiguration}</li>
 * <li>activate the tag balancing feature</li>
 * </ul>
 * @author Marc Guillemot
 * @version $Id: HTMLTagBalancingListener.java 260 2009-09-02 08:26:01Z mguillem $
 */
public interface HTMLTagBalancingListener 
{
	/**
	 * Notifies that the start element has been ignored. 
	 */
	void ignoredStartElement(QName elem, XMLAttributes attrs, Augmentations augs);

	/**
	 * Notifies that the end element has been ignored. 
	 */
	void ignoredEndElement(QName element, Augmentations augs);

}
