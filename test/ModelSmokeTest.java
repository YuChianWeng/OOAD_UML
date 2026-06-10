import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import uml.model.BasicObject;
import uml.model.CompositeObject;
import uml.model.DiagramModel;
import uml.model.GraphicObject;
import uml.model.Link;
import uml.model.LinkType;
import uml.model.OvalObject;
import uml.model.RectObject;

/** Basic non-GUI smoke coverage for the UML Editor model layer. */
public class ModelSmokeTest {

    public static void main(String[] args) {
        DiagramModel model = new DiagramModel();
        RectObject rect = new RectObject(new Rectangle(10, 10, 100, 60));
        OvalObject oval = new OvalObject(new Rectangle(200, 100, 80, 50));

        model.addObject(rect);
        model.addObject(oval);
        assertEquals(2, model.getObjects().size(), "objects are added");
        assertEquals(8, rect.getPorts().size(), "rect has 8 ports");
        assertEquals(4, oval.getPorts().size(), "oval has 4 ports");

        model.addLink(new Link(LinkType.ASSOCIATION, rect, 3, oval, 3));
        assertEquals(1, model.getLinks().size(), "link is added");

        Point oldPort = new Point(rect.getPorts().get(3));
        rect.moveTo(40, 40);
        Point movedPort = rect.getPorts().get(3);
        assertTrue(!oldPort.equals(movedPort), "ports recompute after move");

        rect.resize(new Rectangle(0, 0, 1, 1));
        Rectangle resized = rect.getBoundingBox();
        assertTrue(resized.width >= 20 && resized.height >= 20,
            "resize clamps to at least 20x20");

        List<GraphicObject> selected = Arrays.asList(rect, oval);
        CompositeObject composite = model.group(selected);
        assertEquals(1, model.getObjects().size(), "group replaces objects with composite");
        assertTrue(model.getObjects().get(0) == composite, "composite is top-level after group");
        assertEquals(2, composite.getChildren().size(), "composite owns children");

        List<GraphicObject> restored = model.ungroup(composite);
        assertEquals(2, restored.size(), "ungroup returns direct children");
        assertEquals(2, model.getObjects().size(), "ungroup restores children to model");
        assertTrue(model.getBasicObjectAt(rect.getBoundingBox().getLocation()) instanceof BasicObject,
            "basic hit-test returns a BasicObject");

        System.out.println("MODEL_SMOKE_OK");
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
