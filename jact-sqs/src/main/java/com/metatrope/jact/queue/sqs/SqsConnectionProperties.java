package com.metatrope.jact.queue.sqs;

import java.net.URI;
import java.net.URISyntaxException;

public class SqsConnectionProperties {
    private String user;
    private String secret;
    private String region;
    private URI endpoint; 
    
    public SqsConnectionProperties() {
        
    }
    
    public SqsConnectionProperties(String user, String secret, String region, String endpoint) throws URISyntaxException {
        this(user, secret, region, new URI(endpoint));
    }

    public SqsConnectionProperties(String user, String secret, String region, URI endpoint) {
        setUser(user);
        setSecret(secret);
        setRegion(region);
        setEndpoint(endpoint);
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
    }
}
