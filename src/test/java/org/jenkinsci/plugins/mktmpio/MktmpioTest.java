package org.jenkinsci.plugins.mktmpio;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class MktmpioTest extends MktmpioBaseTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setServer() {
        getConfig().setServer(mockedServer());
    }

    @Test
    public void failWithBadCredentials() throws Exception {
        getConfig().setToken("totally-bad-token");
        final Mktmpio mktmpio = new Mktmpio("redis");
        final FreeStyleProject project = j.jenkins.createProject(FreeStyleProject.class, "failingProject");
        project.getBuildWrappersList().add(mktmpio);
        prepareToRejectUnauthorized("totally-bad-token", "redis");
        FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();
        String s = FileUtils.readFileToString(build.getLogFile());
        assertThat(s, containsString("Error creating redis instance"));
        assertThat(s, containsString("Authentication required"));
        assertThat(s, not(containsString("MKTMPIO_HOST")));
        assertThat(s, not(containsString("mktmpio instance created")));
        assertThat(s, not(containsString("mktmpio instance shutdown")));
    }

    @Test
    public void succeedWithGoodCredentials() throws Exception {
        getConfig().setToken("totally-legit-token");
        final Mktmpio mktmpio = new Mktmpio("redis");
        final FreeStyleProject project = j.jenkins.createProject(FreeStyleProject.class, "basicProject");
        project.getBuildWrappersList().add(mktmpio);
        prepareFakeInstance("totally-legit-token", "redis");
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        String s = FileUtils.readFileToString(build.getLogFile());
        assertThat(s, containsString("MKTMPIO_HOST=12.34.56.78"));
        assertThat(s, containsString("MKTMPIO_PORT=54321"));
        assertThat(s, containsString("mktmpio instance created"));
        assertThat(s, containsString("mktmpio instance shutdown"));
    }

    private Mktmpio.DescriptorImpl getConfig() {
        return (Mktmpio.DescriptorImpl) j.jenkins.getDescriptor(Mktmpio.class);
    }
}