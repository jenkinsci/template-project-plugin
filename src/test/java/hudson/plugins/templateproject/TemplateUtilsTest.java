package hudson.plugins.templateproject;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

/**
 * @author Oleksandr Horobets
 */
public class TemplateUtilsTest {

    public static final String EXISTING_PROJECT_NAME = "existing-project";
    public static final String NON_EXISTING_PROJECT_NAME = "non-existing-project";
    @Mock
    private Hudson hudsonMock;

    @Mock
    private AbstractProject projectMock;

    @Mock
    private AbstractBuild buildMock;

    private TemplateUtils templateUtils;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        templateUtils = new TemplateUtils(hudsonMock);
    }

    @Test
    public void getProjectThrowsExceptionIfProjectDoesNotExist(){
        doReturn(null).when(hudsonMock).getItemByFullName(NON_EXISTING_PROJECT_NAME);
        AbstractProject project = templateUtils.getProject(NON_EXISTING_PROJECT_NAME);
        assertThat(project).isNull();
    }

    @Test
    public void getProjectReturnsProjectByName(){
        doReturn(projectMock).when(hudsonMock).getItemByFullName(EXISTING_PROJECT_NAME);
        AbstractProject project = templateUtils.getProject(EXISTING_PROJECT_NAME);
        assertThat(project).isEqualTo(projectMock);
    }

    @Test
    public void getProjectWithBuildVarsThrowsExceptionIfProjectDoesNotExist(){
        doReturn(null).when(hudsonMock).getItemByFullName(NON_EXISTING_PROJECT_NAME);
        doReturn(new HashMap<String, String>()).when(buildMock).getBuildVariables();

        AbstractProject project = templateUtils.getProject(NON_EXISTING_PROJECT_NAME, buildMock);
        assertThat(project).isNull();
    }

    @Test
    public void getProjectWithBuildVarsReturnsProjectByName(){
        String projectName = "existing-project-$abc";
        String projectNameExpanded = "existing-project-hello";

        doReturn(projectMock).when(hudsonMock).getItemByFullName(projectNameExpanded);
        doReturn(new HashMap<String, String>(){{put("abc", "hello");}}).when(buildMock).getBuildVariables();

        AbstractProject project = templateUtils.getProject(projectName, buildMock);
        assertThat(project).isEqualTo(projectMock);
    }
}
