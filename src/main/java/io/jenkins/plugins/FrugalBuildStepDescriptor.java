package io.jenkins.plugins;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;

import static io.jenkins.plugins.FrugalServerDetails.SERVERURL;

/**
 *
 * Build Step Descriptor: This file takes input from the user for the "Frugal Testing" Build Step option.
 * It forwards those inputs to the builder then on.
 *
 * @author Jakshat Desai
 *
*/

@Extension
@Symbol("frugalTesting")
public class FrugalBuildStepDescriptor extends BuildStepDescriptor<Builder> {

    private ArrayList<FrugalCredentials> allCreds;//Array to store list of all stored credentials
    private FrugalCredentialsOps credOps;//credOps object needed for performing operations requiring credentials
    private String serverUrl = SERVERURL;//Initial value of server url is default

    public FrugalBuildStepDescriptor()
    {
        //Overriding the FrugalBuildStep Constructor so as to obtain data from the build step page form
        super(FrugalBuildStep.class);
        load();
        credOps = new FrugalCredentialsOps();
    }

    public FrugalBuildStepDescriptor(String serverUrl)
    {
        //Overriding the FrugalBuildStep Constructor so as to obtain data from the config form as well as build step page form
        super(FrugalBuildStep.class);
        load();
        credOps = new FrugalCredentialsOps();
        this.serverUrl = serverUrl;
    }

    //setters and getters
    public FrugalBuildStepDescriptor getDescriptor() {
        return this;
    }

    public String getServerUrl()
    {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl)
    {
        this.serverUrl = serverUrl;
    }

    //Obtaining the value of server url from config page
    @Override
    public boolean configure(StaplerRequest req, net.sf.json.JSONObject formData) throws FormException {
        String serverUrl = formData.optString("serverUrl");
        this.serverUrl = serverUrl.isEmpty() ? SERVERURL : serverUrl;
        this.save();
        return true;
    }

    //This function is responsible for fetching the contents appearing in the "Username" dropdown
    public ListBoxModel doFillUserIdItems() {
        ListBoxModel items = new ListBoxModel();
        items.add("Choose username","");//adding a dummy (placeholder)value to the dropdown
        if(!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER))
            return items;
        allCreds = credOps.getAllCredentials();//getting all the credentials using
        //Adding all the credentials to the dropdown
        for (final FrugalCredentials c : allCreds) {
                items.add(c.getUsername(),c.getId());
            }
        return items;
    }

    //This function is responsible for fetching the contents appearing in the "Test Name" dropdown
    //It is called every time there is a change in the QueryParameter userId
    public ListBoxModel doFillTestIdItems(@QueryParameter("userId") final String userId) throws IOException {
        ListBoxModel items = new ListBoxModel();
        items.add("Choose test","");
        if(userId.equals(""))return items;
        FrugalCredentials c = credOps.getCredentials(userId);//Fetching the credentials with the required userId

        //obtaining login details
        String username = c.getUsername();
        Secret password = c.getPassword();

        //Fetching test data for the required user
        FrugalFetchTestDetails fetch = new FrugalFetchTestDetails();
        JSONArray jArray = fetch.fetchAllTests(username,password);

        //checking if the user's account exists
        if(jArray.isEmpty())return items;
        //checking if the user has any tests
        if(!jArray.getJSONObject(0).has("testName"))return items;

        //Adding all the fetched data to the list
        for (int i = 0; i < jArray.length(); i++) {
            JSONObject object = jArray.getJSONObject(i);
            items.add(object.getString("testName"), "" + object.getInt("testID"));
        }
        return items;
    }

    //Checking for validity of various fields in build step:
    public FormValidation doCheckUserId(@QueryParameter String userId)
    {
        if(userId.equals(""))return FormValidation.error("Please choose a valid username");
        return FormValidation.ok();
    }

    public FormValidation doCheckTestId(@QueryParameter String testId)
    {
        if(testId.equals(""))return FormValidation.error("Please choose a valid test");
        return FormValidation.ok();
    }

    public FormValidation doCheckRunTag(@QueryParameter String runTag)
    {
        if(runTag.equals(""))return FormValidation.error("Please choose a valid run tag");
        return FormValidation.ok();
    }

    //This function determines which kind of projects can use this plugin
    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        return true;
    }

    //This function determines the name by which the plugin is represented in the build step menu
    @Override
    public String getDisplayName()
    {
        return "Frugal Testing";
    }

}
