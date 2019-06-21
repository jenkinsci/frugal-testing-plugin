package io.jenkins.plugins;

import com.squareup.okhttp.*;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static io.jenkins.plugins.FrugalServerDetails.ICONPATH;
import static io.jenkins.plugins.FrugalServerDetails.SERVERURL;

/**
 *
 * The operations taking place at build time are coded here
 *
 * @author Jakshat Desai
 *
 */

public class FrugalBuildStep extends Builder implements SimpleBuildStep {
    private String userId;//Stores ID given to a particular credential by jenkins
    private String testId;//ID of test the user wants to execute
    private String runTag;//Run name chosen by user
    private boolean getJtl;
    public String serverUrl = SERVERURL;
    private FrugalCredentialsOps credOps;//credOps object needed for performing operations requiring credentials

    //Data fetched from form at build step configuration are sent to this constructor as input
    @DataBoundConstructor
    public FrugalBuildStep(String userId, String testId, String runTag, boolean getJtl)
    {
        this.userId = userId;
        this.runTag = runTag.replaceAll(" ","%20");
        this.testId = testId;
        this.getJtl = getJtl;
        credOps = new FrugalCredentialsOps();
    }

    //Getters for member variables

    public String getUserId() { return userId; }

    public String getTestId() { return testId; }

    public String getRunTag() { return runTag; }

    public boolean isGetJtl() {return getJtl;}

    public String getServerUrl() { return serverUrl; }

