/**
 * Copyright (c) 2011, ReXSL.com
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
package com.rexsl.core;

import com.google.inject.Singleton;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Converts XML to XHTML, if necessary.
 *
 * @author Yegor Bugayenko (yegor@rexsl.com)
 * @version $Id$
 */
@Singleton
final class XslBrowserFilter implements Filter {

    /**
     * Character encoding of the page.
     */
    private static final String ENCODING = "UTF-8";

    /**
     * Context for the filter.
     */
    private ServletContext context;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final FilterConfig config) {
        this.context = config.getServletContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res,
        final FilterChain chain) {
        if (!(req instanceof HttpServletRequest)
            || !(res instanceof HttpServletResponse)) {
            try {
                chain.doFilter(req, res);
                return;
            } catch (ServletException ex) {
                throw new IllegalStateException(ex);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        this.filter((HttpServletRequest) req, (HttpServletResponse) res, chain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        this.context = null;
    }

    /**
     * Make filtering.
     * @param request The request
     * @param response The response
     * @param chain Chain of filters
     * @see #doFilter(ServletRequest,ServletResponse,FilterChain)
     */
    private void filter(final HttpServletRequest request,
        final HttpServletResponse response, final FilterChain chain) {
        final ByteArrayResponseWrapper wrapper =
            new ByteArrayResponseWrapper(response);
        try {
            chain.doFilter(request, wrapper);
            if (response.isCommitted()) {
                // we can't change response that is already finished
                return;
            }
            final String agent = request.getHeader("User-Agent");
            final String accept = request.getHeader("Accept");
            String page = wrapper.getByteStream().toString(this.ENCODING);
            // let's check whether we should transform or not
            if (!page.isEmpty() && page.startsWith("<?xml")
                && !(this.isXsltCapable(agent) && this.isXmlAccepted(accept))) {
                response.setContentType("text/html");
                page = this.transform(page);
            }
            response.getOutputStream().write(page.getBytes(this.ENCODING));
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        } catch (ServletException ex) {
            throw new IllegalStateException(ex);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Check if the XSLT transformation is required on the server side.
     * @param agent User agent string from the request.
     * @return If the transformation is needed.
     */
    private boolean isXsltCapable(final String agent) {
        return (agent == null) || agent.matches(".*(Chrome|Safari).*");
    }

    /**
     * Check if the application/xml MIME type is present in given Accept header.
     * @param accept Accept header string from the request.
     * @return If the application/XML MIME type is present
     * @todo #3 This implemenation is very rough, and should be improved. We
     *  should property parse "Accept" header and detect whether "XML" type
     *  is accepted there or not.
     */
    private boolean isXmlAccepted(final String accept) {
        return (accept != null) && (accept.contains("application/xml"));
    }

    /**
     * Transform XML into HTML.
     * @param xml XML page to be transformed.
     * @return Resulting HTML page.
     */
    private String transform(final String xml) {
        final StringWriter writer = new StringWriter();
        try {
            final TransformerFactory factory = TransformerFactory.newInstance();
            factory.setURIResolver(
                new ContextResourceResolver(this.context)
            );
            final Source stylesheet = factory.getAssociatedStylesheet(
                new StreamSource(new StringReader(xml)),
                null, null, null
            );
            final Transformer trans = factory.newTransformer(stylesheet);
            trans.setURIResolver(new ContextResourceResolver(this.context));
            trans.transform(
                new StreamSource(new StringReader(xml)),
                new StreamResult(writer)
            );
        } catch (TransformerConfigurationException ex) {
            throw new IllegalStateException(ex);
        } catch (TransformerException ex) {
            throw new IllegalStateException(ex);
        }
        return writer.toString();
    }

}