package net.trustyuri.rdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;

import org.nanopub.CustomTrigWriterFactory;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.rio.Rio;

public class TransformRdfGraph {

	// TODO only transform blank nodes that appear within the given graph

	static {
		RDFWriterRegistry.getInstance().add(new CustomTrigWriterFactory());
	}

	public static void main(String[] args) throws IOException, TrustyUriException {
		if (args.length < 2) {
			throw new RuntimeException("Not enough arguments: <file> <graph-uri1> (<graph-uri2> ...)");
		}
		File inputFile = new File(args[0]);
		List<URI> baseUris = new ArrayList<URI>();
		for (int i = 1 ; i < args.length ; i++) {
			String arg = args[i];
			if (arg.contains("://")) {
				baseUris.add(new URIImpl(arg));
			} else {
				BufferedReader reader = new BufferedReader(new FileReader(arg));
				String line;
				while ((line = reader.readLine()) != null) {
				   baseUris.add(new URIImpl(line));
				}
				reader.close();
			}
		}
		RdfFileContent content = RdfUtils.load(new TrustyUriResource(inputFile));
		String outputFilePath = inputFile.getPath().replaceFirst("[.][^.]+$", "") + ".t";
		RDFFormat format = content.getOriginalFormat();
		if (!format.getFileExtensions().isEmpty()) {
			outputFilePath += "." + format.getFileExtensions().get(0);
		}
		transform(content, new File(outputFilePath), baseUris.toArray(new URI[baseUris.size()]));
	}

	public static void transform(RdfFileContent content, File outputFile, URI... baseUris)
			throws IOException, TrustyUriException {
		try {
			OutputStream out = new FileOutputStream(outputFile);
			processBaseUris(content, Rio.createWriter(content.getOriginalFormat(), out), baseUris);
			out.close();
		} catch (RDFHandlerException ex) {
			throw new TrustyUriException(ex);
		}
	}

	public static void transform(RdfFileContent content, RDFHandler handler, URI... baseUris)
			throws IOException, TrustyUriException {
		try {
			processBaseUris(content, handler, baseUris);
		} catch (RDFHandlerException ex) {
			throw new TrustyUriException(ex);
		}
	}

	public static void transform(InputStream in, RDFFormat format, OutputStream out, URI... baseUris)
			throws IOException, TrustyUriException {
		RdfFileContent content = RdfUtils.load(in, format);
		try {
			processBaseUris(content, Rio.createWriter(format, out), baseUris);
		} catch (RDFHandlerException ex) {
			throw new TrustyUriException(ex);
		}
		out.close();
	}

	private static void processBaseUris(RdfFileContent content, RDFHandler handler, URI... baseUris)
			throws RDFHandlerException, TrustyUriException {
		for (URI baseUri : baseUris) {
			RdfFileContent newContent = new RdfFileContent(content.getOriginalFormat());
			RdfPreprocessor preprocessor = new RdfPreprocessor(newContent, baseUri);
			content.propagate(preprocessor);
			content = newContent;
			String artifactCode = RdfHasher.makeGraphArtifactCode(content.getStatements(), baseUri);
			newContent = new RdfFileContent(content.getOriginalFormat());
			HashAdder hashAdder = new HashAdder(baseUri, artifactCode, newContent, null);
			content.propagate(hashAdder);
			content = newContent;
		}
		content.propagate(handler);
	}

}
