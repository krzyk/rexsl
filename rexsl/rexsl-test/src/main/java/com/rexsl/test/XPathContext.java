/**
 * Copyright (c) 2011-2012, ReXSL.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the ReXSL.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rexsl.test;

import com.ymock.util.Logger;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * Convenient internal implementation of {@link NamespaceContext}.
 *
 * @author Yegor Bugayenko (yegor@rexsl.com)
 * @version $Id$
 * @since 0.3.7
 */
final class XPathContext implements NamespaceContext {

    /**
     * Map of prefixes and URIs.
     */
    private final transient ConcurrentMap<String, String> map =
        new ConcurrentHashMap<String, String>();

    /**
     * Public ctor.
     */
    public XPathContext() {
        this.map.put("xhtml", "http://www.w3.org/1999/xhtml");
        this.map.put("xs", "http://www.w3.org/2001/XMLSchema");
        this.map.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        this.map.put("xsl", "http://www.w3.org/1999/XSL/Transform");
    }

    /**
     * Public ctor with custom namespaces.
     * @param namespaces List of namespaces
     */
    public XPathContext(final Object... namespaces) {
        this();
        for (int pos = 0; pos < namespaces.length; ++pos) {
            this.map.put(
                Logger.format("ns%d", pos + 1),
                namespaces[pos].toString()
            );
        }
    }

    /**
     * Public ctor.
     * @param old Old map of prefixes and namespaces
     * @param prefix The prefix
     * @param namespace The namespace
     */
    private XPathContext(final ConcurrentMap<String, String> old,
        final String prefix, final Object namespace) {
        this();
        this.map.putAll(old);
        this.map.put(prefix, namespace.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNamespaceURI(final String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("prefix can't be NULL");
        }
        String namespace = this.map.get(prefix);
        if (namespace == null) {
            if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
                namespace = XMLConstants.XML_NS_URI;
            } else if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                namespace = XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
            } else {
                namespace = XMLConstants.NULL_NS_URI;
            }
        }
        return namespace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPrefix(final String namespace) {
        final Iterator<String> prefixes = this.getPrefixes(namespace);
        String prefix = null;
        if (prefixes.hasNext()) {
            prefix = prefixes.next();
        }
        return prefix;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<String> getPrefixes(final String namespace) {
        if (namespace == null) {
            throw new IllegalArgumentException("namespace can't be NULL");
        }
        final List<String> prefixes = new LinkedList<String>();
        for (ConcurrentMap.Entry<String, String> entry : this.map.entrySet()) {
            if (entry.getValue().equals(namespace)) {
                prefixes.add(entry.getKey());
            }
        }
        if (namespace.equals(XMLConstants.XML_NS_URI)) {
            prefixes.add(XMLConstants.XML_NS_PREFIX);
        }
        if (namespace.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            prefixes.add(XMLConstants.XMLNS_ATTRIBUTE);
        }
        return Collections.unmodifiableList(prefixes).iterator();
    }

    /**
     * Add new prefix and namespace.
     * @param prefix The prefix
     * @param namespace The namespace
     * @return New context
     */
    public XPathContext add(final String prefix, final Object namespace) {
        return new XPathContext(this.map, prefix, namespace);
    }

}