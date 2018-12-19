package net.trustyuri;

import org.eclipse.rdf4j.model.URI;

import java.io.File;
import java.io.IOException;

/**
 * A trusty URI module handles a particular type of content (such as RDF graphs or the byte content
 * of files).
 *
 * @author Tobias Kuhn
 */
public interface TrustyUriModule {

    /**
     * The module ID is a two-character identifier. The first character defines the type of
     * content; the second one defines the version.
     *
     * @return the module ID
     */
    String getModuleId();

    /**
     * Returns the algorithm ID used for the transformation to ni-URIs.
     *
     * @return the algorithm ID
     */
    String getAlgorithmId();

    /**
     * Returns the length of the trusty URI data part in bytes.
     *
     * @return data part length
     */
    int getDataPartLength();

    /**
     * Checks the hash for the given resource.
     *
     * @param resource the resource to be checked
     * @return true if the hash matches the content
     */
    boolean hasCorrectHash(TrustyUriResource resource) throws IOException, TrustyUriException;

    void fixTrustyFile(File file) throws IOException, TrustyUriException, UnsupportedOperationException;

    /**
     * Checks whether the given URI could be a trusty URI represented by this module.
     *
     * @param uri the URI
     * @return true if the URI matches the format of this module
     */
    boolean matches(URI uri);

}
