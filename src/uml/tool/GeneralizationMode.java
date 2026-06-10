package uml.tool;

import uml.model.LinkType;

/**
 * Generalization link-creation tool (Phase 5 / T033).
 *
 * Produces a Link with a hollow equilateral triangle arrowhead at the target
 * (superclass) end.
 * All interaction logic lives in AbstractLinkMode; this class only
 * declares which LinkType it creates — Open-Closed Principle (Principle III).
 */
public class GeneralizationMode extends AbstractLinkMode {

    @Override
    protected LinkType getLinkType() {
        return LinkType.GENERALIZATION;
    }
}
