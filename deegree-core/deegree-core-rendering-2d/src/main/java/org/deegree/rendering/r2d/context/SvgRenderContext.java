package org.deegree.rendering.r2d.context;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

public class SvgRenderContext extends Java2DRenderContext {
        
    private SvgRenderContext ( RenderingInfo info, Document document, OutputStream outputStream ) {
        super( info, new SVGGraphics2D( document ), outputStream );
    }
    
    public static RenderContext createInstance( RenderingInfo info, OutputStream outputStream ) {
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document document = domImpl.createDocument( "http://www.w3.org/2000/svg", "svg", null );
        
        return new SvgRenderContext( info, document, outputStream );
    }
    
    @Override
    public boolean close () throws IOException {
        try {
            if ( outputStream != null ) {
                final Writer writer = new OutputStreamWriter( outputStream, "UTF-8" );
                ((SVGGraphics2D)graphics).stream( writer, true );
            }
            graphics.dispose();
            return outputStream != null;
        } finally {
            closeQuietly( outputStream );
        }
    }

    @Override
    public void applyOptions (final MapOptions options) {
        // TODO: apply options.
    }
}