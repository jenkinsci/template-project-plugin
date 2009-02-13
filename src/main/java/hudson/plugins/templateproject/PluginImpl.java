package hudson.plugins.templateproject;

import hudson.Plugin;
import hudson.scm.SCMS;
import hudson.tasks.BuildStep;
import hudson.tasks.Builder;

public class PluginImpl extends Plugin {

	public PluginImpl() {
		
		BuildStep.PUBLISHERS.add(ProxyPublisher.DESCRIPTOR);
		Builder.BUILDERS.add(ProxyBuilder.DESCRIPTOR);
		SCMS.SCMS.add(ProxySCM.DESCRIPTOR);
		
	}
}
