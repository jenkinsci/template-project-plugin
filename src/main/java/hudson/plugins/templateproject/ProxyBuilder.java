package hudson.plugins.templateproject;

import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.DependecyDeclarer;
import hudson.model.DependencyGraph;
import hudson.model.TopLevelItem;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Project;
import hudson.security.AccessControlled;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Messages;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

	public Item getJob() {
		return Hudson.getInstance().getItemByFullName(getProjectName(), Item.class);
	}

	public List<Builder> getProjectBuilders() {
		AbstractProject p = (AbstractProject) Hudson.getInstance().getItemByFullName(projectName);
		if (p instanceof Project) return ((Project)p).getBuilders();
		else if (p instanceof MatrixProject) return ((MatrixProject)p).getBuilders();
		else return Collections.emptyList();
	}

	@Override
	public void buildDependencyGraph(AbstractProject project, DependencyGraph graph) {
		Item item = Hudson.getInstance().getItemByFullName(getProjectName());
		if (item instanceof Project) {
			Project<?, ?> templateProject = (Project)item;
			for (Builder builder : templateProject.getBuildersList().toList()) {
				if (builder instanceof DependecyDeclarer) {
					((DependecyDeclarer)builder).buildDependencyGraph(project, graph);
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
		for (Builder builder: getProjectBuilders()) {
			listener.getLogger().println("[TemplateProject] Starting builders from: '" + getProjectName() + "'");
			if (!builder.perform(build, launcher, listener)) {
				listener.getLogger().println("[TemplateProject] FAILED performing builders from: '" + getProjectName() + "'");
				return false;
			}
            String URLTemplateLink;
            listener.getLogger().println("Starting template job: " + getProjectName());
            if (Jenkins.getInstance().getRootUrl() == null){
                listener.getLogger().println("Could not get URL from configuration, change URL settings in \"Manage Jenkins->Configure System\"");
                listener.getLogger().println("If you set this, a link to the template job will be displayed instead");
            } else {
                URLTemplateLink = Jenkins.getInstance().getRootUrl() + "job/" + getProjectName();
                listener.getLogger().println(URLTemplateLink);
            }
            listener.getLogger().println("[TemplateProject] Successfully performed builders from: '" + getProjectName() + "'");
		}
		return true;
	}

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
		for (Builder builder: getProjectBuilders()) {
			if (!builder.prebuild(build, listener)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
		List<Action> actions = new ArrayList<Action>();
		for (Builder builder : getProjectBuilders()) {
			actions.addAll(builder.getProjectActions(project));
		}
		return actions;
	}
	
}
