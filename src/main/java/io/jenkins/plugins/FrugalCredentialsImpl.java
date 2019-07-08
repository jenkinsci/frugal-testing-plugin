package io.jenkins.plugins;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.squareup.okhttp.*;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * This file consists of the implementation and use of the FrugalCredentials class.
 * It also consists of the backend of the form at the add credentials stage.
 *
 * @author Jakshat Desai
 *
*/

class CheckingLogin
{
    public boolean isLoginSuccessful(String username, Secret password) throws IOException {
        String serverUrl = (new FrugalBuildStepDescriptor()).getDescriptor().getServerUrl();
        CookieManager ck = new CookieManager();
        CookieHandler.setDefault(ck);
        OkHttpClient client= new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        username = URLEncoder.encode(username, StandardCharsets.UTF_8.name());
        String pswd = URLEncoder.encode(password.getPlainText(), StandardCharsets.UTF_8.name());
        String loginContent = "username="+username+"&password="+pswd;
        RequestBody loginReq = RequestBody.create(mediaType,loginContent);

        //login
        Request request1 = new Request.Builder()
                .url(serverUrl+"/login")
                .post(loginReq)
                .build();
        client.newCall(request1).execute();

        //checking if login is valid
        Request request2 = new Request.Builder()
                .url(serverUrl +"/CheckUserStatus")
                .get()
                .build();
        Response response2 = client.newCall(request2).execute();

        //unsuccessful response implies login failed
        if(!response2.isSuccessful())
            return false;

        String userDetails = response2.body().string();
        //if the response isnt json then login failed
        try
        {
            new JSONObject(userDetails);
        }
        catch(Exception e)
        {
            return false;
        }

        //Empty response implies login failed
        if(userDetails.isEmpty())
            return false;
        return true;
    }
}

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
            if((new CheckingLogin()).isLoginSuccessful(username,password))
                return FormValidation.ok("Successfully logged in!");
            return FormValidation.error("Login failed!");
        }
    }
}

