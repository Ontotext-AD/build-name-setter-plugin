package org.jenkinsci.plugins.buildnamesetter;

import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.*;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.release.ReleaseWrapper;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

public class BuildNamePublisher extends Recorder implements
		MatrixAggregatable {

	public final String templateFailed;
	public final String template;
	public final boolean setForMatrix;

	@DataBoundConstructor
	public BuildNamePublisher(String template, String templateFailed,
			boolean setForMatrix) {
		this.templateFailed = templateFailed;
		this.template = template;
		this.setForMatrix = setForMatrix;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		boolean result;

		boolean useUnstable = (templateFailed != null)
				&& build.getResult().isWorseThan(Result.UNSTABLE);
		if (checkForOtherBuildNameSetters(build, listener)) {
			listener.getLogger().println("There are already build name setters, not apply this one.");
			result = true;
		} else {
			result = setDisplayName(build, listener, useUnstable ? templateFailed : template);
		}
		return result;
	}

	public boolean checkForOtherBuildNameSetters(AbstractBuild<?, ?> build, BuildListener listener) {
		ReleaseWrapper.ReleaseBuildBadgeAction releaseBuildBadgeAction =
				build.getAction(ReleaseWrapper.ReleaseBuildBadgeAction.class);
		return (releaseBuildBadgeAction != null) ? true : false;
	}

	private boolean setDisplayName(AbstractBuild build, BuildListener listener, String template)
			throws IOException, InterruptedException {
		boolean result = true;
		try {
			build.setDisplayName(TokenMacro.expandAll(build, listener, template));
		} catch (MacroEvaluationException e) {
			listener.getLogger().println(e.getMessage());
			result = false;
		}
		return result;
	}

	@Extension
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Publisher> {

		public DescriptorImpl() {
			super(BuildNamePublisher.class);
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
		public Publisher newInstance(StaplerRequest req, JSONObject formData)
				throws FormException {
			return req.bindJSON(BuildNamePublisher.class, formData);
		}

		public boolean isMatrixProject(AbstractProject project) {
			return project instanceof MatrixProject;
		}
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	public MatrixAggregator createAggregator(final MatrixBuild build,
			Launcher launcher, final BuildListener listener) {

		if (!isSetForMatrix()) {
			return null;
		}

		return new MatrixAggregator(build, launcher, listener) {
			@Override
			public boolean endRun(MatrixRun run) throws InterruptedException,
					IOException {
				if (build.getDisplayName() == null
						&& run.getDisplayName() != null) {
					build.setDescription(run.getDisplayName());
				} else if (build.getDisplayName() != null && run.getDisplayName() != null) {
					String oldDescr = build.getDisplayName();
					String newDescr = oldDescr + "<br />" + run.getDisplayName();
					build.setDisplayName(newDescr);
				}
				return true;
			}
		};
	}

	public boolean isSetForMatrix() {
		return setForMatrix;
	}

}
