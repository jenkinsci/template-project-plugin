package hudson.plugins.templateproject;

import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Project;
import hudson.model.listeners.ItemListener;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
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

	private Hudson hudson;
	private UpdateHandler updateHandler;

	public ItemListenerImpl(){
		this(Hudson.getInstance(), UpdateHandler.getInstance());
	}

	@VisibleForTesting
	ItemListenerImpl(Hudson hudson, UpdateHandler updateHandler){
		this.hudson = hudson;
		this.updateHandler = updateHandler;
	}

	/**
	 * Let's force the projects using either the ProxyPublisher or the ProxyBuilder
	 * to update their transient actions.
	 */
	@Override
	public void onLoaded() {
		for (AbstractProject<?,?> project : Hudson.getInstance().getAllItems(AbstractProject.class)) {
			if (hasTemplateSteps(project)) {
				try {
					project.addProperty(new UpdateTransientProperty());
					project.removeProperty(UpdateTransientProperty.class);
				} catch (IOException e) {
					LOGGER.severe(e.getMessage());
				}
			}
		}
	}

	@Override
	public void onRenamed(Item item, String oldName, String newName) {
		for (AbstractProject project : hudson.getAllItems(AbstractProject.class)) {
			updateHandler.updateScm(project, oldName, newName);

			boolean updatedItems = false;

			if(project instanceof Project){
				if(updateHandler.updateBuilders(((Project) project).getBuildersList(), oldName, newName)){
					updatedItems = true;
				}
			} else if(project instanceof MatrixProject){
				if(updateHandler.updateBuilders(((MatrixProject) project).getBuildersList(), oldName, newName)){
					updatedItems = true;
				}
			}

			if(updateHandler.updatePublishers(project.getPublishersList(), oldName, newName)){
				updatedItems = true;
			}

			if(updatedItems) {
				try {
					project.save();
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, "[TemplateProject] Unable to save updated configuration", e);
				}
			}
		}
	}

	private boolean hasTemplateSteps(AbstractProject project) {
		return project.getPublishersList().get(ProxyPublisher.class) != null || hasBuilder(project, ProxyBuilder.class);
	}

	private List<Builder> getBuilders(AbstractProject<?, ?> project) {
		if (project instanceof Project) {
			return ((Project) project).getBuilders();
		} else if (project instanceof MatrixProject) {
			return ((MatrixProject) project).getBuilders();
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
