package io.jenkins.plugins;

import hudson.Extension;
import hudson.model.Action;

import javax.annotation.CheckForNull;

/**
 *
 *This action appears during execution of build step, once test has been started
 *
 * @author Jakshat Desai
 *
*/

@Extension
public class FrugalAction implements Action {

    private String reportUrl;
    private String displayName;
    private String iconFileName;

    public void setReportUrl(String reportUrl)
    {
        this.reportUrl = reportUrl;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public void setIconFileName(String iconFileName)
    {
        this.iconFileName = iconFileName;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return iconFileName;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return displayName;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return reportUrl;
    }
}
