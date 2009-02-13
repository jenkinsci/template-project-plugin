package hudson.plugins.templateproject;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Project;
import hudson.security.AccessControlled;
import hudson.tasks.Builder;
import hudson.tasks.Messages;
import hudson.util.FormFieldValidator;

import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class ProxyBuilder extends Builder {

	private final String projectName;

	@DataBoundConstructor
	public ProxyBuilder(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectName() {
		return projectName;
	}

	public Project<?, ?> getProject() {
		return (Project<?, ?>) Hudson.getInstance()
				.getItem(projectName);
	}

	public static Descriptor<Builder> DESCRIPTOR = new DescriptorImpl();

	public Descriptor<Builder> getDescriptor() {
		return DESCRIPTOR;
	}
	
	public static class DescriptorImpl extends Descriptor<Builder> {

		@Override
		public String getDisplayName() {
			return "Use builders from another project";
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
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		for (Builder builder: getProject().getBuilders()) {
			if (!builder.perform(build, launcher, listener)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
		for (Builder builder: getProject().getBuilders()) {
			if (!builder.prebuild(build, listener)) {
				return false;
			}
		}
		return true;
	}

}
