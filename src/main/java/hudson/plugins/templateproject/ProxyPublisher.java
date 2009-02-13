package hudson.plugins.templateproject;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.servlet.ServletException;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.security.AccessControlled;
import hudson.tasks.Messages;
import hudson.tasks.Publisher;
import hudson.util.FormFieldValidator;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class ProxyPublisher extends Publisher {

	private final String projectName;

	@DataBoundConstructor
	public ProxyPublisher(String projectName) {
		this.projectName = projectName;
	}

	public Descriptor<Publisher> getDescriptor() {
		return DESCRIPTOR;
	}

	public String getProjectName() {
		return projectName;
	}

	public AbstractProject<?, ?> getProject() {
		return (AbstractProject<?, ?>) Hudson.getInstance()
				.getItem(projectName);
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return false;
	}

	@Override
	public boolean prebuild(Build build, BuildListener listener) {
		for (Publisher publisher : getProject().getPublishersList().toList()) {
			if (!publisher.prebuild(build, listener)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		for (Publisher publisher : getProject().getPublishersList().toList()) {
			if (!publisher.perform(build, launcher, listener)) {
				return false;
			}
		}
		return true;
	}

	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	public static class DescriptorImpl extends Descriptor<Publisher> {

		@Override
		public String getDisplayName() {
			return "Use publishers from another project";
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

}
