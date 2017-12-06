package hudson.plugins.templateproject;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.*;
import hudson.util.LogTaskListener;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;

public class TemplateUtils {
	private static final Logger logger = Logger.getLogger("TemplateProject");

	public static AbstractProject<?, ?> getProject(String projectName, AbstractBuild<?, ?> build) {
		String pName = projectName;

		if (build != null) {
			pName = TemplateUtils.getExpandedProjectName(projectName, build);

			if (getItemByFullNameWorkaround(pName) == null) {
				logger.info("[TemplateProject] Template Project '" + pName + "' not found. Skipping.");
			}
		}

		return getItemByFullNameWorkaround(pName);
	}

	private static AbstractProject<?, ?> getItemByFullNameWorkaround(String projectName) {
		Item project = Hudson.getInstance().getItemByFullName(projectName);
		if (project == null) { //for some reason sometimes it returns simply null although the project is existing
			logger.fine("[TemplateProject] fall back for getItemByFullName while searching '" + projectName + "'");
			for (TopLevelItem item : Hudson.getInstance().getItems()) {
				if (projectName.equals(item.getFullName()))
					return (AbstractProject<?, ?>) item;
			}
		}

		return (AbstractProject<?, ?>) project;
	}

	public static String getExpandedProjectName(String projectName, AbstractBuild<?, ?> build) {
		// Limitation : Currently only supports build variable for replacement.
		// Gets into infinite loop using `getEnvironment() since it loops
		// back to `getScm().buildEnvVars()`
		return Util.replaceMacro(projectName, build.getBuildVariables());
	}

}
