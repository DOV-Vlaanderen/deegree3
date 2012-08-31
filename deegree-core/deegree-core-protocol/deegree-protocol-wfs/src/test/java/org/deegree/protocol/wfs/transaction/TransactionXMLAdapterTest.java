//$HeadURL$
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2008 by:
 Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
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

package org.deegree.protocol.wfs.transaction;

import static org.deegree.protocol.wfs.WFSConstants.VERSION_200;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import junit.framework.TestCase;

import org.deegree.commons.xml.stax.XMLStreamUtils;
import org.deegree.filter.Filter;
import org.deegree.protocol.wfs.WFSConstants;
import org.deegree.protocol.wfs.transaction.action.Delete;
import org.deegree.protocol.wfs.transaction.action.IDGenMode;
import org.deegree.protocol.wfs.transaction.action.Insert;
import org.deegree.protocol.wfs.transaction.action.PropertyReplacement;
import org.deegree.protocol.wfs.transaction.action.Update;
import org.deegree.protocol.wfs.transaction.xml.TransactionXMLAdapter;
import org.junit.Test;

/**
 * Test cases for the {@link TransactionXMLAdapter}.
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class TransactionXMLAdapterTest extends TestCase {

    private final String DELETE_110 = "examples_xml/v110/delete.xml";

    private final String INSERT_110 = "examples_xml/v110/insert.invalidxml";

    private final String UPDATE_110 = "examples_xml/v110/update.xml";

    private final String COMPLEX_110 = "examples_xml/v110/complex.invalidxml";

    private final String DELETE1_200 = "examples_xml/v200/delete1.xml";

    @Test
    public void testDelete110()
                            throws Exception {

        Transaction ta = parse( DELETE_110 );
        assertEquals( WFSConstants.VERSION_110, ta.getVersion() );
        assertEquals( "TA_1", ta.getHandle() );
        assertEquals( null, ta.getReleaseAction() );

        Iterator<TransactionAction> iter = ta.getActions().iterator();
        TransactionAction operation = iter.next();
        assertEquals( TransactionActionType.DELETE, operation.getType() );
        Delete delete = (Delete) operation;
        assertEquals( "delete1", delete.getHandle() );
        assertEquals( new QName( "http://www.deegree.org/app", "Philosopher" ), delete.getTypeName() );
        assertEquals( Filter.Type.OPERATOR_FILTER, delete.getFilter().getType() );

        assertFalse( iter.hasNext() );
    }

    @Test
    public void testInsert110()
                            throws Exception {

        Transaction ta = parse( INSERT_110 );
        assertEquals( WFSConstants.VERSION_110, ta.getVersion() );
        assertEquals( null, ta.getHandle() );
        assertEquals( null, ta.getReleaseAction() );

        Iterator<TransactionAction> iter = ta.getActions().iterator();
        TransactionAction operation = iter.next();
        assertEquals( TransactionActionType.INSERT, operation.getType() );
        Insert insert = (Insert) operation;
        assertEquals( "insert", insert.getHandle() );
        assertEquals( IDGenMode.GENERATE_NEW, insert.getIdGen() );
        assertEquals( null, insert.getInputFormat() );
        assertEquals( null, insert.getSrsName() );
        XMLStreamReader xmlStream = insert.getFeatures();
        XMLStreamUtils.skipElement( xmlStream );
        xmlStream.nextTag();
        assertFalse( iter.hasNext() );
    }

    @Test
    public void testUpdate110()
                            throws Exception {

        Transaction ta = parse( UPDATE_110 );
        assertEquals( WFSConstants.VERSION_110, ta.getVersion() );
        assertEquals( null, ta.getHandle() );
        assertEquals( null, ta.getReleaseAction() );

        Iterator<TransactionAction> iter = ta.getActions().iterator();
        TransactionAction operation = iter.next();
        assertEquals( TransactionActionType.UPDATE, operation.getType() );
        Update update = (Update) operation;
        assertEquals( "update1", update.getHandle() );
        assertEquals( new QName( "http://www.deegree.org/app", "Philosopher" ), update.getTypeName() );
        assertEquals( null, update.getInputFormat() );
        assertEquals( null, update.getSRSName() );

        Iterator<PropertyReplacement> iter2 = update.getReplacementProps();
        PropertyReplacement replacementProp1 = iter2.next();
        assertEquals( new QName( "http://www.deegree.org/app", "name" ), replacementProp1.getPropertyName() );
        XMLStreamReader prop1ValueStream = replacementProp1.getReplacementValue();
        assertEquals( "Albert Camus", prop1ValueStream.getElementText() );
        prop1ValueStream.nextTag();
        prop1ValueStream.nextTag();
        PropertyReplacement replacementProp2 = iter2.next();
        assertEquals( new QName( "http://www.deegree.org/app", "subject" ), replacementProp2.getPropertyName() );
        XMLStreamReader prop2ValueStream = replacementProp2.getReplacementValue();
        assertEquals( "existentialism", prop1ValueStream.getElementText() );
        prop2ValueStream.nextTag();
        prop2ValueStream.nextTag();

        Filter filter = update.getFilter();
        assertEquals( Filter.Type.OPERATOR_FILTER, filter.getType() );
        assertFalse( iter.hasNext() );
    }

    @Test
    public void testComplex110()
                            throws Exception {

        Transaction ta = parse( COMPLEX_110 );
        assertEquals( WFSConstants.VERSION_110, ta.getVersion() );
        assertEquals( "COMPLEX", ta.getHandle() );
        assertEquals( null, ta.getReleaseAction() );

        Iterator<TransactionAction> operationIter = ta.getActions().iterator();

        // first operation: delete1
        TransactionAction operation = operationIter.next();
        assertEquals( TransactionActionType.DELETE, operation.getType() );
        Delete delete = (Delete) operation;
        assertEquals( "delete1", delete.getHandle() );
        assertEquals( new QName( "http://www.deegree.org/app", "Philosopher" ), delete.getTypeName() );
        assertEquals( Filter.Type.OPERATOR_FILTER, delete.getFilter().getType() );
        assertTrue( operationIter.hasNext() );

        // second operation: insert
        Insert insert = (Insert) operationIter.next();
        assertEquals( "insert", insert.getHandle() );
        assertEquals( IDGenMode.GENERATE_NEW, insert.getIdGen() );
        assertEquals( null, insert.getInputFormat() );
        assertEquals( null, insert.getSrsName() );
        XMLStreamReader xmlStream = insert.getFeatures();
        // contract: read until feature/featureCollection END_ELEMENT
        XMLStreamUtils.skipElement( xmlStream );
        // contract: skip to wfs:Insert END_ELEMENT
        xmlStream.nextTag();
        // contract: skip to next operation
        xmlStream.nextTag();
        assertTrue( operationIter.hasNext() );

        // third operation: update1
        Update update = (Update) operationIter.next();
        assertEquals( "update1", update.getHandle() );
        assertEquals( new QName( "http://www.deegree.org/app", "Philosopher" ), update.getTypeName() );
        assertEquals( null, update.getInputFormat() );
        assertEquals( null, update.getSRSName() );

        Iterator<PropertyReplacement> iter2 = update.getReplacementProps();
        PropertyReplacement replacementProp1 = iter2.next();
        assertEquals( new QName( "http://www.deegree.org/app", "name" ), replacementProp1.getPropertyName() );
        XMLStreamReader prop1ValueStream = replacementProp1.getReplacementValue();
        assertEquals( "Albert Camus", prop1ValueStream.getElementText() );
        prop1ValueStream.nextTag();
        prop1ValueStream.nextTag();
        PropertyReplacement replacementProp2 = iter2.next();
        assertEquals( new QName( "http://www.deegree.org/app", "subject" ), replacementProp2.getPropertyName() );
        XMLStreamReader prop2ValueStream = replacementProp2.getReplacementValue();
        assertEquals( "existentialism", prop1ValueStream.getElementText() );
        // contract: skip to wfs:Property END_ELEMENT
        prop2ValueStream.nextTag();
        // contract: skip to next tag
        prop2ValueStream.nextTag();
        Filter filter = update.getFilter();
        assertEquals( Filter.Type.OPERATOR_FILTER, filter.getType() );
        assertTrue( operationIter.hasNext() );

        // fourth operation: delete2
        delete = (Delete) operationIter.next();
        assertEquals( "delete2", delete.getHandle() );
        assertEquals( new QName( "http://www.deegree.org/app", "Philosopher" ), delete.getTypeName() );
        assertEquals( Filter.Type.OPERATOR_FILTER, delete.getFilter().getType() );
        assertFalse( operationIter.hasNext() );
    }

    private Transaction parse( String resourceName )
                            throws XMLStreamException, FactoryConfigurationError, IOException {
        URL exampleURL = this.getClass().getResource( resourceName );
        XMLStreamReader xmlStream = XMLInputFactory.newInstance().createXMLStreamReader( exampleURL.toString(),
                                                                                         exampleURL.openStream() );
        xmlStream.nextTag();
        return TransactionXMLAdapter.parse( xmlStream );
    }

    @Test
    public void testDelete1_200()
                            throws Exception {

        Transaction ta = parse( DELETE1_200 );
        assertEquals( VERSION_200, ta.getVersion() );
        assertNull( ta.getHandle() );
        assertNull( ta.getReleaseAction() );

        Iterator<TransactionAction> iter = ta.getActions().iterator();
        TransactionAction operation = iter.next();
        assertEquals( TransactionActionType.DELETE, operation.getType() );
        Delete delete = (Delete) operation;
        assertNull( delete.getHandle() );
        assertEquals( new QName( "InWaterA_1M" ), delete.getTypeName() );
        assertNotNull( delete.getFilter() );

        assertFalse( iter.hasNext() );
    }
}
