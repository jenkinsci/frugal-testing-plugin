package io.jenkins.plugins;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.json.JSONArray;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

/**
 * This file consists of the implementation and use of the FrugalCredentials class.
 * It also consists of the backend of the form at the add credentials stage.
 *
 * @author Jakshat Desai
 *
*/

public class FrugalCredentialsImpl extends UsernamePasswordCredentialsImpl implements FrugalCredentials {

    //Data from add credentials step form sent to this constructor
    @DataBoundConstructor
    public FrugalCredentialsImpl(CredentialsScope scope, String id, String description, String username, String password) {

        // Constructor overridden from FrugalCredentials class
        super(scope, id, description, username, password);
    }


    //Form validation at add credentials step
    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        //This function determines the name under which this plugin's credentials appear on the add credentials page
        @Override
        public String getDisplayName(){ return "Frugal Testing Credentials"; }

        //This function is called when the "Test Frugal Testing Credentials" button is pressed
        //It takes the username and password from the form as query input and validates them
        public FormValidation doTestLogin(
                @QueryParameter("username") final String username,
                @QueryParameter("password") final Secret password) throws IOException {

            FrugalFetchTestDetails fetch = new FrugalFetchTestDetails();
            JSONArray jArray = fetch.fetchAllTests(username,password);
            //If the json array returned is empty then credentials are incorrect
            if(jArray.isEmpty())return FormValidation.error("Login failed!");
            return FormValidation.ok("Successfully logged in!");
        }
    }
}

