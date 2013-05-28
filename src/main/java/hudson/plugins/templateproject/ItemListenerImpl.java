package hudson.plugins.templateproject;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import hudson.matrix.MatrixProject;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.listeners.ItemListener;
import hudson.tasks.Builder;
import java.util.logging.Logger;

/**
 * This ItemListener implementation will force job using 
 * template for publisher to regenerate the transient actions list.  
 * 
 * To do so we use the {@link UpdateTransientProperty} property which does not do 
 * anything. This is a bit a hack that relies on the behavior, but
 * there does not seem to be any better way to force updating projects transients actions.
 * 
 * @author william.bernardet@gmail.com
 *
 */
@Extension
public class ItemListenerImpl extends ItemListener {
	private static final Logger LOGGER = Logger.getLogger(ItemListenerImpl.class.getName());

	/**
	 * Let's force the projects using either the ProxyPublisher or the ProxyBuilder
	 * to update their transient actions.
	 */
	@Override
	public void onLoaded() {
		for (AbstractProject<?,?> project : Hudson.getInstance().getAllItems(AbstractProject.class)) {
			if (project.getPublishersList().get(ProxyPublisher.class) != null ||
					hasBuilder(project, ProxyBuilder.class)) {
				try {
					project.addProperty(new UpdateTransientProperty());
					project.removeProperty(UpdateTransientProperty.class);
				} catch (IOException e) {
					LOGGER.severe(e.getMessage());
				}
			}
		}
	}

	private List<Builder> getBuilders(AbstractProject<?, ?> project) {
		if (project instanceof Project) {
			return ((Project)project).getBuilders();
		} else if (project instanceof MatrixProject) {
			return ((MatrixProject)project).getBuilders();
		} else {
			return Collections.emptyList();
		}
	}
	
	public <T> boolean hasBuilder(AbstractProject<?, ?> project, Class<T> type) {
		for (Builder b : getBuilders(project)) {
			if (type.isInstance(b)) {
				return true;
			}
		}
		return false;
	}	
}
