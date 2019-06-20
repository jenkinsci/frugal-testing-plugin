package io.jenkins.plugins;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.common.collect.ImmutableList;
import hudson.model.Item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static hudson.security.ACL.SYSTEM;

/**
 *
 * This class handles operations associated with credentials
 *
 * @author Jakshat Desai
 *
 */

public class FrugalCredentialsOps {

    public static final List<DomainRequirement> NO_REQUIREMENTS = ImmutableList.of();

    //This function returns an array of all the credentials which have been set up in jenkins for the plugin
    public ArrayList<FrugalCredentials> getAllCredentials()
    {
        Set<String> ids = new HashSet<String>();
        ArrayList<FrugalCredentials> allCreds = new ArrayList<FrugalCredentials>();

        for (final FrugalCredentials c : CredentialsProvider
                .lookupCredentials(FrugalCredentials.class, (Item) null,SYSTEM,NO_REQUIREMENTS)) {
            final String id = c.getId();
            if (!ids.contains(id)) {
                ids.add(id);
                allCreds.add(c);
            }
        }
        return allCreds;
    }

    //This function returns the credentials for the userId passed to it as an argument
    public FrugalCredentials getCredentials(String userId)
    {
        ArrayList<FrugalCredentials> allCred = getAllCredentials();

        for(FrugalCredentials c: allCred) {
            if(c.getId().equals(userId)) {
                return c;
            }
        }
        return null;
    }
}
