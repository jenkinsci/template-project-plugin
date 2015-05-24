package hudson.plugins.templateproject;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;

/**
 * A property that is only used to trigger the transient actions creations 
 * under a project.
 * 
 * @author william.bernardet@gmail.com
 *
 */
public class UpdateTransientProperty extends JobProperty<AbstractProject<?, ?>> {
	
	@DataBoundConstructor
	public UpdateTransientProperty() {
	}
	
	@Extension
	public static class DescriptorImpl extends JobPropertyDescriptor {

		@Override
		public String getDisplayName() {
			return "Property used to for job to update transient actions.";
		}

	}
	
}