    @DataBoundSetter
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }

    //Operations taking place at Build time are performed within the perform() function
    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        BuildListener listener = (BuildListener) launcher.getListener();
        //getting server details
        String serverUrl = (new FrugalBuildStepDescriptor()).getDescriptor().getServerUrl();
        //Getting login details
        FrugalCredentials c = credOps.getCredentials(getUserId());
        if(c==null)
        {
            listener.getLogger().println("Credentials not found");
            run.setResult(Result.FAILURE);
            return;
        }
        String username = c.getUsername();
        String password = c.getPassword().getPlainText();

        //Setting up cookie manager
        CookieManager ck = new CookieManager();
        CookieHandler.setDefault(ck);

        //Setting up HTTP client
        OkHttpClient client= new OkHttpClient();
        //Setting up socket connection timeout as 5 minutes
        client.setConnectTimeout(5, TimeUnit.MINUTES);
        client.setReadTimeout(5, TimeUnit.MINUTES);
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        String loginContent = "username="+username+"&password="+password;
        RequestBody loginReq = RequestBody.create(mediaType,loginContent);
        RequestBody req = RequestBody.create(mediaType, "");

        //Sending login request
        Request request1 = new Request.Builder()
                .url(serverUrl+"/login")
                .post(loginReq)
                .build();
        Response response1 = client.newCall(request1).execute();
        if(!response1.isSuccessful())
        {
            listener.getLogger().println("Login failed");
            run.setResult(Result.FAILURE);
            return;
        }
        listener.getLogger().println("Logged in");

        //Creating a test run and obtaining a test run ID from it
        Request request3 = new Request.Builder()
                .url(serverUrl+"/rest/createTestRun/"+getTestId()+"/"+getRunTag())
                .post(req)
                .build();
        Response response3 = client.newCall(request3).execute();
        String testRunId = response3.body().string();
        if(!response3.isSuccessful())
        {
            listener.getLogger().println("Test Run could not be created");
            run.setResult(Result.FAILURE);
            return;
        }
        else if(testRunId.equals("0"))
        {
            listener.getLogger().println("Your account either has insufficient balance or you ran out of tests");
            run.setResult(Result.FAILURE);
            return;
        }
        listener.getLogger().println("Test Run Created. Test Run ID: "+testRunId);

        //Creating a test instance
        Request request4 = new Request.Builder()
                .url(serverUrl+"/rest/createInstance.action/"+getTestId()+"/"+testRunId)
                .post(req)
                .build();
        Response response4 = client.newCall(request4).execute();
        if(!response4.isSuccessful())
        {
            listener.getLogger().println("Test Instance could not be created");
            run.setResult(Result.FAILURE);
            return;
        }
        listener.getLogger().println("Instance Created");

        //Staring test
        Request request5 = new Request.Builder()
                .url(serverUrl+"/rest/startTest.action/"+getTestId()+"/"+getRunTag()+"/"+testRunId)
                .post(req)
                .build();
        //Link to Frugal Testing test progress page
        String reportUrl = serverUrl+"/progress/"+getTestId()+"/"+getRunTag()+"/"+testRunId;
        String displayName = "Frugal Testing Report "+getRunTag();
        //Getting test report action
        FrugalAction faction = new FrugalAction();
        faction.setDisplayName(displayName);
        faction.setIconFileName(ICONPATH);
        faction.setReportUrl(reportUrl);
        run.addAction(faction);
        listener.getLogger().println("Monitor your test at :"+reportUrl);
        listener.getLogger().println("Starting test. This may take a few minutes. . . ");
        Response response5 = client.newCall(request5).execute();
        if(!response5.isSuccessful())
        {
            listener.getLogger().println("Test could not be started");
            run.setResult(Result.FAILURE);
            return;
        }

        //Getting live test status
        try {
            int counter=0;
            String previous_timeStamp = "";
            while(true) {
                Request request6 = new Request.Builder()
                        .url(serverUrl + "/rest/getLatestTestResultByRunID?testRunID=" + testRunId + "&locationID=all")
                        .addHeader("Connection","keep-alive")
                        .get()
                        .build();
                Response response6 = client.newCall(request6).execute();
                String jsonData = response6.body().string();
                FrugalFetchTestDetails fetch = new FrugalFetchTestDetails();
                try {
                    JSONObject jObject = new JSONObject(jsonData);
                    String toPrint = fetch.getLog(previous_timeStamp,jObject);
                    //Checking if test run completed
                    if(jObject.getString("testRunComplete").equals("yes"))break;
                    if(toPrint.equals(""))continue;
                    previous_timeStamp = toPrint.substring(1,9);
                    listener.getLogger().println(toPrint);
                    //print result in intervals of one minute
                    TimeUnit.SECONDS.sleep(30);
                }catch(Exception e) {
                    counter++;
                    if(counter>=20)
                    {
                        listener.getLogger().println("Results could not be fetched due to some reason. Please try again later");
                        run.setResult(Result.FAILURE);
                        return;
                    }
                    continue;
                }
                counter=0;
            }
        }
        catch(Exception e)
        {
            listener.getLogger().println("Test aborted "+e);
            run.setResult(Result.FAILURE);
            return;
        }
        listener.getLogger().println("Test Completed");

       //Checking if JTL is to be downloaded
        if(isGetJtl())
        {
            //Downloading JTL
            listener.getLogger().println("Downloading jtl file. . .");
            Request request7 = new Request.Builder()
                    .url(serverUrl+"/downloadJTL/"+getTestId()+"/"+testRunId)
                    .get()
                    .build();
            Response response7 = client.newCall(request7).execute();
            if(response7.isSuccessful())
            {
                listener.getLogger().println("Download Complete!");
                String jtlFile = response7.body().string();
                String workspaceDir = filePath+"/"+run.getId();
                String completeJtlPath = workspaceDir+"/"+"output_"+getTestId()+"_"+testRunId+".jtl";
                boolean created = new File(workspaceDir).mkdir();
                if(created) {
                    File file = new File(completeJtlPath);
                    Writer fw = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
                    fw.write(jtlFile);
                    fw.close();
                    listener.getLogger().println("Your file is at: " + filePath);
                }else listener.getLogger().println("Some error occurred in creating the directory to store your file");
            }
            else listener.getLogger().println("Download unsuccessful");
        }

        //clear cookies
        ck.getCookieStore().removeAll();
        return;
    }
}
