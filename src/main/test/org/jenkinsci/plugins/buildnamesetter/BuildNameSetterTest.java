package org.jenkinsci.plugins.buildnamesetter;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Shell;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class BuildNameSetterTest {
	@Rule
	public JenkinsRule jenkins = new JenkinsRule();

	@Test
	public void shouldExpand_BUILD_NUMBER_macro() throws InterruptedException, ExecutionException, IOException {
		FreeStyleProject fooProj = jenkins.createFreeStyleProject("foo");
		fooProj.getBuildersList().add(new BuildNameSetter("a_#${BUILD_NUMBER}"));

		FreeStyleBuild fooBuild = fooProj.scheduleBuild2(0).get();
		assertDisplayName(fooBuild, "a_#1", Result.SUCCESS);
	}

	@Test
	public void shouldExpand_JOB_NAME_full_env_macro() throws InterruptedException, ExecutionException, IOException {
		FreeStyleProject barProj = jenkins.createFreeStyleProject("bar");
		barProj.getBuildersList().add(new BuildNameSetter("b_${ENV,var=\"JOB_NAME\"}"));

		FreeStyleBuild barBuild = barProj.scheduleBuild2(0).get();
		assertDisplayName(barBuild, "b_bar", Result.SUCCESS);
	}

	@Bug(13347)
	@Test
	public void shouldExpand_JOB_NAME_macro() throws InterruptedException, ExecutionException, IOException {
		FreeStyleProject barProj = jenkins.createFreeStyleProject("bar");
		barProj.getBuildersList().add(new BuildNameSetter("c_${JOB_NAME}"));

		FreeStyleBuild barBuild = barProj.scheduleBuild2(0).get();
		assertDisplayName(barBuild, "c_bar", Result.SUCCESS);
	}

	@Bug(13347)
	@Test
	public void shouldExpand_JOB_NAME_macro_twice() throws InterruptedException, ExecutionException, IOException {
		FreeStyleProject barProj = jenkins.createFreeStyleProject("bar");
		barProj.getBuildersList().add(new BuildNameSetter("c_${JOB_NAME}_d_${JOB_NAME}"));

		FreeStyleBuild barBuild = barProj.scheduleBuild2(0).get();
		assertDisplayName(barBuild, "c_bar_d_bar", Result.SUCCESS);
	}

	@Bug(13347)
	@Test
	public void shouldExpand_JOB_NAME_macro_and_JOB_NAME_full_env_macro()
			throws InterruptedException, ExecutionException, IOException {
		FreeStyleProject fooProj = jenkins.createFreeStyleProject("foo");
		fooProj.getBuildersList().add(new BuildNameSetter("d_${NODE_NAME}_${ENV,var=\"JOB_NAME\"}"));

		FreeStyleBuild fooBuild = fooProj.scheduleBuild2(0).get();
		assertDisplayName(fooBuild, "d_master_foo", Result.SUCCESS);
	}

	@Test
	public void testPublisherSuccess() throws InterruptedException, ExecutionException, IOException {
		FreeStyleProject fooProj = jenkins.createFreeStyleProject("foo");

		fooProj.getPublishersList().add(new BuildNamePublisher("publisher-success", "publisher-fail", false));
		FreeStyleBuild fooBuild = fooProj.scheduleBuild2(0).get();
		assertDisplayName(fooBuild, "publisher-success", Result.SUCCESS);
	}

	@Test
	public void testPublisherFail() throws InterruptedException, ExecutionException, IOException {
		FreeStyleProject fooProj = jenkins.createFreeStyleProject("foo");

		fooProj.getBuildersList().add(new Shell("exit 1"));
		fooProj.getPublishersList().add(new BuildNamePublisher("publisher-success", "publisher-fail", false));
		FreeStyleBuild fooBuild = fooProj.scheduleBuild2(0).get();
		assertDisplayName(fooBuild, "publisher-fail", Result.FAILURE);
	}

	private void assertDisplayName(FreeStyleBuild build, String expectedName, Result expectedResult) {
		assertEquals(expectedResult, build.getResult());
		assertEquals(expectedName, build.getDisplayName());
	}
}