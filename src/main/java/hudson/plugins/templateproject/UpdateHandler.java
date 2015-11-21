package hudson.plugins.templateproject;

import hudson.model.AbstractProject;
import hudson.scm.SCM;
import hudson.util.DescribableList;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Oleksandr Horobets
 */
public class UpdateHandler {
    private static final Logger LOGGER = Logger.getLogger(UpdateHandler.class.getName());
    private static UpdateHandler instance;

    public void updateScm(@Nonnull AbstractProject project, @Nonnull String oldName, @Nonnull String newName) {
        SCM scm = project.getScm();

        if (scm instanceof ProxySCM) {
            ProxySCM proxySCM = (ProxySCM) scm;

            if (oldName.equals(proxySCM.getProjectName())) {
                try {
                    project.setScm(new ProxySCM(newName));
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "[TemplateProject] Unable to update SCM template reference", e);
                }
            }
        }
    }

    public boolean updateBuilders(@Nonnull DescribableList items, @Nonnull String oldName, @Nonnull String newName) {
        ProxyBuilder oldProxyBuilder = new ProxyBuilder(oldName);

        if (items.contains(oldProxyBuilder)){
            try {
                items.replace(oldProxyBuilder, new ProxyBuilder(newName));
                return true;
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "[TemplateProject] Unable to update builder template reference", e);
            }
        }

        return false;
    }

    public boolean updatePublishers(@Nonnull DescribableList items, @Nonnull String oldName, @Nonnull String newName) {
        ProxyPublisher oldProxyPublisher = new ProxyPublisher(oldName);

        if(items.contains(oldProxyPublisher)) {
            try {
                items.replace(oldProxyPublisher, new ProxyPublisher(newName));
                return true;
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "[TemplateProject] Unable to update publisher template reference", e);
            }
        }

        return false;
    }

    public static UpdateHandler getInstance() {
        if (instance == null) {
            instance = new UpdateHandler();
        }

        return instance;
    }
}
