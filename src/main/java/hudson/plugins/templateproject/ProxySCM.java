package hudson.plugins.templateproject;

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
import hudson.util.FormFieldValidator;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class ProxySCM extends SCM {

	public static final SCMDescriptor<?> DESCRIPTOR = new DescriptorImpl();
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
	public SCMDescriptor<?> getDescriptor() {
		return DESCRIPTOR;
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
	
	public static class DescriptorImpl extends SCMDescriptor {

		protected DescriptorImpl() {
			super(null);
		}

		@Override
		public String getDisplayName() {
			return "Use SCM from another project";
		}
		
		/**
		 * Form validation method.
		 */
		public void doCheck(StaplerRequest req, StaplerResponse rsp)
				throws IOException, ServletException {
			// Require CONFIGURE permission on this project
			AccessControlled anc = req
					.findAncestorObject(AccessControlled.class);
			new FormFieldValidator(req, rsp, anc, Item.CONFIGURE) {
				protected void check() throws IOException, ServletException {
					String projectName = request.getParameter("value");

					Item item = Hudson.getInstance().getItemByFullName(
							projectName, Item.class);
					if (item == null) {
						error(Messages.BuildTrigger_NoSuchProject(projectName,
								AbstractProject.findNearest(projectName)
										.getName()));
						return;
					}
					if (!(item instanceof AbstractProject)) {
						error(Messages.BuildTrigger_NotBuildable(projectName));
						return;
					}
					ok();
				}
			}.process();
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
			AbstractProject<?, ?> project, FilePath workspace, Node node) {
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
