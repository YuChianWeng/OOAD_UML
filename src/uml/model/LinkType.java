package uml.model;

/**
 * The three supported UML relationship types.
 *
 * Each constant drives the arrowhead renderer in Link.draw() — adding a new
 * arrowhead style means adding one constant here and one branch in Link, with
 * zero changes to the rest of the codebase (Open-Closed Principle, Principle III).
 */
public enum LinkType {
    ASSOCIATION,
    GENERALIZATION,
    COMPOSITION
}
