package io.jenkins.plugins;

import com.squareup.okhttp.*;
import hudson.util.Secret;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.Iterator;

/**
 *
 * This class is responsible for providing us a way to fetch test details of a user
 *
 * @author Jakshat Desai
 *
 */

public class FrugalFetchTestDetails {

    //This function returns a JSON array consisting of data from all the user's tests
    public JSONArray fetchAllTests(String username, Secret password) throws IOException {
        String serverUrl = (new FrugalBuildStepDescriptor()).getDescriptor().getServerUrl();
        CookieManager ck = new CookieManager();
        CookieHandler.setDefault(ck);
        OkHttpClient client= new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        String loginContent = "username="+username+"&password="+password.getPlainText();
        RequestBody loginReq = RequestBody.create(mediaType,loginContent);
        RequestBody req = RequestBody.create(mediaType, "");

        //login
        Request request1 = new Request.Builder()
                .url(serverUrl+"/login)
                .post(loginReq)
                .build();
        client.newCall(request1).execute();

        //fetch tests
        Request request2 = new Request.Builder()
                .url(serverUrl +"/rest/getTestNTestRuns")
                .post(req)
                .build();
        Response response2 = client.newCall(request2).execute();

        ck.getCookieStore().removeAll();

        JSONArray jArray = new JSONArray();
        String jsonData = response2.body().string();
        try {
            JSONObject jObject = new JSONObject("{\"\":" + jsonData + "}");
            jArray = jObject.getJSONArray("");
            return jArray;
        }
        catch(Exception e)
        {
            System.out.println("Either there's a network error or user does not exist");
            return jArray;
        }
    }

    //This function obtains required data from JSON object containing data of latest test results
    public String getLog(String previous_timestamp, JSONObject jObject)
    {
        JSONObject testLogs = jObject.getJSONObject("testRunResults");
        Iterator<String> key = testLogs.keys();
        if(!key.hasNext())return "";
        String timestamp = key.next();
        if(timestamp.equals(previous_timestamp))return "";
        String ans="["+timestamp+"]";
        JSONArray jArray = testLogs.getJSONArray(timestamp);
        int flag = 0;
        for(int i=0;i<jArray.length();i++)
        {
            JSONObject object = jArray.getJSONObject(i);
            if(object.getString("label").equals("Total")) {
                flag = 1;
                ans = ans.concat(
                        "SampleCount:" + object.getInt("samplescount")
                        + "\tAvg:" + object.getInt("avgResponseTime")+"ms"
                        + "\tMin:" + object.getInt("minResponseTime")+"ms"
                        + "\tMax:" + object.getInt("maxResponseTime")+"ms"
                        + "\tErr:" + object.getFloat("errorPercentage")+"%"
                        + "\tHits/s:" + object.getFloat("throughput"));
                break;
            }
        }
        if(flag==0)return "";
        return ans;
    }
}
