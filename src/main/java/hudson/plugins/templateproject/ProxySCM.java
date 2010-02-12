package hudson.plugins.templateproject;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.security.AccessControlled;
import hudson.tasks.Messages;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class ProxySCM extends SCM {

	private final String projectName;

	@DataBoundConstructor
	public ProxySCM(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectName() {
		return projectName;
	}

	public AbstractProject<?, ?> getProject() {
		return (AbstractProject<?, ?>) Hudson.getInstance()
				.getItem(projectName);
	}

	@Override
	public boolean checkout(AbstractBuild build, Launcher launcher,
			FilePath workspace, BuildListener listener, File changelogFile)
			throws IOException, InterruptedException {
		return getProject().getScm().checkout(build, launcher, workspace, listener, changelogFile);
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		return getProject().getScm().createChangeLogParser();
	}

	@Override
	public boolean pollChanges(AbstractProject project, Launcher launcher,
			FilePath workspace, TaskListener listener) throws IOException,
			InterruptedException {
		return getProject().getScm().pollChanges(project, launcher, workspace, listener);
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
			Item item = Hudson.getInstance().getItemByFullName(
					value, Item.class);
			if (item == null) {
				return FormValidation.error(Messages.BuildTrigger_NoSuchProject(value,
						AbstractProject.findNearest(value)
								.getName()));
			}
			if (!(item instanceof AbstractProject)) {
				return FormValidation.error(Messages.BuildTrigger_NotBuildable(value));
			}
			return FormValidation.ok();
		}
	}

	@Override
	public void buildEnvVars(AbstractBuild build, Map<String, String> env) {
		getProject().getScm().buildEnvVars(build, env);
	}

	@Override
	public RepositoryBrowser getBrowser() {
		return getProject().getScm().getBrowser();
	}

	@Override
	public FilePath getModuleRoot(FilePath workspace) {
		return getProject().getScm().getModuleRoot(workspace);
	}

	@Override
	public FilePath[] getModuleRoots(FilePath workspace) {
		return getProject().getScm().getModuleRoots(workspace);
	}

	@Override
	public boolean processWorkspaceBeforeDeletion(
			AbstractProject<?, ?> project, FilePath workspace, Node node)
			throws IOException, InterruptedException {
		return getProject().getScm().processWorkspaceBeforeDeletion(project, workspace, node);
	}

	@Override
	public boolean requiresWorkspaceForPolling() {
		return getProject().getScm().requiresWorkspaceForPolling();
	}

	@Override
	public boolean supportsPolling() {
		return getProject().getScm().supportsPolling();
	}

}
