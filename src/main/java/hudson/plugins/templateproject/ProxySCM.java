package hudson.plugins.templateproject;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.console.HyperlinkNote;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Run;
import hudson.scm.ChangeLogParser;
import hudson.scm.NullSCM;
import hudson.scm.PollingResult;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import hudson.security.AccessControlled;
import hudson.tasks.Messages;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.List;
import org.jenkinsci.plugins.multiplescms.MultiSCM;
import org.jenkinsci.plugins.multiplescms.MultiSCMRevisionState;


public class ProxySCM extends SCM {

	private final String projectName;

	@DataBoundConstructor
	public ProxySCM(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getExpandedProjectName(AbstractBuild<?, ?> build) {
		return TemplateUtils.getExpandedProjectName(projectName, build);
	}

	// Primarily used for polling, not building.
	public AbstractProject<?, ?> getProject() {
		return TemplateUtils.getProject(projectName, null);
	}

	public SCM getProjectScm(AbstractBuild<?, ?> build) {
		try {
			return TemplateUtils.getProject(projectName, build).getScm();
		} catch (Exception e) {
			return new NullSCM();
		}
	}

	public SCM getProjectScm() {
		return getProjectScm(null);
	}

	public void checkout(@Nonnull Run<?,?> build, @Nonnull Launcher launcher, @Nonnull FilePath workspace,
			@Nonnull TaskListener listener, @CheckForNull File changelogFile, @CheckForNull SCMRevisionState baseline)
			throws IOException, InterruptedException {

		// Unique situation where MultiSCM has $None for SCMRevisionState
		// Potentially due to SCM polling and references lost, or fixed with:
		// https://github.com/jenkinsci/multiple-scms-plugin/pull/6
		// https://issues.jenkins-ci.org/browse/JENKINS-27638
		if (getProjectScm((AbstractBuild) build) instanceof MultiSCM) {
			if ((baseline == SCMRevisionState.NONE) || (baseline == null)) {
				baseline = new MultiSCMRevisionState();
			}
		}

		AbstractProject p = TemplateUtils.getProject(getProjectName(), (AbstractBuild) build);
		listener.getLogger().println("[TemplateProject] Using SCM from: " + HyperlinkNote.encodeTo('/'+ p.getUrl(), p.getFullDisplayName()));
		getProjectScm((AbstractBuild) build).checkout(build, launcher, workspace, listener, changelogFile, baseline);
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		return getProjectScm().createChangeLogParser();
	}

	@Override
	@Deprecated
	public boolean pollChanges(AbstractProject project, Launcher launcher,
			FilePath workspace, TaskListener listener) throws IOException,
			InterruptedException {
		return getProjectScm().pollChanges(project, launcher, workspace, listener);
	}

	@Extension
	public static class DescriptorImpl extends SCMDescriptor {

		public DescriptorImpl() {
			super(null);
		}

		@Override
		public String getDisplayName() {
			return "Use SCM from another project";
		}

		/**
		 * Form validation method.
		 */
		public FormValidation doCheckProjectName(@AncestorInPath AccessControlled anc, @QueryParameter String value) {
			// Require CONFIGURE permission on this project
			if (!anc.hasPermission(Item.CONFIGURE)) return FormValidation.ok();
			//this check is important because otherwise plugin will check for similar project which impacts performance
			//the check will be performed even if this plugin is not used as SCM for the current project
			if(StringUtils.isEmpty(value)) {
				return FormValidation.error("Project cannot be empty");
			}
			Item item = Hudson.getInstance().getItemByFullName(value, Item.class);
			if (item == null) {
				return FormValidation.error(Messages.BuildTrigger_NoSuchProject(value,
						AbstractProject.findNearest(value).getName()));
			}
			if (!(item instanceof AbstractProject)) {
				return FormValidation.error(Messages.BuildTrigger_NotBuildable(value));
			}
			return FormValidation.ok();
		}
	}

	// If a Parameter is used for projectName, some of these won't return anythign useful.
	// Because of it's nature `expand()`-ing the parameter is only useful at run time.

	@Override
	public RepositoryBrowser getBrowser() {
		return getProjectScm().getBrowser();
	}

	@Override
	public FilePath getModuleRoot(FilePath workspace) {
		return getProjectScm().getModuleRoot(workspace);
	}

	@Override
	public FilePath[] getModuleRoots(FilePath workspace) {
		return getProjectScm().getModuleRoots(workspace);
	}

	@Override
	public boolean processWorkspaceBeforeDeletion(
			AbstractProject<?, ?> project, FilePath workspace, Node node)
			throws IOException, InterruptedException {
		return getProjectScm().processWorkspaceBeforeDeletion(project, workspace, node);
	}

	@Override
	public boolean requiresWorkspaceForPolling() {
		return getProjectScm().requiresWorkspaceForPolling();
	}

	@Override
	public boolean supportsPolling() {
		// @TODO: worth adding check if expandedProjectName even exists?
		// If still $PROJECT, won't expand so nothing to poll.
		return getProjectScm().supportsPolling();
	}

	@Override
	public void buildEnvVars(AbstractBuild<?, ?> build, java.util.Map<String, String> env) {
		// Limitation : Currently only supports build variable for replacement.
		// Gets into infinite loop using `getEnvironment() since it loops
		// back to `getScm().buildEnvVars()`
		String pName = Util.replaceMacro(getProjectName(), build.getBuildVariables());
		getProjectScm().buildEnvVars(build, env);
	}

	@Override
	public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> paramAbstractBuild, Launcher paramLauncher,
			TaskListener paramTaskListener) throws IOException, InterruptedException {
		return getProjectScm(paramAbstractBuild).calcRevisionsFromBuild(paramAbstractBuild, paramLauncher, paramTaskListener);
	}


	@Override
	protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher,
			FilePath workspace, TaskListener listener, SCMRevisionState baseline)
			throws IOException, InterruptedException {
		return getProjectScm().poll(project, launcher, workspace, listener, baseline);
	}

}
