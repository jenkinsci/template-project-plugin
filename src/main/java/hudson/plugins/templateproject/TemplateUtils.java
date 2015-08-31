package hudson.plugins.templateproject;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.Hudson;
import hudson.util.LogTaskListener;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;

public class TemplateUtils {
	private static final Logger logger = Logger.getLogger("TemplateProject");

	public static AbstractProject<?, ?> getProject(String projectName, AbstractBuild<?, ?> build) {
		String pName = projectName;

		if (build != null) {
			pName = TemplateUtils.getExpandedProjectName(projectName, build);

			if (Hudson.getInstance().getItemByFullName(pName) == null) {
				logger.info("[TemplateProject] Template Project '" + pName + "' not found. Skipping.");
			}
		}

		return (AbstractProject<?, ?>) Hudson.getInstance().getItemByFullName(pName);
	}

	public static String getExpandedProjectName(String projectName, AbstractBuild<?, ?> build) {
		// Limitation : Currently only supports build variable for replacement.
		// Gets into infinite loop using `getEnvironment() since it loops
		// back to `getScm().buildEnvVars()`
		return Util.replaceMacro(projectName, build.getBuildVariables());
	}

}
