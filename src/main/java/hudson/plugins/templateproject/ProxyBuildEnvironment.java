package hudson.plugins.templateproject;

import hudson.Extension;
import hudson.Launcher;
import hudson.console.HyperlinkNote;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import jenkins.model.DependencyDeclarer;
import hudson.model.DependencyGraph;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Project;
import hudson.model.Run;
import hudson.security.AccessControlled;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Messages;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class ProxyBuildEnvironment extends BuildWrapper implements DependencyDeclarer {

	private final String projectName;

	@DataBoundConstructor
	public ProxyBuildEnvironment(String projectName) {
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

	public List<BuildWrapper> getProjectBuildWrappers(AbstractBuild<?, ?> build) {
		AbstractProject p = TemplateUtils.getProject(getProjectName(), build);

		if (p instanceof Project) {
			return ((Project) p).getBuildWrappersList();
		} else if (p instanceof MatrixProject) {
			return ((MatrixProject) p).getBuildWrappersList();
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public final void buildDependencyGraph(final AbstractProject project, final DependencyGraph graph) {
		final Item item = Hudson.getInstance().getItemByFullName(getProjectName());
		AbstractProject<?, ?> templateProject = (AbstractProject) Hudson.getInstance().getItem(getProjectName());
		if (item instanceof Project) {
			// @TODO : see how important it is that this gets expanded projectName
			for (BuildWrapper wrapper : getProjectBuildWrappers(null)) {
				if (wrapper instanceof DependencyDeclarer) {
					((DependencyDeclarer) wrapper).buildDependencyGraph(project, graph);
				}
			}
		}
	}

	@Override
	public Environment setUp(@SuppressWarnings("rawtypes") AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException,
					InterruptedException {

		AbstractProject p = TemplateUtils.getProject(getProjectName(), build);
		listener.getLogger().println("[TemplateProject] Getting environment from: " + HyperlinkNote.encodeTo('/'+ p.getUrl(), p.getFullDisplayName()));
		final ArrayList<Environment> envs = new ArrayList<Environment>();
		for (BuildWrapper builder : getProjectBuildWrappers(build)) {
			envs.add(builder.setUp(build, launcher, listener));
		}
		listener.getLogger().println("[TemplateProject] Successfully setup environment from: '" + p.getFullDisplayName() + "'");

		return new Environment() {
			@Override
			public boolean tearDown(@SuppressWarnings("rawtypes") AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
				boolean operationSucceeded = true;
				for (Environment env : envs) {
					operationSucceeded &= env.tearDown(build, listener);
				}
				return operationSucceeded;
			}
		};
	}

	@Override
	public void preCheckout(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		AbstractProject p = TemplateUtils.getProject(getProjectName(), build);
		listener.getLogger().println("[TemplateProject] Starting pre-checkout from: " + HyperlinkNote.encodeTo('/'+ p.getUrl(), p.getFullDisplayName()));
		for (BuildWrapper builder : getProjectBuildWrappers(build)) {
			builder.preCheckout(build, launcher, listener);
		}
		listener.getLogger().println("[TemplateProject] Successfully performed pre-checkout from: '" + p.getFullDisplayName() + "'");
	}

	@Override
	public Launcher decorateLauncher(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, Run.RunnerAbortedException {
		for (BuildWrapper builder : getProjectBuildWrappers(build)) {
			launcher = builder.decorateLauncher(build, launcher, listener);
		}
		return launcher;
	}

	@Override
	public OutputStream decorateLogger(AbstractBuild build, OutputStream logger) throws IOException, InterruptedException, Run.RunnerAbortedException {
		for (BuildWrapper builder : getProjectBuildWrappers(build)) {
			logger = builder.decorateLogger(build, logger);
		}
		return logger;
	}

	@Extension
	public static class DescriptorImpl extends BuildWrapperDescriptor {

		@Override
		public String getDisplayName() {
			return "Use build environment from another project";
		}

		@Override
		public boolean isApplicable(AbstractProject<?, ?> jobType) {
			return true;
		}

		/**
		 * Form validation method.
		 */
		public FormValidation doCheckProjectName(@AncestorInPath AccessControlled anc, @QueryParameter String value) {
			// Require CONFIGURE permission on this project
			if (!anc.hasPermission(Item.CONFIGURE)) {
				return FormValidation.ok();
			}
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
	public Collection<? extends Action> getProjectActions(AbstractProject project) {
		List<Action> actions = new ArrayList<Action>();
                // @TODO : see how important it is that this gets expanded projectName
		for (BuildWrapper wrapper : getProjectBuildWrappers(null)) {
			actions.addAll(wrapper.getProjectActions(project));
		}
		return actions;
	}

}
