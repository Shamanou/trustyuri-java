package net.trustyuri.rdf;

import net.trustyuri.AbstractTrustyUriModule;
import net.trustyuri.TrustyUriResource;

public class RdfGraphModule extends AbstractTrustyUriModule {

	public static final String MODULE_ID = "RB";

	@Override
	public String getModuleId() {
		return MODULE_ID;
	}

	@Override
	public String getAlgorithmId() {
		return "sha-256";
	}

	@Override
	public int getDataPartLength() {
		return 43;
	}

	@Override
	public boolean hasCorrectHash(TrustyUriResource r) throws Exception {
		RdfFileContent content = RdfUtils.load(r);
		content = RdfPreprocessor.run(content, r.getArtifactCode());
		String ac = RdfHasher.makeGraphArtifactCode(content.getStatements());
		return r.getArtifactCode().equals(ac);
	}

}
