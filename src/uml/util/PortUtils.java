package uml.util;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Stateless port-computation helpers.
 *
 * Port positions are derived purely from a bounding box; no object state is
 * stored here (Principle II: utility layer has no coupling to domain objects).
 *
 * Port index conventions — used consistently by Link and SelectMode:
 *
 *   Rect (8 ports, clockwise from TL):
 *     0=TL  1=TM  2=TR
 *     7=ML        3=MR
 *     6=BL  5=BM  4=BR
 *
 *   Oval (4 ports, clockwise from top):
 *     0=top  1=right  2=bottom  3=left
 */
public final class PortUtils {

    private PortUtils() {}

    /**
     * Compute 8 port points for a rectangle in clockwise order starting top-left:
     * TL(0), TM(1), TR(2), MR(3), BR(4), BM(5), BL(6), ML(7).
     */
    public static List<Point> computeRectPorts(Rectangle bbox) {
        int x  = bbox.x,          y  = bbox.y;
        int x2 = bbox.x + bbox.width, y2 = bbox.y + bbox.height;
        int cx = bbox.x + bbox.width  / 2;
        int cy = bbox.y + bbox.height / 2;

        List<Point> ports = new ArrayList<>(8);
        ports.add(new Point(x,  y ));   // 0 TL
        ports.add(new Point(cx, y ));   // 1 TM
        ports.add(new Point(x2, y ));   // 2 TR
        ports.add(new Point(x2, cy));   // 3 MR
        ports.add(new Point(x2, y2));   // 4 BR
        ports.add(new Point(cx, y2));   // 5 BM
        ports.add(new Point(x,  y2));   // 6 BL
        ports.add(new Point(x,  cy));   // 7 ML
        return ports;
    }

    /**
     * Compute 4 port points for an oval in clockwise order:
     * top(0), right(1), bottom(2), left(3).
     */
    public static List<Point> computeOvalPorts(Rectangle bbox) {
        int cx = bbox.x + bbox.width  / 2;
        int cy = bbox.y + bbox.height / 2;

        List<Point> ports = new ArrayList<>(4);
        ports.add(new Point(cx,              bbox.y));               // 0 top
        ports.add(new Point(bbox.x + bbox.width, cy));               // 1 right
        ports.add(new Point(cx,              bbox.y + bbox.height)); // 2 bottom
        ports.add(new Point(bbox.x,          cy));                   // 3 left
        return ports;
    }
}
