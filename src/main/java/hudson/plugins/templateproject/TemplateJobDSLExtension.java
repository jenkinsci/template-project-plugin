package hudson.plugins.templateproject;

import hudson.Extension;
import javaposse.jobdsl.dsl.RequiresPlugin;
import javaposse.jobdsl.dsl.helpers.ScmContext;
import javaposse.jobdsl.dsl.helpers.publisher.PublisherContext;
import javaposse.jobdsl.plugin.DslExtensionMethod;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.dsl.helpers.wrapper.WrapperContext;

import javaposse.jobdsl.plugin.ContextExtensionPoint;

@Extension(optional = true)
public class TemplateJobDSLExtension extends ContextExtensionPoint  {
    
    @RequiresPlugin(id = "template-project")
    @DslExtensionMethod(context = StepContext.class)
    public Object templateBuilder(String jobName) {
        return new ProxyBuilder(jobName);
    }
    
    @RequiresPlugin(id = "template-project")
    @DslExtensionMethod(context = PublisherContext.class)    
    public Object templatePublisher(String jobName){
        return new ProxyPublisher(jobName);
    }
    
    @RequiresPlugin(id = "template-project")
    @DslExtensionMethod(context = WrapperContext.class)    
    public Object templateBuildEnv(String jobName){
        return new ProxyBuildEnvironment(jobName);
    }    
    
    @RequiresPlugin(id = "template-project")
    @DslExtensionMethod(context = ScmContext.class)    
    public Object templateScm(String jobName){
        return new ProxySCM(jobName);
    }
    
}
