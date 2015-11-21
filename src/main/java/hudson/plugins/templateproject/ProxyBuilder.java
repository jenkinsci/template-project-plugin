package hudson.plugins.templateproject;

import hudson.Extension;
import hudson.Launcher;
import hudson.console.HyperlinkNote;
import hudson.matrix.MatrixProject;
import hudson.model.*;
import hudson.security.AccessControlled;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Messages;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ProxyBuilder extends Builder implements DependecyDeclarer {

	private final String projectName;

	@DataBoundConstructor
	public ProxyBuilder(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectName() {
		return projectName;
	}

	public List<Builder> getProjectBuilders(AbstractBuild<?, ?> build) {
		return getBuildersFrom(TemplateUtils.getInstance().getProject(getProjectName(), build));
	}

	public List<Builder> getProjectBuilders() {
		return getBuildersFrom(TemplateUtils.getInstance().getProject(getProjectName()));
	}

	private List<Builder> getBuildersFrom(AbstractProject project) {
		if (project instanceof Project) {
			return ((Project) project).getBuilders();
		} else if (project instanceof MatrixProject) {
			return ((MatrixProject) project).getBuilders();
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public void buildDependencyGraph(AbstractProject project, DependencyGraph graph) {
		AbstractProject<?, ?> templateProject = (AbstractProject) Hudson.getInstance().getItem(getProjectName());
		if (templateProject != null) {
			for (Publisher publisher : templateProject.getPublishersList().toList()) {
				if (publisher instanceof DependecyDeclarer) {
					((DependecyDeclarer)publisher).buildDependencyGraph(project, graph);
				}
			}
		}
	}

	
	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

		@Override
		public String getDisplayName() {
			return "Use builders from another project";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
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
			if (!(item instanceof Project) && !(item instanceof MatrixProject)) {
				return FormValidation.error(Messages.BuildTrigger_NotBuildable(value));
			}
			return FormValidation.ok();
		}
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		AbstractProject project = TemplateUtils.getInstance().getProject(getProjectName(), build);

		if(project != null) {
			for (Builder builder: getProjectBuilders(build)) {
				listener.getLogger().println("[TemplateProject] Starting builders from: " + HyperlinkNote.encodeTo('/' + project.getUrl(), project.getFullDisplayName()));
				if (!builder.perform(build, launcher, listener)) {
					listener.getLogger().println("[TemplateProject] FAILED performing builders from: '" + project.getFullDisplayName() + "'");
					return false;
				}
				listener.getLogger().println("[TemplateProject] Successfully performed builders from: '" + project.getFullDisplayName() + "'");
			}

			return true;

		} else {
			listener.getLogger().printf("[TemplateProject] FAILED builders loading, couldn't find project: '%s' %n", getProjectName());
			return false;
		}
	}

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
		for (Builder builder: getProjectBuilders(build)) {
			if (!builder.prebuild(build, listener)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
		List<Action> actions = new ArrayList<Action>();
		// @TODO : see how important it is that this gets expanded projectName
		for (Builder builder : getProjectBuilders()) {
			actions.addAll(builder.getProjectActions(project));
		}
		return actions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ProxyBuilder that = (ProxyBuilder) o;

		return !(projectName != null ? !projectName.equals(that.projectName) : that.projectName != null);

	}

	@Override
	public int hashCode() {
		return projectName != null ? projectName.hashCode() : 0;
	}
}
