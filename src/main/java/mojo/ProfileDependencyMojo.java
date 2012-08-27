package mojo;

import java.util.StringTokenizer;
import java.util.Properties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.lang.reflect.Method;

import org.apache.maven.plugin.logging.Log;

import org.apache.maven.model.Dependency;	

import org.apache.maven.project.MavenProject;

import org.apache.maven.profiles.Profile;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.AbstractMojo;

/**
 * Profile dependency injection. 
 * Injects dependencies from profile properties. Gets profile properties matching given prefix, then parses those properties. Format used in those properties to define dependency is groupId:artifactId:version:type:scope (currently none part can be omited). 
 * Each property can define one or more dependencies, dependency strings being separated by space (' ').
 * Injected dependencies are then treated by standard dependency mecanism
 * (as static dependencies).
 *
 * @author Cedric Chantepie
 * @executionStrategy once-per-session
 * @goal inject
 * @phase validate
 */
public class ProfileDependencyMojo extends AbstractMojo {
    // --- Properties ---

    /**
     * Current project
     * @parameter default-value="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * Prefix for profile properties defining dependencies
     * @parameter
     * @required
     */
    private String prefix = null;

    // ---

    /**
     * {@inheritDoc}
     */
    public void execute() 
	throws MojoExecutionException {

	Log log = getLog();

	log.info("Injecting profile dependencies");
	log.debug("prefix = " + this.prefix);

	if (this.prefix == null ||
	    this.prefix.trim().length() == 0) {

	    log.error("No property prefix");

	    return;
	} // end of if

	// ---

	List profiles = this.project.getActiveProfiles();

	if (profiles == null || profiles.isEmpty()) {
	    log.warn("No profile");
	    log.debug("profiles = " + profiles);

	    return;
	} // end of if

	// ---

	ArrayList<Dependency> dynDeps = new ArrayList<Dependency>();
	Method getProps = null;

	Properties props;
	String value;
	String depSpec;
	StringTokenizer vtok;
	StringTokenizer ptok;
	Dependency dep;
	for (Object profile : profiles) {
	    if (getProps == null) {
		try {
		    getProps = profile.getClass().
			getMethod("getProperties", new Class[0]);

		} catch (Exception e) {
		    throw new MojoExecutionException("Fails to load properties getter", e);
		} // end of catch
	    } // end of if

	    try {
		props = (Properties) getProps.
		    invoke(profile, new Object[0]);
	    
	    } catch (Exception e) {
		throw new MojoExecutionException("Fails to get profile properties", e);
	    } // end of catch

	    // ---

	    log.debug("properties = " + props);
	    
	    for (Object key : props.keySet()) {
		log.debug("property key = " + key);

		if (!((String) key).startsWith(this.prefix)) {
		    continue;
		} // end of if

		// ---

		value = props.getProperty((String) key);

		log.debug("Find matching property: " + 
			  key + " = " + value);
		
		vtok = new StringTokenizer(value, " ");

		while (vtok.hasMoreTokens()) {
		    depSpec = vtok.nextToken();
		    dep = new Dependency();

		    log.debug("dependency specification = " +
			      depSpec);

		    ptok = new StringTokenizer(depSpec, ":");

		    for (int i = 0; ptok.hasMoreTokens(); i++) {
			switch (i) {
			case 0: // groupId
			    dep.setGroupId(ptok.nextToken());
			    break;
			case 1: // artifactId
			    dep.setArtifactId(ptok.nextToken());
			    break;
			case 2: // version
			    dep.setVersion(ptok.nextToken());
			    break;
			case 3: // type
			    dep.setType(ptok.nextToken());
			    break;
			case 4: // scope
			    dep.setScope(ptok.nextToken());
			    break;
			default:
			    ptok.nextToken();
			} // end of switch
		    } // end of for

		    log.debug("dependency instance = " + dep);

		    dynDeps.add(dep);
		} // end of while
	    } // end of for
	} // end of while

	log.debug("injected dependencies = " + dynDeps);

	this.project.getDependencies().
	    addAll(dynDeps);

	for (Dependency d : dynDeps) {
	    log.info("* " + d);
	} // end of for
    } // end of execute
} // end of class ProfileDependencyMojo
