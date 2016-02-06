package hudson.plugins.templateproject;

import hudson.Extension;
import hudson.Launcher;
import hudson.console.HyperlinkNote;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.DependecyDeclarer;
import hudson.model.DependencyGraph;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Project;
import hudson.security.AccessControlled;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Messages;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class ProxyBuilder extends Builder implements DependecyDeclarer {

	private final String projectName;

	@DataBoundConstructor
	public ProxyBuilder(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getExpandedProjectName(AbstractBuild<?, ?> build) {
		return TemplateUtils.getExpandedProjectName(projectName, build);
	}

	public AbstractProject<?, ?> getProject() {
		return TemplateUtils.getProject(projectName, null);
	}

	public List<Builder> getProjectBuilders(AbstractBuild<?, ?> build) {
		AbstractProject p = TemplateUtils.getProject(getProjectName(), build);

		if (p instanceof Project) return ((Project)p).getBuilders();
		else if (p instanceof MatrixProject) return ((MatrixProject)p).getBuilders();
		else return Collections.emptyList();
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
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		for (Builder builder: getProjectBuilders(build)) {
			AbstractProject p = TemplateUtils.getProject(getProjectName(), build);
			listener.getLogger().println("[TemplateProject] Starting builders from: " + HyperlinkNote.encodeTo('/'+ p.getUrl(), p.getFullDisplayName()));
			if (!builder.perform(build, launcher, listener)) {
				listener.getLogger().println("[TemplateProject] FAILED performing builders from: '" + p.getFullDisplayName() + "'");
				return false;
			}
			listener.getLogger().println("[TemplateProject] Successfully performed builders from: '" + p.getFullDisplayName() + "'");
		}
		return true;
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
		for (Builder builder : getProjectBuilders(null)) {
			actions.addAll(builder.getProjectActions(project));
		}
		return actions;
	}
	
}
