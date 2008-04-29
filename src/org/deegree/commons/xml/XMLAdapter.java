// $HeadURL$
/*----------------    FILE HEADER  ------------------------------------------

 This file is part of deegree.
 Copyright (C) 2001-2008 by:
 EXSE, Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53115 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de
 
 ---------------------------------------------------------------------------*/

package org.deegree.commons.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

/**
 * An instance of <code>XMLAdapter</code> encapsulates an underlying XML element which acts as the root element of the
 * document (which may be a fragment or a whole document).
 * <p>
 * Basically, <code>XMLAdapter</code> provides easy loading and proper saving (automatically generated CDATA-elements
 * for text nodes that need to be escaped) and acts as base class for all XML parsers in deegree.
 * 
 * TODO: automatically generated CDATA-elements are not implemented yet
 * 
 * <p>
 * Additionally, <code>XMLAdapter</code> tries to make the handling of relative paths inside the document's content as
 * painless as possible. This means that after initialization of the <code>XMLAdapter</code> with the correct SystemID
 * (i.e. the URL of the document):
 * <ul>
 * <li>external parsed entities (in the DOCTYPE part) can use relative URLs; e.g. &lt;!ENTITY local SYSTEM
 * "conf/wfs/wfs.cfg"&gt;</li>
 * <li>application specific documents which extend <code>XMLFragment</code> can resolve relative URLs during parsing
 * by calling the <code>resolve()</code> method</li>
 * </ul>
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider </a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class XMLAdapter {

    private static final Log LOG = LogFactory.getLog( XMLAdapter.class );

    protected static NamespaceContext nsContext = CommonNamespaces.getNamespaceContext();

    protected static final String XLN_NS = "http://www.w3.org/1999/xlink";

    protected static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    private QName SCHEMA_ATTRIBUTE_NAME = new QName( XSI_NS, "schemaLocation" );

    /**
     * Use this URL as SystemID only if an <code>XMLAdapter</code> cannot be pinpointed to a URL - in this case it may
     * not use any relative references!
     */
    public static final String DEFAULT_URL = "http://www.deegree.org";

    // encapsulated element
    protected OMElement rootElement;

    // the physical source of the element (used for resolving of URLs)
    private URL systemId;

    /**
     * Creates a new <code>XMLAdapter</code> which is not bound to an XML element.
     */
    public XMLAdapter() {
        // nothing to do
    }

    /**
     * Creates a new <code>XMLAdapter</code> which loads its content from the given <code>URL</code>.
     * 
     * @param url
     * 
     * @throws IOException
     * @throws FactoryConfigurationError
     * @throws XMLStreamException
     */
    public XMLAdapter( URL url ) throws IOException, XMLStreamException, FactoryConfigurationError {
        load( url );
    }

    /**
     * Creates a new <code>XMLAdapter</code> which is loaded from the given <code>File</code>.
     * 
     * @param file
     *            the file to load from
     * @throws IOException
     *             if the document could not be read from the file
     * @throws MalformedURLException
     *             if the file cannot be transposed to a valid url
     * @throws FactoryConfigurationError
     * @throws XMLStreamException
     */
    public XMLAdapter( File file ) throws MalformedURLException, IOException, XMLStreamException,
                            FactoryConfigurationError {
        if ( file != null ) {
            load( file.toURI().toURL() );
        }
    }

    /**
     * Creates a new <code>XMLAdapter</code> which is loaded from the given <code>Reader</code>.
     * 
     * @param reader
     * @param systemId
     *            this string should represent a URL that is related to the passed reader. If this URL is not available
     *            or unknown, the string should contain the value of XMLAdapter.DEFAULT_URL
     * 
     * @throws IOException
     * @throws FactoryConfigurationError
     * @throws XMLStreamException
     */
    public XMLAdapter( Reader reader, String systemId ) throws IOException, XMLStreamException,
                            FactoryConfigurationError {
        load( reader, systemId );
    }

    /**
     * Creates a new <code>XMLAdapter</code> instance based on the submitted document.
     * 
     * @param doc
     * @param systemId
     *            the URL that is the source of the passed doc. If this URL is not available or unknown, the string
     *            should contain the value of XMLFragment.DEFAULT_URL
     * @throws MalformedURLException
     *             if systemId is no valid and absolute <code>URL</code>
     */
    public XMLAdapter( OMDocument doc, String systemId ) throws MalformedURLException {
        this( doc.getOMDocumentElement(), systemId );
    }

    /**
     * Creates a new <code>XMLFragment</code> instance that encapsulates the given element.
     * 
     * @param element
     * @param systemId
     *            the URL that is the source of the passed doc. If this URL is not available or unknown, the string
     *            should contain the value of XMLFragment.DEFAULT_URL
     * @throws MalformedURLException
     */
    public XMLAdapter( OMElement element, String systemId ) throws MalformedURLException {
        setRootElement( element );
        setSystemId( systemId );
    }

    /**
     * Returns the systemId (the URL of the <code>XMLFragment</code>).
     * 
     * @return the systemId
     */
    public URL getSystemId() {
        return systemId;
    }

    /**
     * @param systemId
     *            The systemId (physical location) to set (may be null).
     * @throws MalformedURLException
     */
    public void setSystemId( String systemId )
                            throws MalformedURLException {
        if ( systemId != null ) {
            this.systemId = new URL( systemId );
        }
    }

    /**
     * @param systemId
     *            The systemId (physical location) to set.
     */
    public void setSystemId( URL systemId ) {
        this.systemId = systemId;
    }

    /**
     * Returns whether the document has a schema reference.
     * 
     * @return true, if the document has a schema reference, false otherwise
     */
    public boolean hasSchema() {
        return rootElement.getAttribute( SCHEMA_ATTRIBUTE_NAME ) != null;
    }

    /**
     * Determines the namespace <code>URI</code>s and the bound schema <code>URL</code>s from the
     * 'xsi:schemaLocation' attribute of the document element.
     * 
     * @return keys are URIs (namespaces), values are URLs (schema locations)
     * @throws XMLParsingException
     */
    public Map<URI, URL> getSchemas()
                            throws XMLParsingException {

        Map<URI, URL> schemaMap = new HashMap<URI, URL>();

        OMAttribute schemaLocationAttr = rootElement.getAttribute( SCHEMA_ATTRIBUTE_NAME );
        if ( schemaLocationAttr == null ) {
            return schemaMap;
        }

        String target = schemaLocationAttr.getAttributeValue();
        StringTokenizer tokenizer = new StringTokenizer( target );

        while ( tokenizer.hasMoreTokens() ) {
            URI nsURI = null;
            String token = tokenizer.nextToken();
            try {
                nsURI = new URI( token );
            } catch ( URISyntaxException e ) {
                String msg = "Invalid 'xsi:schemaLocation' attribute: namespace " + token + "' is not a valid URI.";
                LOG.error( msg );
                throw new XMLParsingException( msg );
            }

            URL schemaURL = null;
            try {
                token = tokenizer.nextToken();
                schemaURL = resolve( token );
            } catch ( NoSuchElementException e ) {
                String msg = "Invalid 'xsi:schemaLocation' attribute: namespace '" + nsURI
                             + "' is missing a schema URL.";
                LOG.error( msg );
                throw new XMLParsingException( msg );
            } catch ( MalformedURLException ex ) {
                String msg = "Invalid 'xsi:schemaLocation' attribute: '" + token + "' for namespace '" + nsURI
                             + "' could not be parsed as URL.";
                throw new XMLParsingException( msg );
            }
            schemaMap.put( nsURI, schemaURL );
        }
        return schemaMap;
    }

    /**
     * Initializes the <code>XMLAdapter</code> with the content from the given <code>URL</code>. Sets the SystemId,
     * too.
     * 
     * @param url
     * 
     * @throws IOException
     * @throws FactoryConfigurationError
     * @throws XMLStreamException
     */
    public void load( URL url )
                            throws IOException, XMLStreamException, FactoryConfigurationError {
        if ( url == null ) {
            throw new IllegalArgumentException( "The given url may not be null" );
        }
        String uri = url.toExternalForm();
        load( url.openStream(), uri );
    }

    /**
     * Initializes the <code>XMLAdapter</code> with the content from the given <code>InputStream</code>. Sets the
     * SystemId, too.
     * 
     * @param istream
     * @param systemId
     *            cannot be null. This string should represent a URL that is related to the passed istream. If this URL
     *            is not available or unknown, the string should contain the value of XMLFragment.DEFAULT_URL
     * 
     * @throws IOException
     * @throws FactoryConfigurationError
     * @throws XMLStreamException
     */
    public void load( InputStream istream, String systemId )
                            throws IOException, XMLStreamException, FactoryConfigurationError {

        PushbackInputStream pbis = new PushbackInputStream( istream, 1024 );
        String encoding = determineEncoding( pbis );

        InputStreamReader isr = new InputStreamReader( pbis, encoding );
        load( isr, systemId );
    }

    /**
     * Reads the encoding of the XML document from its header. If no header available
     * <code>CharsetUtils.getSystemCharset()</code> will be returned
     * 
     * @param pbis
     * @return encoding of a XML document
     * @throws IOException
     */
    private String determineEncoding( PushbackInputStream pbis )
                            throws IOException {

        byte[] b = new byte[80];
        int rd = pbis.read( b );
        String s = new String( b ).toLowerCase();

        // TODO think about this
        String encoding = "UTF-8";
        if ( s.indexOf( "?>" ) > -1 ) {
            int p = s.indexOf( "encoding=" );
            if ( p > -1 ) {
                StringBuffer sb = new StringBuffer();
                int k = p + 1 + "encoding=".length();
                while ( s.charAt( k ) != '"' && s.charAt( k ) != '\'' ) {
                    sb.append( s.charAt( k++ ) );
                }
                encoding = sb.toString();
            }
        }
        pbis.unread( b, 0, rd );
        return encoding;
    }

    /**
     * Initializes the <code>XMLAdapter</code> with the content from the given <code>Reader</code>. Sets the
     * SystemId, too.
     * 
     * @param reader
     * @param systemId
     *            can not be null. This string should represent a URL that is related to the passed reader. If this URL
     *            is not available or unknown, the string should contain the value of XMLFragment.DEFAULT_URL
     * 
     * @throws IOException
     * @throws FactoryConfigurationError
     * @throws XMLStreamException
     */
    public void load( Reader reader, String systemId )
                            throws IOException, XMLStreamException, FactoryConfigurationError {

        PushbackReader pbr = new PushbackReader( reader, 1024 );
        int c = pbr.read();
        if ( c != 65279 && c != 65534 ) {
            // no BOM (byte order mark)! push char back into reader
            pbr.unread( c );
        }

        if ( systemId == null ) {
            throw new NullPointerException( "'systemId' must not be null!" );
        }
        setSystemId( systemId );

        XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader( pbr );
        StAXOMBuilder builder = new StAXOMBuilder( parser );
        rootElement = builder.getDocumentElement();
    }

    /**
     * Sets the root element, i.e. the element encapsulated by this <code>XMLAdapter</code>.
     * 
     * @param rootElement
     */
    public void setRootElement( OMElement rootElement ) {
        this.rootElement = rootElement;
    }

    /**
     * Returns the root element, i.e. the element encapsulated by this <code>XMLAdapter</code>.
     * 
     * @return the root element
     */
    public OMElement getRootElement() {
        return rootElement;
    }

    /**
     * Resolves the given URL (which may be relative) against the SystemID of the <code>XMLFragment</code> into a
     * <code>URL</code> (which is always absolute).
     * 
     * @param url
     * @return the resolved URL object
     * @throws MalformedURLException
     */
    public URL resolve( String url )
                            throws MalformedURLException {

        LOG.debug( "Resolving URL '" + url + "' against SystemID '" + systemId + "'." );

        // check if url is an absolute path
        File file = new File( url );
        if ( file.isAbsolute() ) {
            return file.toURI().toURL();
        }

        URL resolvedURL = new URL( systemId, url );
        LOG.debug( "-> resolvedURL: '" + resolvedURL + "'" );
        return resolvedURL;
    }

    /**
     * Parses the submitted element as a <code>SimpleLink</code>.
     * <p>
     * Possible escaping of the attributes "xlink:href", "xlink:role" and "xlink:arcrole" is performed automatically.
     * </p>
     * 
     * @param element
     * @return the object representation of the element
     * @throws XMLParsingException
     */
    protected SimpleLink parseSimpleLink( Element element )
                            throws XMLParsingException {

        URI href = null;
        URI role = null;
        URI arcrole = null;
        String title = null;
        String show = null;
        String actuate = null;

        String uriString = null;
        try {
            uriString = XMLTools.getNodeAsString( element, "@xlink:href", nsContext, null );
            if ( uriString != null ) {
                href = new URI( null, uriString, null );
            }
            uriString = XMLTools.getNodeAsString( element, "@xlink:role", nsContext, null );
            if ( uriString != null ) {
                role = new URI( null, uriString, null );
            }
            uriString = XMLTools.getNodeAsString( element, "@xlink:arcrole", nsContext, null );
            if ( uriString != null ) {
                arcrole = new URI( null, uriString, null );
            }
        } catch ( URISyntaxException e ) {
            throw new XMLParsingException( "'" + uriString + "' is not a valid URI." );
        }

        return new SimpleLink( href, role, arcrole, title, show, actuate );
    }

    /**
     * Parses the given string as an instance of "xsd:boolean".
     * 
     * @param text
     * @return
     */
    protected boolean parseBoolean( String text ) {
        boolean value = true;
        if ( text != null ) {
            if ( "true".equals( text ) || "1".equals( text ) ) {
                value = true;
            } else if ( "false".equals( text ) || "0".equals( text ) ) {
                value = false;
            }
        }
        return value;
    }

    protected OMElement getRequiredChildElement( OMElement element, QName childName ) {
        OMElement childElement = element.getFirstChildWithName( childName );
        if ( childElement == null ) {
            String msg = "Element '" + element.getQName() + "' is missing required child element '" + childName + "'.";
            throw new XMLParsingException( msg );
        }
        return childElement;
    }
}
