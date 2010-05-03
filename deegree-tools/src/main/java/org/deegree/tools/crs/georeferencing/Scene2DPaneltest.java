//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
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
package org.deegree.tools.crs.georeferencing;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.vecmath.Point2d;

/**
 * TODO add class documentation here
 * 
 * @author <a href="mailto:thomas@deegree.org">Steffen Thomas</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class Scene2DPaneltest extends JPanel {

    BufferedImage imageToDraw;

    private Point2d beginDrawImageAtPosition;

    private Point2d imageDimension;

    private Point2d imageMargin;

    private final double margin = 0.1;

    public void init() {
        imageMargin = new Point2d( imageToDraw.getWidth() * margin, imageToDraw.getHeight() * margin );
        beginDrawImageAtPosition = new Point2d( 0 - imageMargin.getX(), 0 - imageMargin.getY() );
        imageDimension = new Point2d( imageToDraw.getWidth() + imageMargin.getX() * 2, imageToDraw.getHeight()
                                                                                       + imageMargin.getY() * 2 );

    }

    @Override
    public void paintComponent( Graphics g ) {
        super.paintComponent( g );
        Graphics2D g2 = (Graphics2D) g;

        if ( imageToDraw != null ) {

            g2.drawImage( imageToDraw, (int) beginDrawImageAtPosition.getX(), (int) beginDrawImageAtPosition.getY(),
                          (int) imageDimension.getX(), (int) imageDimension.getY(), this );
            System.out.println( "Begin: " + beginDrawImageAtPosition + " Boundaries: " + imageDimension );

        }

    }

    public BufferedImage getImageToDraw() {
        return imageToDraw;
    }

    public void setImageToDraw( BufferedImage imageToDraw ) {
        this.imageToDraw = imageToDraw;
    }

    public double getMargin() {
        return margin;
    }

    public Point2d getBeginDrawImageAtPosition() {
        return beginDrawImageAtPosition;
    }

    public void setBeginDrawImageAtPosition( Point2d beginDrawImageAtPosition ) {
        this.beginDrawImageAtPosition = beginDrawImageAtPosition;
    }

    public Point2d getImageDimension() {
        return imageDimension;
    }

    public Point2d getImageMargin() {
        return imageMargin;
    }

    public void addScene2DMouseListener( MouseListener m ) {

        this.addMouseListener( m );

    }

}
