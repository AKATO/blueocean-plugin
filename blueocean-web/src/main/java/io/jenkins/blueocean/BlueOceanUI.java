package io.jenkins.blueocean;

import hudson.ExtensionList;
import hudson.model.UsageStatistics;
import io.jenkins.blueocean.jsextensions.JenkinsJSExtensions;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.verb.GET;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Root of Blue Ocean UI
 *
 * @author Kohsuke Kawaguchi
 */
public class BlueOceanUI {
    private final String urlBase;

    public BlueOceanUI(String rootPath) {
        this.urlBase = rootPath;
    }

    /**
     * Exposes {@link RootRoutable}s to the URL space. Returns <code>this</code> if none found, allowing the UI to
     * resolve routes. This also has the side effect that we won't be able to generate 404s for any URL that *might*
     * resolve to a valid UI route. If and when we implement server-side rendering of initial state or to solidify the
     * routes on the back-end for real 404s, we'll need to complicate this behaviour :D
     */
    public Object getDynamic(String route) {
        for (RootRoutable r : ExtensionList.lookup(RootRoutable.class)) {
            if (r.getUrlName().equals(route))
                return r;
        }
        return this;
    }

    /**
     * The base of all BlueOcean URLs (underneath wherever Jenkins itself is deployed).
     */
    public String getUrlBase() {
        return urlBase;
    }

    // TODO: Look into using new Stapler stuff for doing this.
    @Restricted(DoNotUse.class)
    @GET
    public HttpResponse doJavaScriptExtensionInfo() {
        return new JsonResponse(JenkinsJSExtensions.INSTANCE.getJenkinsJSExtensionData());
    }

    /**
     * @return True if jenkins reports usage statistics.
     */
    public boolean includeRollbar() {
        return Jenkins.getInstance().isUsageStatisticsCollected() && !UsageStatistics.DISABLED;
    }

    /**
     * @return Version on the plugin e.g 1.0-SNAPSHOT (private-f9a14d3e-jenkins)
     */
    public String getPluginVersion() {
        return Jenkins.getInstance().getPlugin("blueocean-web").getWrapper().getVersion();
    }

    private class JsonResponse implements HttpResponse {

        private final byte[] data;

        public JsonResponse(byte[] data) {
            this.data = data;
        }

        @Override
        public void generateResponse(StaplerRequest staplerRequest, StaplerResponse staplerResponse, Object o) throws IOException, ServletException {
            staplerResponse.setContentType("application/json; charset=UTF-8");
            staplerResponse.getOutputStream().write(data);
        }
    }
}
