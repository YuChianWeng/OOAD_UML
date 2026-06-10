package uml.model;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Root of the graphic-object hierarchy.
 *
 * Every element that lives on the canvas — rectangles, ovals, and composites —
 * is a GraphicObject. The canvas iterates this type; it never casts.
 *
 * Responsibilities (single, per Principle IV):
 *   - Declare the rendering and hit-testing contract that all subclasses honour.
 */
public abstract class GraphicObject {

    /** Render this object onto the given graphics context. */
    public abstract void draw(Graphics2D g);

    /** Return true if point {@code p} is inside or on the boundary of this object. */
    public abstract boolean contains(Point p);

    /** Return the axis-aligned bounding box used for selection, grouping, and depth-sorting. */
    public abstract Rectangle getBoundingBox();

    /** Return the user-visible label (may be empty, never null). */
    public abstract String getLabel();

    /** Return the fill / label-background colour. */
    public abstract Color getLabelColor();
}
