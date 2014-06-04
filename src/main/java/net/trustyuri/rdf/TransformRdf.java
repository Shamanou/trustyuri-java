package net.trustyuri.rdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;

import org.nanopub.CustomTrigWriterFactory;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.rio.Rio;

public class TransformRdf {

	// TODO Use RB module by default if trusty URI represents a single RDF graph

	static {
		RDFWriterRegistry.getInstance().add(new CustomTrigWriterFactory());
	}

	public static void main(String[] args) throws IOException, TrustyUriException {
		File inputFile = new File(args[0]);
		String baseName;
		if (args.length > 1) {
			baseName = args[1];
		} else {
			baseName = inputFile.getName().replaceFirst("[.][^.]+$", "");
		}
		RdfFileContent content = RdfUtils.load(new TrustyUriResource(inputFile));
		transform(content, inputFile.getParent(), baseName);
	}

	public static URI transform(RdfFileContent content, String outputDir, String baseName)
			throws IOException, TrustyUriException {
		URI baseUri = getBaseURI(baseName);
		String name = baseName;
		if (baseName.indexOf("/") > 0) {
			name = baseName.replaceFirst("^.*[^A-Za-z0-9.\\-_]([A-Za-z0-9.\\-_]*)$", "$1");
		}

		content = RdfPreprocessor.run(content, baseUri);
		String artifactCode = RdfHasher.makeArtifactCode(content.getStatements());
		RDFFormat format = content.getOriginalFormat();
		String fileName = name;
		String ext = "";
		if (!format.getFileExtensions().isEmpty()) {
			ext = "." + format.getFileExtensions().get(0);
		}
		if (fileName.length() == 0) {
			fileName = artifactCode + ext;
		} else {
			fileName += "." + artifactCode + ext;
		}
		OutputStream out = new FileOutputStream(new File(outputDir, fileName));
		RDFWriter writer = Rio.createWriter(format, out);
		Map<String,String> ns = makeNamespaceMap(content.getStatements(), baseUri, artifactCode);
		try {
			content.propagate(new HashAdder(baseUri, artifactCode, writer, ns));
		} catch (RDFHandlerException ex) {
			throw new TrustyUriException(ex);
		}
		out.close();
		return RdfUtils.getTrustyUri(baseUri, artifactCode);
	}

	public static URI transform(RdfFileContent content, RDFHandler handler, String baseName)
			throws TrustyUriException {
		URI baseUri = getBaseURI(baseName);
		content = RdfPreprocessor.run(content, baseUri);
		String artifactCode = RdfHasher.makeArtifactCode(content.getStatements());
		Map<String,String> ns = makeNamespaceMap(content.getStatements(), baseUri, artifactCode);
		try {
			content.propagate(new HashAdder(baseUri, artifactCode, handler, ns));
		} catch (RDFHandlerException ex) {
			throw new TrustyUriException(ex);
		}
		return RdfUtils.getTrustyUri(baseUri, artifactCode);
	}

	public static URI transform(InputStream in, RDFFormat format, OutputStream out, String baseName)
			throws IOException, TrustyUriException {
		URI baseUri = getBaseURI(baseName);
		RdfFileContent content = RdfUtils.load(in, format);
		content = RdfPreprocessor.run(content, baseUri);
		String artifactCode = RdfHasher.makeArtifactCode(content.getStatements());
		RDFWriter writer = Rio.createWriter(format, out);
		Map<String,String> ns = makeNamespaceMap(content.getStatements(), baseUri, artifactCode);
		HashAdder replacer = new HashAdder(baseUri, artifactCode, writer, ns);
		try {
			content.propagate(replacer);
		} catch (RDFHandlerException ex) {
			throw new TrustyUriException(ex);
		}
		out.close();
		return RdfUtils.getTrustyUri(baseUri, artifactCode);
	}

	static URI getBaseURI(String baseName) {
		URI baseURI = null;
		if (baseName.indexOf("://") > 0) {
			baseURI = new URIImpl(baseName);
		}
		return baseURI;
	}

	static Map<String,String> makeNamespaceMap(List<Statement> statements, URI baseURI, String artifactCode) {
		Map<String,String> ns = new HashMap<String,String>();
		if (baseURI == null) return ns;
		String u = RdfUtils.getTrustyUriString(baseURI, artifactCode);
		ns.put("this", u);
		for (Statement st : statements) {
			addToNamespaceMap(st.getSubject(), baseURI, artifactCode, ns);
			addToNamespaceMap(st.getPredicate(), baseURI, artifactCode, ns);
			addToNamespaceMap(st.getObject(), baseURI, artifactCode, ns);
			addToNamespaceMap(st.getContext(), baseURI, artifactCode, ns);
		}
		return ns;
	}

	static void addToNamespaceMap(Value v, URI baseURI, String artifactCode, Map<String,String> ns) {
		if (!(v instanceof URI)) return;
		String uri = RdfUtils.getTrustyUriString(baseURI, artifactCode);
		String s = v.toString().replace(" ", artifactCode);
		if (!s.startsWith(uri)) return;
		UriTransformConfig c = UriTransformConfig.getDefault();
		String suffix = s.substring(uri.length());
		if (suffix.length() > 2 && suffix.charAt(0) == c.getPostHashChar() && suffix.charAt(1) == c.getBnodeChar() &&
				!(c.getBnodeChar() + "").matches("[A-Za-z0-9\\-_]")) {
			ns.put("node", uri + "..");
		} else if (suffix.matches("[^A-Za-z0-9\\-_].*")) {
			ns.put("sub", uri + suffix.charAt(0));
		}
	}

}
