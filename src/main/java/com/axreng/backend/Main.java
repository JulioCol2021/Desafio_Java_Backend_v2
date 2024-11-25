
package com.axreng.backend;

import static spark.Spark.*;

/**
 * Main class for starting the backend service for web crawling.
 * This service provides HTTP endpoints to start a new crawl and to check the status of an existing crawl.
 */
public class Main {

    /**
     * Entry point of the backend application.
     * Sets up HTTP endpoints for handling crawl operations.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        // Endpoint to get the status of a crawl by ID
        get("/crawl/:id", (req, res) -> {
            // Retrieve the crawl ID from the request parameters
            String crawlId = req.params("id");
            // Return the crawl status as a response
            return "GET /crawl/" + crawlId;
        });

        // Endpoint to initiate a new crawl
        post("/crawl", (req, res) -> {
            // Retrieve the request body containing crawl details
            String requestBody = req.body();
            // Return a response indicating that a new crawl has been initiated
            return "POST /crawl" + System.lineSeparator() + requestBody;
        });
    }
}
