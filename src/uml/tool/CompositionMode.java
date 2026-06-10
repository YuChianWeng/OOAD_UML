package uml.tool;

import uml.model.LinkType;

/**
 * Composition link-creation tool (Phase 5 / T034).
 *
 * Produces a Link with a filled black diamond at the source (owner / composite) end.
 * All interaction logic lives in AbstractLinkMode; this class only
 * declares which LinkType it creates — Open-Closed Principle (Principle III).
 */
public class CompositionMode extends AbstractLinkMode {

    @Override
    protected LinkType getLinkType() {
        return LinkType.COMPOSITION;
    }
}
