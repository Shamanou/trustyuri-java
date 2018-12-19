package net.trustyuri.rdf;

import javafx.util.Pair;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.ContextStatementImpl;
import org.eclipse.rdf4j.model.impl.StatementImpl;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RdfFileContent implements RDFHandler {

    private Map<Value, Value> rdfEntityMap = new HashMap<Value, Value>();

    private RDFFormat originalFormat = null;
    private List<Pair<String, String>> namespaces;
    private List<Statement> statements;

    public RdfFileContent(RDFFormat originalFormat) {
        this.originalFormat = originalFormat;
    }

    @Override
    public void startRDF() throws RDFHandlerException {
        namespaces = new ArrayList<Pair<String, String>>();
        statements = new ArrayList<Statement>();
    }

    @Override
    public void endRDF() throws RDFHandlerException {
    }

    @Override
    public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
        namespaces.add(new Pair<>(prefix, uri));
    }

    @Override
    public void handleStatement(Statement st) throws RDFHandlerException {
        Resource subj = (Resource) rdfEntityMap.get(st.getSubject());
        if (subj == null) {
            subj = st.getSubject();
            rdfEntityMap.put(subj, subj);
        }
        URI pred = (URI) rdfEntityMap.get(st.getPredicate());
        if (pred == null) {
            pred = st.getPredicate();
            rdfEntityMap.put(pred, pred);
        }
        Value obj = rdfEntityMap.get(st.getObject());
        if (obj == null) {
            obj = st.getObject();
            rdfEntityMap.put(obj, obj);
        }
        Resource context = null;
        if (st.getContext() == null) {
            st = new StatementImpl(subj, pred, obj);
        } else {
            context = (Resource) rdfEntityMap.get(st.getContext());
            if (context == null) {
                context = st.getContext();
                rdfEntityMap.put(context, context);
            }
            st = new ContextStatementImpl(subj, pred, obj, context);
        }

        statements.add(st);
    }

    @Override
    public void handleComment(String comment) throws RDFHandlerException {
        // Ignore comments
    }

    public List<Statement> getStatements() {
        return statements;
    }

    public void propagate(RDFHandler handler) throws RDFHandlerException {
        propagate(handler, true);
    }

    public void propagate(RDFHandler handler, boolean doStartAndEnd) throws RDFHandlerException {
        if (doStartAndEnd) handler.startRDF();
        for (Pair<String, String> ns : namespaces) {
            handler.handleNamespace(ns.getKey(), ns.getValue());
        }
        for (Statement st : statements) {
            handler.handleStatement(st);
        }
        if (doStartAndEnd) handler.endRDF();
    }

    public RDFFormat getOriginalFormat() {
        return originalFormat;
    }

}
