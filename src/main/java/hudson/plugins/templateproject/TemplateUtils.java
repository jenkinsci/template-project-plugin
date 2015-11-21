package hudson.plugins.templateproject;

import com.google.common.annotations.VisibleForTesting;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public class TemplateUtils {

	private static TemplateUtils instance;

	private final Hudson hudson;

	private TemplateUtils(){
		this(Hudson.getInstance());
	}

	@VisibleForTesting
	TemplateUtils(Hudson hudson){
		this.hudson = hudson;
	}

	@CheckForNull
	public AbstractProject<?, ?> getProject(String projectName) {
		return (AbstractProject<?, ?>) hudson.getItemByFullName(projectName);
	}

	@CheckForNull
	public AbstractProject<?, ?> getProject(String projectName, @Nonnull AbstractBuild<?, ?> build) {
		return getProject(getExpandedProjectName(projectName, build));
	}

	private String getExpandedProjectName(String projectName, AbstractBuild<?, ?> build) {
		// Limitation : Currently only supports build variable for replacement.
		// Gets into infinite loop using `getEnvironment() since it loops
		// back to `getScm().buildEnvVars()`
		return Util.replaceMacro(projectName, build.getBuildVariables());
	}

	public static TemplateUtils getInstance(){
		if(instance == null){
			instance = new TemplateUtils();
		}

		return instance;
	}

}
