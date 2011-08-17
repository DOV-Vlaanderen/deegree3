/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
   Department of Geography, University of Bonn
 and
   lat/lon GmbH

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
/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.deegree.rendering.r2d.strokes;

import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

/**
 * <code>ZigzagStroke</code>
 *
 * @author Jerry Huxtable
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 *
 * @version $Revision$, $Date$
 */
public class ZigzagStroke implements Stroke {
    private float amplitude = 10.0f;

    private float wavelength = 10.0f;

    private Stroke stroke;

    private static final float FLATNESS = 1;

    /**
     * @param stroke
     * @param amplitude
     * @param wavelength
     */
    public ZigzagStroke( Stroke stroke, float amplitude, float wavelength ) {
        this.stroke = stroke;
        this.amplitude = amplitude;
        this.wavelength = wavelength;
    }

    public Shape createStrokedShape( Shape shape ) {
        GeneralPath result = new GeneralPath();
        PathIterator it = new FlatteningPathIterator( shape.getPathIterator( null ), FLATNESS );
        float points[] = new float[6];
        float moveX = 0, moveY = 0;
        float lastX = 0, lastY = 0;
        float thisX = 0, thisY = 0;
        int type = 0;
        float next = 0;
        int phase = 0;

        while ( !it.isDone() ) {
            type = it.currentSegment( points );
            switch ( type ) {
            case PathIterator.SEG_MOVETO:
                moveX = lastX = points[0];
                moveY = lastY = points[1];
                result.moveTo( moveX, moveY );
                next = wavelength / 2;
                break;

            case PathIterator.SEG_CLOSE:
                points[0] = moveX;
                points[1] = moveY;
                // Fall into....

            case PathIterator.SEG_LINETO:
                thisX = points[0];
                thisY = points[1];
                float dx = thisX - lastX;
                float dy = thisY - lastY;
                float distance = (float) Math.sqrt( dx * dx + dy * dy );
                if ( distance >= next ) {
                    float r = 1.0f / distance;
                    while ( distance >= next ) {
                        float x = lastX + next * dx * r;
                        float y = lastY + next * dy * r;
                        if ( ( phase & 1 ) == 0 )
                            result.lineTo( x + amplitude * dy * r, y - amplitude * dx * r );
                        else
                            result.lineTo( x - amplitude * dy * r, y + amplitude * dx * r );
                        next += wavelength;
                        phase++;
                    }
                }
                next -= distance;
                lastX = thisX;
                lastY = thisY;
                if ( type == PathIterator.SEG_CLOSE )
                    result.closePath();
                break;
            }
            it.next();
        }

        return stroke.createStrokedShape( result );
    }

}