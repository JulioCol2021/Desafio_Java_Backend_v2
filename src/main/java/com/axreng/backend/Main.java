
package com.axreng.backend;

import static spark.Spark.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main class for the Backend Software Developer test.
 * Provides API endpoints for crawling a website based on a user-specified keyword.
 * The application follows SOLID principles and Clean Code practices.
 */
public class Main {

    private static final Map<String, SearchTask> tasks = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    private static final String BASE_URL = System.getenv("BASE_URL");

    public static void main(String[] args) {
        port(4567);

        // POST /crawl - Start a new search for a keyword
        post("/crawl", (req, res) -> {
            JsonObject requestBody = gson.fromJson(req.body(), JsonObject.class);
            String keyword = requestBody.get("keyword").getAsString().trim();

            // Keyword validation: must be between 4 and 32 characters
            if (keyword.length() < 4 || keyword.length() > 32) {
                res.status(400);
                return gson.toJson(Collections.singletonMap("error", "Keyword must be between 4 and 32 characters."));
            }

            String searchId = generateSearchId();
            SearchTask searchTask = new SearchTask(searchId, keyword);
            tasks.put(searchId, searchTask);

            new Thread(searchTask).start();  // Run the search in a separate thread

            res.status(200);
            res.type("application/json");
            return gson.toJson(Collections.singletonMap("id", searchId));
        });

        // GET /crawl/:id - Get the results of a search
        get("/crawl/:id", (req, res) -> {
            String searchId = req.params(":id");
            SearchTask task = tasks.get(searchId);

            if (task == null) {
                res.status(404);
                return gson.toJson(Collections.singletonMap("error", "Search ID not found."));
            }

            res.status(200);
            res.type("application/json");
            return gson.toJson(task);
        });
    }

    /**
     * Generates a unique alphanumeric search ID of 8 characters.
     *
     * @return A unique search ID.
     */
    private static String generateSearchId() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
    }

    /**
     * Class representing a search task that crawls the website for a given keyword.
     */
    private static class SearchTask implements Runnable {
        private final String id;
        private final String keyword;
        private final Set<String> urls = ConcurrentHashMap.newKeySet();
        private String status;

        public SearchTask(String id, String keyword) {
            this.id = id;
            this.keyword = keyword.toLowerCase();
            this.status = "active";
        }

        @Override
        public void run() {
            try {
                crawl(BASE_URL);
                status = "done";
            } catch (IOException e) {
                status = "failed";
            }
        }

        /**
         * Crawls the given URL for the specified keyword.
         *
         * @param url The URL to crawl.
         * @throws IOException If an error occurs while fetching the page.
         */
        private void crawl(String url) throws IOException {
            if (url == null || urls.contains(url) || !url.startsWith(BASE_URL)) {
                return;
            }

            urls.add(url);
            Document doc = Jsoup.connect(url).get();
            String bodyText = doc.text().toLowerCase();

            if (bodyText.contains(keyword)) {
                urls.add(url);
            }

            Elements links = doc.select("a[href]");
            for (var link : links) {
                String nextUrl = link.absUrl("href");
                crawl(nextUrl);
            }
        }

        public String getId() {
            return id;
        }

        public String getStatus() {
            return status;
        }

        public Set<String> getUrls() {
            return urls;
        }
    }
}
