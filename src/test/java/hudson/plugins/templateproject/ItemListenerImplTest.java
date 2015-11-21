package hudson.plugins.templateproject;

import hudson.matrix.MatrixProject;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Project;
import hudson.util.DescribableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;

/**
 * @author Oleksandr Horobets
 */
public class ItemListenerImplTest {

    private static final String OLD_NAME = "old-name";
    private static final String NEW_NAME = "new-name";

    private ItemListenerImpl itemListener;

    @Mock
    private Hudson hudson;

    @Mock
    private Item item;

    @Mock
    private UpdateHandler updateHandler;

    @Mock
    private AbstractProject abstractProject1;

    @Mock
    private AbstractProject abstractProject2;

    @Mock
    private Project project1;

    @Mock
    private Project project2;

    @Mock
    private MatrixProject matrixProject1;

    @Mock
    private MatrixProject matrixProject2;

    @Mock
    private DescribableList list1;

    @Mock
    private DescribableList list2;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        itemListener = new ItemListenerImpl(hudson, updateHandler);
    }

    @Test
    public void onRenamedUpdatesScm(){
        doReturn(Arrays.asList(abstractProject1, abstractProject2)).when(hudson).getAllItems(AbstractProject.class);
        itemListener.onRenamed(item, OLD_NAME, NEW_NAME);

        Mockito.verify(updateHandler).updateScm(abstractProject1, OLD_NAME, NEW_NAME);
        Mockito.verify(updateHandler).updateScm(abstractProject2, OLD_NAME, NEW_NAME);
    }

    @Test
    public void onRenamedUpdatesProjectBuilders(){
        doReturn(Arrays.asList(project1, project2)).when(hudson).getAllItems(AbstractProject.class);
        doReturn(list1).when(project1).getBuildersList();
        doReturn(list2).when(project2).getBuildersList();

        itemListener.onRenamed(item, OLD_NAME, NEW_NAME);

        Mockito.verify(updateHandler).updateBuilders(list1, OLD_NAME, NEW_NAME);
        Mockito.verify(updateHandler).updateBuilders(list2, OLD_NAME, NEW_NAME);
    }

    @Test
    public void onRenamedUpdatesMatrixProjectBuilders(){
        doReturn(Arrays.asList(matrixProject1, matrixProject2)).when(hudson).getAllItems(AbstractProject.class);
        doReturn(list1).when(matrixProject1).getBuildersList();
        doReturn(list2).when(matrixProject2).getBuildersList();

        itemListener.onRenamed(item, OLD_NAME, NEW_NAME);

        Mockito.verify(updateHandler).updateBuilders(list1, OLD_NAME, NEW_NAME);
        Mockito.verify(updateHandler).updateBuilders(list2, OLD_NAME, NEW_NAME);
    }

    @Test
    public void onRenamedUpdatesPublishers(){
        doReturn(Arrays.asList(abstractProject1, abstractProject2)).when(hudson).getAllItems(AbstractProject.class);
        doReturn(list1).when(abstractProject1).getPublishersList();
        doReturn(list2).when(abstractProject2).getPublishersList();

        itemListener.onRenamed(item, OLD_NAME, NEW_NAME);

        Mockito.verify(updateHandler).updatePublishers(list1, OLD_NAME, NEW_NAME);
        Mockito.verify(updateHandler).updatePublishers(list2, OLD_NAME, NEW_NAME);
    }

    @Test
    public void onRenamedSavesConfigurationIfProjectBuilderChanged() throws IOException {
        doReturn(true).when(updateHandler).updateBuilders(list1, OLD_NAME, NEW_NAME);
        doReturn(list1).when(project1).getBuildersList();
        doReturn(Arrays.asList(project1)).when(hudson).getAllItems(AbstractProject.class);

        itemListener.onRenamed(item, OLD_NAME, NEW_NAME);

        Mockito.verify(project1).save();
    }

    @Test
    public void onRenamedSavesConfigurationIfMatrixProjectBuilderChanged() throws IOException {
        doReturn(true).when(updateHandler).updateBuilders(list1, OLD_NAME, NEW_NAME);
        doReturn(list1).when(matrixProject1).getBuildersList();
        doReturn(Arrays.asList(matrixProject1)).when(hudson).getAllItems(AbstractProject.class);

        itemListener.onRenamed(item, OLD_NAME, NEW_NAME);

        Mockito.verify(matrixProject1).save();
    }

    @Test
    public void onRenamedSavesConfigurationIfProjectPublishersChanged() throws IOException {
        doReturn(true).when(updateHandler).updatePublishers(list1, OLD_NAME, NEW_NAME);
        doReturn(list1).when(project1).getPublishersList();
        doReturn(Arrays.asList(project1)).when(hudson).getAllItems(AbstractProject.class);

        itemListener.onRenamed(item, OLD_NAME, NEW_NAME);

        Mockito.verify(project1).save();
    }

    @Test
    public void onRenamedNotSavesConfigurationIfNothingChanged() throws IOException {
        doReturn(Arrays.asList(abstractProject1)).when(hudson).getAllItems(AbstractProject.class);

        itemListener.onRenamed(item, OLD_NAME, NEW_NAME);

        Mockito.verify(matrixProject1, never()).save();
    }

    @Test
    public void onRenamedSavesConfigurationOnlyOnceIfMultipleItemsChanged() throws IOException {
        doReturn(true).when(updateHandler).updateBuilders(list1, OLD_NAME, NEW_NAME);
        doReturn(true).when(updateHandler).updatePublishers(list2, OLD_NAME, NEW_NAME);

        doReturn(list1).when(project1).getBuildersList();
        doReturn(list2).when(project1).getPublishersList();
        doReturn(Arrays.asList(project1)).when(hudson).getAllItems(AbstractProject.class);

        itemListener.onRenamed(item, OLD_NAME, NEW_NAME);

        Mockito.verify(project1).save();
    }
}
