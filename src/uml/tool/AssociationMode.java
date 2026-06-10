package uml.tool;

import uml.model.LinkType;

/**
 * Association link-creation tool (Phase 5 / T032).
 *
 * Produces a Link with an open chevron arrowhead at the target end.
 * All interaction logic lives in AbstractLinkMode; this class only
 * declares which LinkType it creates — Open-Closed Principle (Principle III).
 */
public class AssociationMode extends AbstractLinkMode {

    @Override
    protected LinkType getLinkType() {
        return LinkType.ASSOCIATION;
    }
}
