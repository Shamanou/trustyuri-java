package net.trustyuri.rdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;


import com.google.code.externalsorting.ExternalSort;
import org.eclipse.rdf4j.OpenRDFException;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.RDFHandlerBase;
import org.eclipse.rdf4j.rio.helpers.RDFaParserSettings;

public class TransformLargeRdf {

	public static void main(String[] args) throws IOException, TrustyUriException {
		File inputFile = new File(args[0]);
		String baseName = "";
		if (args.length > 1) {
			baseName = args[1];
		} else {
			baseName = inputFile.getName().replaceFirst("[.][^.]+$", "");
		}
		TransformLargeRdf t = new TransformLargeRdf(inputFile, baseName);
		t.transform();
	}

	private File inputFile;
	private String inputDir;
	private String baseName;
	private MessageDigest md;
	private URI baseUri;
	private String fileName, ext;

	public TransformLargeRdf(File inputFile, String baseName) {
		this.inputFile = inputFile;
		this.baseName = baseName;
	}

	public URI transform() throws IOException, TrustyUriException {
		baseUri = TransformRdf.getBaseURI(baseName);
		md = RdfHasher.getDigest();
		inputDir = inputFile.getParent();
		TrustyUriResource r = new TrustyUriResource(inputFile);
		RDFFormat format = r.getFormat(RDFFormat.TURTLE).get();

		String name = baseName;
		if (baseName.indexOf("/") > 0) {
			name = baseName.replaceFirst("^.*[^A-Za-z0-9.\\-_]([A-Za-z0-9.\\-_]*)$", "$1");
		}
		fileName = name;
		ext = "";
		if (!format.getFileExtensions().isEmpty()) {
			ext = "." + format.getFileExtensions().get(0);
		}

		RDFParser p = Rio.createParser(format);
		p.getParserConfig().set(RDFaParserSettings.FAIL_ON_RDFA_UNDEFINED_PREFIXES, true);
		File sortInFile = new File(inputDir, fileName + ".temp.sort-in");
		final FileOutputStream preOut = new FileOutputStream(sortInFile);
		p.setRDFHandler(new RdfPreprocessor(new RDFHandlerBase() {
			
			@Override
			public void handleStatement(Statement st) throws RDFHandlerException {
				String s = SerStatementComparator.toString(st) + "\n";
				try {
					preOut.write(s.getBytes());
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}

		}, baseUri));
		BufferedReader reader = new BufferedReader(r.getInputStreamReader(), 64*1024);
		try {
			p.parse(reader, "");
		} catch (OpenRDFException ex) {
			throw new TrustyUriException(ex);
		} finally {
			reader.close();
			preOut.close();
		}

		File sortOutFile = new File(inputDir, fileName + ".temp.sort-out");
		File sortTempDir = new File(inputDir, fileName + ".temp");
		sortTempDir.mkdir();
		Comparator<String> cmp = new SerStatementComparator();
		Charset cs = Charset.defaultCharset();
		System.gc();
		List<File> tempFiles = ExternalSort.sortInBatch(sortInFile, cmp, 1024, cs, sortTempDir, false);
		ExternalSort.mergeSortedFiles(tempFiles, sortOutFile, cmp, cs);
		sortInFile.delete();
		sortTempDir.delete();

		BufferedReader br = new BufferedReader(new FileReader(sortOutFile));
		String line;
		Statement previous = null;
		while ((line = br.readLine()) != null) {
			Statement st = SerStatementComparator.fromString(line);
			if (!st.equals(previous)) {
				RdfHasher.digest(st, md);
			}
			previous = st;
		}
		br.close();

		String artifactCode = RdfHasher.getArtifactCode(md);
		String acFileName = fileName;
		if (acFileName.length() == 0) {
			acFileName = artifactCode + ext;
		} else {
			acFileName += "." + artifactCode + ext;
		}
		OutputStream out;
		if (inputFile.getName().matches(".*\\.(gz|gzip)")) {
			out = new GZIPOutputStream(new FileOutputStream(new File(inputDir, acFileName + ".gz")));
		} else {
			out = new FileOutputStream(new File(inputDir, acFileName));
		}
		RDFWriter writer = Rio.createWriter(format, new OutputStreamWriter(out, Charset.forName("UTF-8")));
		final HashAdder replacer = new HashAdder(baseUri, artifactCode, writer, null);

		br = new BufferedReader(new FileReader(sortOutFile));
		try {
			replacer.startRDF();
			previous = null;
			while ((line = br.readLine()) != null) {
				Statement st = SerStatementComparator.fromString(line);
				if (!st.equals(previous)) {
					replacer.handleStatement(st);
				}
				previous = st;
			}
			replacer.endRDF();
		} catch (RDFHandlerException ex) {
			throw new TrustyUriException(ex);
		} finally {
			br.close();
			sortOutFile.delete();
		}
		out.close();

		return RdfUtils.getTrustyUri(baseUri, artifactCode);
	}

}
