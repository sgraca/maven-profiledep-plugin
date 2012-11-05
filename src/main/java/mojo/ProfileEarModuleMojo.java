package mojo;


import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Profile EAR module injection.
 * Injects dependencies from profile properties. Gets profile properties matching given prefix, then parses those properties. Format used in those properties to define module is groupId:artifactId:earModuleType[:contextRoot] (contextRoot is only required for web module).
 * earModuleType part must correspond to a configuration element name for maven-ear-plugin. To use a war artifact as module, earModuleType will be webModule as it is the way it would be defined in plain EAR configuration.
 * Each property can define one or more modules, module strings being separated by space (' ').
 * Relies on maven-ear-plugin, so that injected modules are then treated
 * as those specified in pom.xml in the usage way. As usually defined EAR modules, for each of them a matching dependency must be found by maven-ear-plugin (either statically or injected dependency).
 *
 * @author Cedric Chantepie
 * @executionStrategy once-per-session
 * @goal inject-ear-module
 * @phase validate
 */
public class ProfileEarModuleMojo extends AbstractMojo {
    // --- Constants ---

    /**
     * groupId of ear plugin
     * @value "org.apache.maven.plugins"
     */
    private static final String EAR_PLUGIN_GROUP_ID = "org.apache.maven.plugins";

    /**
     * artifactId of ear plugin
     * @value "maven-ear-plugin"
     */
    private static final String EAR_PLUGIN_ARTIFACT_ID = "maven-ear-plugin";

    // --- Properties ---

    /**
     * Current project
     * @parameter default-value="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * Prefix for profile properties defining EAR modules
     * @parameter
     * @required
     */
    private String prefix = null;

    // ---

    /**
     * Returns EAR plugin from current project.
     */
    private Plugin earPlugin() {
	Log log = getLog();
	List plugins = this.project.getBuildPlugins();
	Plugin plugin;

	for (Object p : plugins) {
	    log.debug("plugin = " + p);

	    plugin = (Plugin) p;

	    if (EAR_PLUGIN_GROUP_ID.
		equals(plugin.getGroupId()) &&
		EAR_PLUGIN_ARTIFACT_ID.
		equals(plugin.getArtifactId())) {

		return plugin;
	    } // end of if
	} // end of for

	return null;
    } // end of earPlugin

    /**
     * {@inheritDoc}
     */
    public void execute()
	throws MojoExecutionException {

	Log log = getLog();

	log.info("Injecting profile EAR modules");
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

	Plugin earPlugin = earPlugin();

	if (earPlugin == null) {
	    log.error("EAR plugin is not set up");
	    log.debug("ear plugin = " + earPlugin);

	    return;
	} // end of if

	// ---

	Xpp3Dom config = null;

	try {
	    config = (Xpp3Dom) earPlugin.getConfiguration();
	} catch (Exception e) {
	    log.error("Unsupported configuration");
	    log.debug("configuration = " + config);

	    return;
	} // end of if

	// ---

	ArrayList<String> moduleSpecs = new ArrayList<String>();
	ArrayList<Xpp3Dom> modules = new ArrayList<Xpp3Dom>();
	Method getProps = null;

	Properties props;
	String value;
	String moduleSpec;
	StringTokenizer vtok;
	StringTokenizer ptok;
	StringBuffer moduleConfigXml;
	Xpp3Dom moduleConfig;
	String type;
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
		    moduleSpec = vtok.nextToken();
		    moduleConfigXml = new StringBuffer();
		    moduleConfig = null;
		    type = null;

		    log.debug("dependency specification = " +
			      moduleSpec);

		    ptok = new StringTokenizer(moduleSpec, ":");

		    for (int i = 0; ptok.hasMoreTokens(); i++) {
			switch (i) {
			case 0: // groupId
			    moduleConfigXml.
				append("<groupId>").
				append(ptok.nextToken()).
				append("</groupId>");

			    break;
			case 1: // artifactId
			    moduleConfigXml.
				append("<artifactId>").
				append(ptok.nextToken()).
				append("</artifactId>");

			    break;
			case 2: // module type
			    type = ptok.nextToken();

			case 3: // context root (only for web module)
			    moduleConfigXml.
				append("<contextRoot>").
				append(ptok.nextToken()).
				append("</contextRoot>");

			    break;

			default:
			    ptok.nextToken();
			} // end of switch
		    } // end of for

		    if (type == null) {
			log.error("Module type not found");
			log.debug("type = " + type);

			continue;
		    } // end of if

		    // ---

		    moduleConfigXml.
			insert(0, '<' + type + "Module>");

		    moduleConfigXml.append("</").
			append(type).append("Module>");

		    try {
			moduleConfig = Xpp3DomBuilder.
			    build(new StringReader(moduleConfigXml.toString()));

			log.debug("module config = " + moduleConfig);
		    } catch (Exception e) {
			e.printStackTrace();
		    }

		    if (moduleConfig == null) {
			log.error("Fails to create module configuration");
			log.debug("module config XML = " + moduleConfigXml);

			continue;
		    } // end of if

		    // ---

		    modules.add(moduleConfig);
		    moduleSpecs.add(moduleSpec);
		} // end of while
	    } // end of for
	} // end of while

	log.debug("injected modules = " + modules);
	Xpp3Dom modulesElmt = config.getChild("modules");

	int i = 0;
	for (Xpp3Dom module : modules) {
	    modulesElmt.addChild(module);

	    log.info("* " + moduleSpecs.get(i));
	} // end of for
    } // end of execute
} // end of class ProfileEarModuleMojo
