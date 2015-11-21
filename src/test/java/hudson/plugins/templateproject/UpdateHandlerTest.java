package hudson.plugins.templateproject;

import hudson.matrix.MatrixProject;
import hudson.model.AbstractProject;
import hudson.model.Project;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.util.DescribableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;

/**
 * @author Oleksandr Horobets
 */
public class UpdateHandlerTest {
    private static final String OLD_NAME = "old-name";
    private static final String NEW_NAME = "new-name";

    private UpdateHandler updateHandler;

    @Mock
    private AbstractProject abstractProject;

    @Mock
    private Project project;

    @Mock
    private MatrixProject matrixProject;

    @Mock
    private DescribableList describableList;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        updateHandler = new UpdateHandler();
    }

    @Test
    public void updateScmUpdatingWhenScmIsFromRenamedProject() throws IOException {
        doReturn(new ProxySCM(OLD_NAME)).when(abstractProject).getScm();
        updateHandler.updateScm(abstractProject, OLD_NAME, NEW_NAME);

        ArgumentCaptor<ProxySCM> argumentCaptor = ArgumentCaptor.forClass(ProxySCM.class);
        Mockito.verify(abstractProject).setScm(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getProjectName()).isEqualTo(NEW_NAME);
    }

    @Test
    public void updateScmNotUpdatingWhenScmIsNull() throws IOException {
        doReturn(null).when(abstractProject).getScm();
        updateHandler.updateScm(abstractProject, OLD_NAME, NEW_NAME);

        Mockito.verify(abstractProject, never()).setScm(any(SCM.class));
    }

    @Test
    public void updateScmNotUpdatingWhenScmIsNotProxyScm() throws IOException {
        doReturn(new NullSCM()).when(abstractProject).getScm();
        updateHandler.updateScm(abstractProject, OLD_NAME, NEW_NAME);

        Mockito.verify(abstractProject, never()).setScm(any(SCM.class));
    }

    @Test
    public void updateScmNotUpdatingWhenScmIsFromAnotherProject() throws IOException {
        doReturn(new ProxySCM("another-name")).when(abstractProject).getScm();
        updateHandler.updateScm(abstractProject, OLD_NAME, NEW_NAME);

        Mockito.verify(abstractProject, never()).setScm(any(SCM.class));
    }

    @Test
    public void updateBuildersUpdatingWhenContainsReferencedItem() throws IOException {
        ProxyBuilder oldProxyBuilder = new ProxyBuilder(OLD_NAME);
        doReturn(true).when(describableList).contains(oldProxyBuilder);

        boolean updated = updateHandler.updateBuilders(describableList, OLD_NAME, NEW_NAME);

        ArgumentCaptor<ProxyBuilder> argumentCaptor = ArgumentCaptor.forClass(ProxyBuilder.class);
        Mockito.verify(describableList).replace(argumentCaptor.capture(), argumentCaptor.capture());
        assertThat(argumentCaptor.getAllValues()).containsSequence(oldProxyBuilder, new ProxyBuilder(NEW_NAME));
        assertThat(updated).isTrue();
    }

    @Test
    public void updateBuildersNotUpdatingWhenNotContainReferencedItem() throws IOException {
        doReturn(false).when(describableList).contains(any());
        boolean updated = updateHandler.updateBuilders(describableList, OLD_NAME, NEW_NAME);

        Mockito.verify(describableList, never()).replace(any(), any());
        assertThat(updated).isFalse();
    }

    @Test
    public void updatePublishersUpdatingWhenContainsReferencedItem() throws IOException {
        ProxyPublisher oldProxyPublisher = new ProxyPublisher(OLD_NAME);
        doReturn(true).when(describableList).contains(oldProxyPublisher);

        boolean updated = updateHandler.updatePublishers(describableList, OLD_NAME, NEW_NAME);

        ArgumentCaptor<ProxyPublisher> argumentCaptor = ArgumentCaptor.forClass(ProxyPublisher.class);
        Mockito.verify(describableList).replace(argumentCaptor.capture(), argumentCaptor.capture());
        assertThat(argumentCaptor.getAllValues()).containsSequence(oldProxyPublisher, new ProxyPublisher(NEW_NAME));
        assertThat(updated).isTrue();
    }

    @Test
    public void updatePublishersNotUpdatingWhenNotContainReferencedItem() throws IOException {
        doReturn(false).when(describableList).contains(any());
        boolean updated = updateHandler.updatePublishers(describableList, OLD_NAME, NEW_NAME);

        Mockito.verify(describableList, never()).replace(any(), any());
        assertThat(updated).isFalse();
    }
}
