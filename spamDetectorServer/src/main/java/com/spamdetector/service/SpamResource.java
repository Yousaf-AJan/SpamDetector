package com.spamdetector.service;

import com.spamdetector.domain.TestFile;
import com.spamdetector.util.SpamDetector;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import java.io.File;
import java.util.List;

import jakarta.ws.rs.core.Response;

@Path("/spam")
public class SpamResource {

    private final SpamDetector detector = new SpamDetector();

    SpamResource(){
//      TODO: load resources, train and test to improve performance on the endpoint calls
        System.out.print("Training and testing the model, please wait");
        this.trainAndTest();
    }

    @GET
    @Produces("application/json")
    public Response getSpamResults() {
        List<TestFile> testResults = trainAndTest();
        Response response = Response.status(200)
                .header("Access-Control-Allow-Origin", "http://localhost:63342")
                .header("Content-Type", "application/json")
                .entity(testResults.toArray(new TestFile[0]))
                .build();
        return response;
    }
    @GET
    @Path("/accuracy")
    @Produces("application/json")
    public Response getAccuracy() {
        double accuracy = detector.getAccuracy();
        Response response = Response.status(200)
                .header("Access-Control-Allow-Origin", "http://localhost:63342")
                .header("Content-Type", "application/json")
                .entity(accuracy)
                .build();
        return response;
    }

    @GET
    @Path("/precision")
    @Produces("application/json")
    public Response getPrecision() {
        double precision = detector.getPrecision();
        Response response = Response.status(200)
                .header("Access-Control-Allow-Origin", "http://localhost:63342")
                .header("Content-Type", "application/json")
                .entity(precision)
                .build();
        return response;
    }

    private List<TestFile> trainAndTest() {

        File mainDirectory = new File(getClass().getClassLoader().getResource("data").getFile());
        return detector.trainAndTest(mainDirectory);
    }
}