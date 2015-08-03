package org.jenkinsci.plugins.buildnamesetter;

import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

public class BuildNameSetter extends Builder {

	public final String template;

	@DataBoundConstructor
	public BuildNameSetter(String template) {
		this.template = template;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		return setDisplayName(build, listener);
	}

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
		boolean result;
		try {
			result = setDisplayName(build, listener);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
		}
		return result;
	}

	private boolean setDisplayName(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
		boolean result;
		try {
			build.setDisplayName(TokenMacro.expandAll(build, listener, template));
			result = true;
		} catch (MacroEvaluationException e) {
			listener.getLogger().println(e.getMessage());
			result = false;
		}
		return result;
	}

	public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
		return new MatrixAggregator(build, launcher, listener) {
			@Override
			public boolean startBuild() throws InterruptedException, IOException {
				setDisplayName(build, listener);
				return super.startBuild();
			}

			@Override
			public boolean endBuild() throws InterruptedException, IOException {
				setDisplayName(build, listener);
				return super.endBuild();
			}
		};
	}

	@Extension
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Builder> {

		public DescriptorImpl() {
			super(BuildNameSetter.class);
		}

		@Override
		public String getDisplayName() {
			return "Set Build Name";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public Builder newInstance(StaplerRequest req, JSONObject formData)
				throws FormException {
			return req.bindJSON(BuildNameSetter.class, formData);
		}
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}
}
