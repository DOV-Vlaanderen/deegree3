//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2010 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.feature.persistence.sql.mapper;

import static javax.xml.XMLConstants.NULL_NS_URI;
import static org.apache.xerces.xs.XSComplexTypeDefinition.CONTENTTYPE_ELEMENT;
import static org.apache.xerces.xs.XSComplexTypeDefinition.CONTENTTYPE_EMPTY;
import static org.deegree.commons.tom.primitive.PrimitiveType.STRING;
import static org.deegree.feature.persistence.sql.blob.BlobCodec.Compression.NONE;
import static org.deegree.feature.types.property.GeometryPropertyType.CoordinateDimension.DIM_2;
import static org.deegree.gml.GMLVersion.GML_32;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;
import org.deegree.commons.jdbc.QTableName;
import org.deegree.commons.tom.primitive.PrimitiveType;
import org.deegree.commons.tom.primitive.XMLValueMangler;
import org.deegree.commons.xml.CommonNamespaces;
import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.feature.persistence.sql.BBoxTableMapping;
import org.deegree.feature.persistence.sql.DataTypeMapping;
import org.deegree.feature.persistence.sql.FeatureTypeMapping;
import org.deegree.feature.persistence.sql.MappedApplicationSchema;
import org.deegree.feature.persistence.sql.blob.BlobCodec;
import org.deegree.feature.persistence.sql.blob.BlobMapping;
import org.deegree.feature.persistence.sql.expressions.JoinChain;
import org.deegree.feature.persistence.sql.id.FIDMapping;
import org.deegree.feature.persistence.sql.id.IDGenerator;
import org.deegree.feature.persistence.sql.id.UUIDGenerator;
import org.deegree.feature.persistence.sql.rules.CodeMapping;
import org.deegree.feature.persistence.sql.rules.CompoundMapping;
import org.deegree.feature.persistence.sql.rules.FeatureMapping;
import org.deegree.feature.persistence.sql.rules.GenericObjectMapping;
import org.deegree.feature.persistence.sql.rules.GeometryMapping;
import org.deegree.feature.persistence.sql.rules.Mapping;
import org.deegree.feature.persistence.sql.rules.PrimitiveMapping;
import org.deegree.feature.types.ApplicationSchema;
import org.deegree.feature.types.FeatureType;
import org.deegree.feature.types.property.CodePropertyType;
import org.deegree.feature.types.property.CustomPropertyType;
import org.deegree.feature.types.property.FeaturePropertyType;
import org.deegree.feature.types.property.GenericObjectPropertyType;
import org.deegree.feature.types.property.GeometryPropertyType;
import org.deegree.feature.types.property.GeometryPropertyType.CoordinateDimension;
import org.deegree.feature.types.property.GeometryPropertyType.GeometryType;
import org.deegree.feature.types.property.ObjectPropertyType;
import org.deegree.feature.types.property.PropertyType;
import org.deegree.feature.types.property.SimplePropertyType;
import org.deegree.filter.expression.PropertyName;
import org.deegree.filter.sql.DBField;
import org.deegree.filter.sql.MappingExpression;
import org.deegree.gml.GMLVersion;
import org.deegree.gml.schema.GMLSchemaInfoSet;
import org.jaxen.NamespaceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link MappedApplicationSchema} instances from {@link ApplicationSchema}s by inferring a canonical database
 * mapping.
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class AppSchemaMapper {

    private static Logger LOG = LoggerFactory.getLogger( AppSchemaMapper.class );

    private final ApplicationSchema appSchema;

    private final MappingContextManager mcManager;

    private final ICRS storageCrs;

    private final String storageSrid;

    private List<DataTypeMapping> dtMappings = new ArrayList<DataTypeMapping>();

    // TODO
    private final CoordinateDimension storageDim = DIM_2;

    private final MappedApplicationSchema mappedSchema;

    /**
     * Creates a new {@link AppSchemaMapper} instance for the given schema.
     * 
     * @param appSchema
     *            application schema to be mapped, must not be <code>null</code>
     * @param createBlobMapping
     *            true, if BLOB mapping should be performed, false otherwise
     * @param createRelationalMapping
     *            true, if relational mapping should be performed, false otherwise
     * @param storageCrs
     *            CRS to use for geometry properties, must not be <code>null</code>
     * @param srid
     *            native DB-SRS identifier, must not be <code>null</code>
     */
    public AppSchemaMapper( ApplicationSchema appSchema, boolean createBlobMapping, boolean createRelationalMapping,
                            ICRS storageCrs, String srid ) {

        this.appSchema = appSchema;
        this.storageCrs = storageCrs;
        this.storageSrid = srid;

        List<FeatureType> ftList = appSchema.getFeatureTypes( null, false, false );
        FeatureType[] fts = appSchema.getFeatureTypes( null, false, false ).toArray( new FeatureType[ftList.size()] );
        Map<FeatureType, FeatureType> ftToSuperFt = appSchema.getFtToSuperFt();
        Map<String, String> prefixToNs = appSchema.getNamespaceBindings();
        GMLSchemaInfoSet xsModel = appSchema.getXSModel();
        FeatureTypeMapping[] ftMappings = null;

        Map<String, String> nsToPrefix = new HashMap<String, String>();
        Iterator<String> nsIter = CommonNamespaces.getNamespaceContext().getNamespaceURIs();
        while ( nsIter.hasNext() ) {
            String ns = nsIter.next();
            nsToPrefix.put( ns, CommonNamespaces.getNamespaceContext().getPrefix( ns ) );
        }
        nsToPrefix.putAll( xsModel.getNamespacePrefixes() );

        mcManager = new MappingContextManager( nsToPrefix );
        if ( createRelationalMapping ) {
            ftMappings = generateFtMappings( fts );
        }

        BBoxTableMapping bboxMapping = createBlobMapping ? generateBBoxMapping() : null;
        BlobMapping blobMapping = createBlobMapping ? generateBlobMapping() : null;

        DataTypeMapping[] dtMappings = this.dtMappings.toArray( new DataTypeMapping[this.dtMappings.size()] );

        this.mappedSchema = new MappedApplicationSchema( fts, ftToSuperFt, prefixToNs, xsModel, ftMappings, dtMappings,
                                                         bboxMapping, blobMapping );
    }

    /**
     * Returns the {@link MappedApplicationSchema} instance.
     * 
     * @return mapped schema, never <code>null</code>
     */
    public MappedApplicationSchema getMappedSchema() {
        return mappedSchema;
    }

    private BlobMapping generateBlobMapping() {
        // TODO
        String table = "GML_OBJECTS";
        // TODO
        BlobCodec codec = new BlobCodec( GMLVersion.GML_32, NONE );
        return new BlobMapping( table, storageCrs, codec );
    }

    private BBoxTableMapping generateBBoxMapping() {
        // TODO
        String ftTable = "FEATURE_TYPES";
        return new BBoxTableMapping( ftTable, storageCrs );
    }

    private FeatureTypeMapping[] generateFtMappings( FeatureType[] fts ) {
        FeatureTypeMapping[] ftMappings = new FeatureTypeMapping[fts.length];
        for ( int i = 0; i < fts.length; i++ ) {
            ftMappings[i] = generateFtMapping( fts[i] );
        }
        return ftMappings;
    }

    private FeatureTypeMapping generateFtMapping( FeatureType ft ) {
        LOG.info( "Mapping feature type '" + ft.getName() + "'" );
        MappingContext mc = mcManager.newContext( ft.getName(), "attr_gml_id" );

        // TODO
        QTableName table = new QTableName( mc.getTable() );
        // TODO
        IDGenerator generator = new UUIDGenerator();
        // TODO
        FIDMapping fidMapping = new FIDMapping( "", "attr_gml_id", STRING, generator );

        List<Mapping> mappings = new ArrayList<Mapping>();
        // TODO: gml properties
        for ( PropertyType pt : ft.getPropertyDeclarations( GML_32 ) ) {
            mappings.add( generatePropMapping( pt, mc ) );
        }
        return new FeatureTypeMapping( ft.getName(), table, fidMapping, mappings );
    }

    private Mapping generatePropMapping( PropertyType pt, MappingContext mc ) {
        LOG.debug( "Mapping property '" + pt.getName() + "'" );
        Mapping mapping = null;
        if ( pt instanceof SimplePropertyType ) {
            mapping = generatePropMapping( (SimplePropertyType) pt, mc );
        } else if ( pt instanceof GeometryPropertyType ) {
            mapping = generatePropMapping( (GeometryPropertyType) pt, mc );
        } else if ( pt instanceof FeaturePropertyType ) {
            mapping = generatePropMapping( (FeaturePropertyType) pt, mc );
        } else if ( pt instanceof CustomPropertyType ) {
            mapping = generatePropMapping( (CustomPropertyType) pt, mc );
        } else if ( pt instanceof CodePropertyType ) {
            mapping = generatePropMapping( (CodePropertyType) pt, mc );
        } else if ( pt instanceof GenericObjectPropertyType ) {
            mapping = generatePropMapping( (GenericObjectPropertyType) pt, mc );
        } else {
            LOG.warn( "Unhandled property type '" + pt.getName() + "': " + pt.getClass().getName() );
        }
        return mapping;
    }

    private PrimitiveMapping generatePropMapping( SimplePropertyType pt, MappingContext mc ) {
        LOG.debug( "Mapping simple property '" + pt.getName() + "'" );
        PropertyName path = getPropName( pt.getName() );
        MappingContext propMc = null;
        JoinChain jc = null;
        if ( pt.getMaxOccurs() == 1 ) {
            propMc = mcManager.mapOneToOneElement( mc, pt.getName() );
        } else {
            propMc = mcManager.mapOneToManyElements( mc, pt.getName() );
            LOG.warn( "TODO: Build JoinChain" );
        }
        MappingExpression mapping = new DBField( propMc.getColumn() );

        DBField nilMapping = null;
        if ( pt.isNillable() ) {
            nilMapping = getNilMapping( propMc );
        }
        return new PrimitiveMapping( path, mapping, pt.getPrimitiveType(), jc, nilMapping );
    }

    private DBField getNilMapping( MappingContext ctx ) {
        QName nilAttrName = new QName( CommonNamespaces.XSINS, "nil", "xsi" );
        return new DBField( mcManager.mapOneToOneAttribute( ctx, nilAttrName ).getColumn() );
    }

    private GeometryMapping generatePropMapping( GeometryPropertyType pt, MappingContext mc ) {
        LOG.debug( "Mapping geometry property '" + pt.getName() + "'" );
        PropertyName path = getPropName( pt.getName() );
        MappingContext propMc = null;
        JoinChain jc = null;
        if ( pt.getMaxOccurs() == 1 ) {
            propMc = mcManager.mapOneToOneElement( mc, pt.getName() );
        } else {
            propMc = mcManager.mapOneToManyElements( mc, pt.getName() );
            LOG.warn( "TODO: Build JoinChain" );
        }
        MappingExpression mapping = new DBField( propMc.getColumn() );
        DBField nilMapping = null;
        if ( pt.isNillable() ) {
            nilMapping = getNilMapping( propMc );
        }
        return new GeometryMapping( path, mapping, pt.getGeometryType(), storageDim, storageCrs, storageSrid, jc,
                                    nilMapping );
    }

    private FeatureMapping generatePropMapping( FeaturePropertyType pt, MappingContext mc ) {
        LOG.debug( "Mapping feature property '" + pt.getName() + "'" );
        PropertyName path = getPropName( pt.getName() );
        JoinChain jc = null;
        MappingContext mc2 = null;
        MappingExpression mapping = null;
        if ( pt.getMaxOccurs() == 1 ) {
            mc2 = mcManager.mapOneToOneElement( mc, pt.getName() );
            mapping = new DBField( mc2.getColumn() );
        } else {
            mc2 = mcManager.mapOneToManyElements( mc, pt.getName() );
            jc = generateJoinChain( mc, mc2 );
            mapping = new DBField( "ref" );
        }
        DBField nilMapping = null;
        if ( pt.isNillable() ) {
            nilMapping = getNilMapping( mc2 );
        }
        return new FeatureMapping( path, mapping, pt.getFTName(), jc, nilMapping );
    }

    private GenericObjectMapping generatePropMapping( GenericObjectPropertyType pt, MappingContext mc ) {
        LOG.warn( "Mapping generic object property '" + pt.getName() + "'" );
        PropertyName path = getPropName( pt.getName() );
        JoinChain jc = null;
        MappingContext mc2 = null;
        MappingExpression mapping = null;
        if ( pt.getMaxOccurs() == 1 ) {
            mc2 = mcManager.mapOneToOneElement( mc, pt.getName() );
            mapping = new DBField( mc2.getColumn() );
        } else {
            mc2 = mcManager.mapOneToManyElements( mc, pt.getName() );
            jc = generateJoinChain( mc, mc2 );
            mapping = new DBField( "ref" );
        }
        QTableName table = new QTableName( mc2.getColumn() );
        XSElementDeclaration xsElementDecl = pt.getValueElementDecl();
        XSComplexTypeDefinition xsTypeDef = (XSComplexTypeDefinition) xsElementDecl.getTypeDefinition();
        QName name = new QName( xsElementDecl.getNamespace(), xsElementDecl.getName() );
        MappingContext newMc = mcManager.newContext( name, "id" );
        List<Mapping> particles = generateMapping( xsTypeDef, newMc, new HashMap<QName, QName>() );
        DataTypeMapping dtMapping = new DataTypeMapping( xsElementDecl, table, particles );
        dtMappings.add( dtMapping );

        DBField nilMapping = null;
        if ( pt.isNillable() ) {
            nilMapping = getNilMapping( mc2 );
        }
        return new GenericObjectMapping( path, mapping, xsElementDecl, jc, nilMapping );
    }

    private CompoundMapping generatePropMapping( CustomPropertyType pt, MappingContext mc ) {

        LOG.debug( "Mapping custom property '" + pt.getName() + "'" );

        XSComplexTypeDefinition xsTypeDef = pt.getXSDValueType();
        if ( xsTypeDef == null ) {
            LOG.warn( "No XSD type definition available for custom property '" + pt.getName() + "'. Skipping it." );
            return null;
        }

        PropertyName path = getPropName( pt.getName() );

        MappingContext propMc = null;
        JoinChain jc = null;
        if ( pt.getMaxOccurs() == 1 ) {
            propMc = mcManager.mapOneToOneElement( mc, pt.getName() );
        } else {
            propMc = mcManager.mapOneToManyElements( mc, pt.getName() );
            jc = generateJoinChain( mc, propMc );
        }
        List<Mapping> particles = generateMapping( pt.getXSDValueType(), propMc, new HashMap<QName, QName>() );
        DBField nilMapping = null;
        if ( pt.isNillable() ) {
            nilMapping = getNilMapping( propMc );
        }
        return new CompoundMapping( path, particles, jc, nilMapping );
    }

    private CodeMapping generatePropMapping( CodePropertyType pt, MappingContext mc ) {
        LOG.debug( "Mapping code property '" + pt.getName() + "'" );
        PropertyName path = getPropName( pt.getName() );
        MappingContext propMc = null;
        MappingContext codeSpaceMc = null;
        JoinChain jc = null;
        MappingExpression mapping = null;
        if ( pt.getMaxOccurs() == 1 ) {
            propMc = mcManager.mapOneToOneElement( mc, pt.getName() );
            codeSpaceMc = mcManager.mapOneToOneAttribute( propMc, new QName( "codeSpace" ) );
            mapping = new DBField( propMc.getColumn() );
        } else {
            propMc = mcManager.mapOneToManyElements( mc, pt.getName() );
            codeSpaceMc = mcManager.mapOneToOneAttribute( propMc, new QName( "codeSpace" ) );
            jc = generateJoinChain( mc, propMc );
            mapping = new DBField( "value" );
        }
        MappingExpression csMapping = new DBField( codeSpaceMc.getColumn() );
        DBField nilMapping = null;
        if ( pt.isNillable() ) {
            nilMapping = getNilMapping( propMc );
        }
        return new CodeMapping( path, mapping, STRING, jc, csMapping, nilMapping );
    }

    private JoinChain generateJoinChain( MappingContext from, MappingContext to ) {
        return new JoinChain( new DBField( from.getTable(), from.getIdColumn() ), new DBField( to.getTable(),
                                                                                               "parentfk" ) );
    }

    private List<Mapping> generateMapping( XSComplexTypeDefinition typeDef, MappingContext mc,
                                           Map<QName, QName> elements ) {

        List<Mapping> particles = new ArrayList<Mapping>();

        // text node
        if ( typeDef.getContentType() != CONTENTTYPE_EMPTY && typeDef.getContentType() != CONTENTTYPE_ELEMENT ) {
            // TODO
            NamespaceContext nsContext = null;
            PropertyName path = new PropertyName( "text()", nsContext );
            String column = mc.getColumn();
            if ( column == null || column.isEmpty() ) {
                column = "value";
            }
            DBField dbField = new DBField( mc.getTable(), column );
            PrimitiveType pt = PrimitiveType.STRING;
            if ( typeDef.getSimpleType() != null ) {
                pt = XMLValueMangler.getPrimitiveType( typeDef.getSimpleType() );
            }
            particles.add( new PrimitiveMapping( path, dbField, pt, null, null ) );
        }

        // attributes
        XSObjectList attributeUses = typeDef.getAttributeUses();
        for ( int i = 0; i < attributeUses.getLength(); i++ ) {
            XSAttributeDeclaration attrDecl = ( (XSAttributeUse) attributeUses.item( i ) ).getAttrDeclaration();
            QName attrName = new QName( attrDecl.getName() );
            if ( attrDecl.getNamespace() != null ) {
                attrName = new QName( attrDecl.getNamespace(), attrDecl.getName() );
            }
            MappingContext attrMc = mcManager.mapOneToOneAttribute( mc, attrName );
            // TODO
            NamespaceContext nsContext = null;
            PropertyName path = new PropertyName( "@" + getName( attrName ), nsContext );
            DBField dbField = new DBField( attrMc.getTable(), attrMc.getColumn() );
            PrimitiveType pt = XMLValueMangler.getPrimitiveType( attrDecl.getTypeDefinition() );
            particles.add( new PrimitiveMapping( path, dbField, pt, null, null ) );
        }

        // child elements
        XSParticle particle = typeDef.getParticle();
        if ( particle != null ) {
            List<Mapping> childElMappings = generateMapping( particle, 1, mc, elements );
            particles.addAll( childElMappings );
        }
        return particles;
    }

    private List<Mapping> generateMapping( XSParticle particle, int maxOccurs, MappingContext mc,
                                           Map<QName, QName> elements ) {

        List<Mapping> childElMappings = new ArrayList<Mapping>();

        // // check if the particle term defines a GMLObjectPropertyType
        // if ( particle.getTerm() instanceof XSElementDeclaration ) {
        // XSElementDeclaration elDecl = (XSElementDeclaration) particle.getTerm();
        // QName elName = new QName( elDecl.getNamespace(), elDecl.getName() );
        // int minOccurs = particle.getMinOccurs();
        // maxOccurs = particle.getMaxOccursUnbounded() ? -1 : particle.getMaxOccurs();
        // // TODO
        // List<PropertyType> ptSubstitutions = null;
        // ObjectPropertyType pt = appSchema.getXSModel().getGMLPropertyDecl( elDecl, elName, minOccurs, maxOccurs,
        // ptSubstitutions );
        // if ( pt != null ) {
        // if ( pt instanceof GeometryPropertyType ) {
        // childElMappings.add( generatePropMapping( (GeometryPropertyType) pt, mc ) );
        // } else if ( pt instanceof FeaturePropertyType ) {
        // childElMappings.add( generatePropMapping( (FeaturePropertyType) pt, mc ) );
        // } else {
        // LOG.warn( "TODO: Generic object property type " + pt );
        // }
        // }
        // }
        if ( childElMappings.isEmpty() ) {
            if ( particle.getMaxOccursUnbounded() ) {
                childElMappings.addAll( generateMapping( particle.getTerm(), -1, mc, elements ) );
            } else {
                for ( int i = 1; i <= particle.getMaxOccurs(); i++ ) {
                    childElMappings.addAll( generateMapping( particle.getTerm(), i, mc, elements ) );
                }
            }
        }
        return childElMappings;
    }

    private List<Mapping> generateMapping( XSTerm term, int occurence, MappingContext mc, Map<QName, QName> elements ) {
        List<Mapping> mappings = new ArrayList<Mapping>();
        if ( term instanceof XSElementDeclaration ) {
            mappings.addAll( generateMapping( (XSElementDeclaration) term, occurence, mc, elements ) );
        } else if ( term instanceof XSModelGroup ) {
            mappings.addAll( generateMapping( (XSModelGroup) term, occurence, mc, elements ) );
        } else {
            mappings.addAll( generateMapping( (XSWildcard) term, occurence, mc, elements ) );
        }
        return mappings;
    }

    private List<Mapping> generateMapping( XSElementDeclaration elDecl, int occurence, MappingContext mc,
                                           Map<QName, QName> elements ) {

        List<Mapping> mappings = new ArrayList<Mapping>();

        QName eName = new QName( elDecl.getNamespace(), elDecl.getName() );
        if ( eName.equals( new QName( "http://www.opengis.net/gml/3.2", "AbstractCRS" ) ) ) {
            LOG.warn( "Skipping mapping of AbstractCRS element" );
            return mappings;
        }
        if ( eName.equals( new QName( "http://www.opengis.net/gml/3.2", "TimeOrdinalEra" ) ) ) {
            LOG.warn( "Skipping mapping of TimeOrdinalEra element" );
            return mappings;
        }

        if ( eName.equals( new QName( "http://www.opengis.net/gml/3.2", "TimePeriod" ) ) ) {
            LOG.warn( "Skipping mapping of TimePeriod element" );
            return mappings;
        }

        if ( eName.equals( new QName( "http://www.isotc211.org/2005/gmd", "EX_GeographicDescription" ) ) ) {
            LOG.warn( "Skipping mapping of EX_GeographicDescription element" );
        }

        // consider every concrete element substitution
        List<XSElementDeclaration> substitutions = appSchema.getXSModel().getSubstitutions( elDecl, null, true, true );
        if ( eName.equals( new QName( "http://www.isotc211.org/2005/gco", "CharacterString" ) ) ) {
            substitutions.clear();
            substitutions.add( elDecl );
        }

        if ( eName.equals( new QName( "http://www.isotc211.org/2005/gmd", "MD_Identifier" ) ) ) {
            substitutions.clear();
            substitutions.add( elDecl );
        }

        for ( XSElementDeclaration substitution : substitutions ) {
            ObjectPropertyType opt = appSchema.getCustomElDecl( substitution );
            if ( opt instanceof GenericObjectPropertyType ) {
                LOG.warn( "Found generic object property type: " + opt
                          + ". Not implemented, treating as CustomPropertyType." );
                opt = null;
            }
            if ( opt != null ) {
                mappings.add( generatePropMapping( opt, mc ) );
            } else {
                Map<QName, QName> elements2 = new LinkedHashMap<QName, QName>( elements );

                QName elName = new QName( substitution.getName() );
                if ( substitution.getNamespace() != null ) {
                    elName = new QName( substitution.getNamespace(), substitution.getName() );
                }

                MappingContext elMC = null;
                if ( occurence == 1 ) {
                    elMC = mcManager.mapOneToOneElement( mc, elName );
                } else {
                    elMC = mcManager.mapOneToManyElements( mc, elName );
                }

                NamespaceContext nsContext = null;
                PropertyName path = new PropertyName( getName( elName ), nsContext );

                if ( appSchema.getFeatureType( elName ) != null ) {
                    QName valueFtName = elName;
                    JoinChain jc = null;
                    DBField mapping = null;
                    if ( occurence == -1 ) {
                        // TODO
                    } else {
                        mapping = new DBField( elMC.getColumn() );
                    }
                    DBField nilMapping = null;
                    if ( substitution.getNillable() ) {
                        nilMapping = getNilMapping( elMC );
                    }
                    mappings.add( new FeatureMapping( path, mapping, valueFtName, jc, nilMapping ) );
                } else if ( appSchema.getXSModel().getGeometryElement( elName ) != null ) {
                    JoinChain jc = null;
                    DBField mapping = null;
                    // TODO
                    GeometryType gt = GeometryType.GEOMETRY;
                    // TODO
                    CoordinateDimension dim = CoordinateDimension.DIM_2;
                    // TODO
                    String srid = "-1";
                    if ( occurence == -1 ) {
                        // TODO
                        // writeJoinedTable( writer, elMC.getTable() );
                    } else {
                        mapping = new DBField( elMC.getColumn() );
                    }
                    DBField nilMapping = null;
                    if ( substitution.getNillable() ) {
                        nilMapping = getNilMapping( elMC );
                    }
                    mappings.add( new GeometryMapping( path, mapping, gt, dim, storageCrs, srid, jc, nilMapping ) );
                } else {
                    XSTypeDefinition typeDef = substitution.getTypeDefinition();
                    QName complexTypeName = getQName( typeDef );
                    // TODO multiple elements with same name?
                    QName complexTypeName2 = elements2.get( elName );
                    if ( complexTypeName2 != null && complexTypeName2.equals( complexTypeName ) ) {
                        // during this mapping traversal, there already has been an element with this name and type
                        StringBuffer sb = new StringBuffer( "Path: " );
                        for ( QName qName : elements2.keySet() ) {
                            sb.append( qName );
                            sb.append( " -> " );
                        }
                        sb.append( elName );
                        LOG.info( "Skipping complex element '" + elName + "' -- detected recursion: " + sb );
                        continue;
                    }
                    elements2.put( elName, getQName( typeDef ) );

                    JoinChain jc = null;
                    if ( occurence == -1 ) {
                        jc = generateJoinChain( mc, elMC );
                    }

                    DBField nilMapping = null;
                    if ( substitution.getNillable() ) {
                        nilMapping = getNilMapping( elMC );
                    }

                    if ( typeDef instanceof XSComplexTypeDefinition ) {
                        List<Mapping> particles = generateMapping( (XSComplexTypeDefinition) typeDef, elMC, elements2 );
                        mappings.add( new CompoundMapping( path, particles, jc, nilMapping ) );
                    } else {
                        MappingExpression mapping = new DBField( elMC.getColumn() );
                        PrimitiveType pt = XMLValueMangler.getPrimitiveType( (XSSimpleTypeDefinition) typeDef );
                        mappings.add( new PrimitiveMapping( path, mapping, pt, jc, nilMapping ) );
                    }
                }
            }
        }
        return mappings;
    }

    private List<Mapping> generateMapping( XSModelGroup modelGroup, int occurrence, MappingContext mc,
                                           Map<QName, QName> elements ) {
        List<Mapping> mappings = new ArrayList<Mapping>();
        XSObjectList particles = modelGroup.getParticles();
        for ( int i = 0; i < particles.getLength(); i++ ) {
            XSParticle particle = (XSParticle) particles.item( i );
            mappings.addAll( generateMapping( particle, occurrence, mc, elements ) );
        }
        return mappings;
    }

    private List<Mapping> generateMapping( XSWildcard wildCard, int occurrence, MappingContext mc,
                                           Map<QName, QName> elements ) {
        LOG.debug( "Handling of wild cards not implemented yet." );
        StringBuffer sb = new StringBuffer( "Path: " );
        for ( QName qName : elements.keySet() ) {
            sb.append( qName );
            sb.append( " -> " );
        }
        sb.append( "wildcard" );
        LOG.debug( "Skipping wildcard at path: " + sb );
        return new ArrayList<Mapping>();
    }

    //
    // private String getPrimitiveTypeName( XSSimpleTypeDefinition typeDef ) {
    // if ( typeDef == null ) {
    // return "string";
    // }
    // return XMLValueMangler.getPrimitiveType( typeDef ).getXSTypeName();
    // }
    //

    private QName getQName( XSTypeDefinition xsType ) {
        QName name = null;
        if ( !xsType.getAnonymous() ) {
            name = new QName( xsType.getNamespace(), xsType.getName() );
        }
        return name;
    }

    private String getName( QName name ) {
        if ( name.getNamespaceURI() != null && !name.getNamespaceURI().equals( NULL_NS_URI ) ) {
            String prefix = appSchema.getXSModel().getNamespacePrefixes().get( name.getNamespaceURI() );
            return prefix + ":" + name.getLocalPart();
        }
        return name.getLocalPart();
    }

    private PropertyName getPropName( QName name ) {
        if ( name.getNamespaceURI() != null && !name.getNamespaceURI().equals( NULL_NS_URI ) ) {
            String prefix = name.getPrefix();
            if ( prefix == null || prefix.isEmpty() ) {
                prefix = appSchema.getXSModel().getNamespacePrefixes().get( name.getNamespaceURI() );
            }
            name = new QName( name.getNamespaceURI(), name.getLocalPart(), prefix );
        }
        return new PropertyName( name );
    }
}