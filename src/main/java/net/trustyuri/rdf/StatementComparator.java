package net.trustyuri.rdf;

import org.eclipse.rdf4j.model.*;

import java.util.Comparator;

public class StatementComparator implements Comparator<Statement> {

    public StatementComparator() {
    }

    public static int compareStatement(Statement st1, Statement st2) {
        int c;
        c = compareContext(st1, st2);
        if (c != 0) return c;
        c = compareSubject(st1, st2);
        if (c != 0) return c;
        c = comparePredicate(st1, st2);
        if (c != 0) return c;
        return compareObject(st1, st2);
    }

    private static int compareContext(Statement st1, Statement st2) {
        Resource r1 = st1.getContext();
        Resource r2 = st2.getContext();
        if (r1 == null && r2 == null) {
            return 0;
        } else if (r1 == null && r2 != null) {
            return -1;
        } else if (r1 != null && r2 == null) {
            return 1;
        }
        return compareResource(r1, r2);
    }

    private static int compareSubject(Statement st1, Statement st2) {
        return compareResource(st1.getSubject(), st2.getSubject());
    }

    private static int comparePredicate(Statement st1, Statement st2) {
        return compareURIs(st1.getPredicate(), st2.getPredicate());
    }

    private static int compareObject(Statement st1, Statement st2) {
        Value v1 = st1.getObject();
        Value v2 = st2.getObject();
        if (v1 instanceof Literal && !(v2 instanceof Literal)) {
            return 1;
        } else if (!(v1 instanceof Literal) && v2 instanceof Literal) {
            return -1;
        } else if (v1 instanceof Literal) {
            return compareLiteral((Literal) v1, (Literal) v2);
        } else {
            return compareResource((Resource) v1, (Resource) v2);
        }
    }

    private static int compareResource(Resource r1, Resource r2) {
        if (r1 instanceof BNode) {
            throw new RuntimeException("Unexpected blank node");
        } else {
            return compareURIs((URI) r1, (URI) r2);
        }
    }

    private static int compareLiteral(Literal l1, Literal l2) {
        String s1 = l1.stringValue();
        String s2 = l2.stringValue();
        if (!s1.equals(s2)) {
            return s1.compareTo(s2);
        }
        s1 = null;
        s2 = null;
        if (l1.getDatatype() != null) s1 = l1.getDatatype().toString();
        if (l1.getLanguage().isPresent()) s1 = null;
        if (l2.getDatatype() != null) s2 = l2.getDatatype().toString();
        if (l2.getLanguage().isPresent()) s2 = null;
        if (s1 == null && s2 != null) {
            return -1;
        } else if (s1 != null && s2 == null) {
            return 1;
        } else if (s1 != null && !s1.equals(s2)) {
            return s1.compareTo(s2);
        }
        s1 = l1.getLanguage().get();
        s2 = l2.getLanguage().get();
        if (s1 == null && s2 != null) {
            return -1;
        } else if (s1 != null && s2 == null) {
            return 1;
        } else if (s1 != null && !s1.equals(s2)) {
            return s1.compareTo(s2);
        }
        return 0;
    }

    private static int compareURIs(URI uri1, URI uri2) {
        return uri1.toString().compareTo(uri2.toString());
    }

    @Override
    public int compare(Statement st1, Statement st2) {
        return compareStatement(st1, st2);
    }

}
