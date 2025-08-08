package gatling.simulations;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * End-to-end API coverage simulation with strict success guarantees.
 * - Authenticates via /api/authenticate and extracts JWT from JSON: { "id_token": "..." }
 * - Uses the token for all subsequent requests
 * - Performs valid GratitudeEntry CRUD (create -> get -> delete) with unique per-user dates to avoid conflicts
 * - Optionally discovers OpenAPI endpoints if enabled (see enableApiDiscovery system property)
 * - Tracks coverage in a shared map and prints a summary at the end
 */
public class ApiCoverageSimulation extends Simulation {

    private final String baseURL = System.getProperty("baseURL", "http://localhost:8080");
    private final boolean enableApiDiscovery = Boolean.parseBoolean(System.getProperty("enableApiDiscovery", "false"));

    private final HttpProtocolBuilder httpProtocol = http
        .baseUrl(baseURL)
        .inferHtmlResources()
        .acceptHeader("application/json")
        .acceptEncodingHeader("gzip, deflate")
        .acceptLanguageHeader("en-US,en;q=0.9")
        .userAgentHeader("Gatling/ApiCoverageSimulation")
        .contentTypeHeader("application/json")
        .silentResources();

    // Shared API coverage map
    private static final Map<String, Boolean> apiCoverage = new ConcurrentHashMap<>();
    private static final Iterator<Map<String, Object>> uniqueIndexFeeder = IntStream.range(0, 10_000_000)
        .mapToObj(i -> Map.<String, Object>of("uniqueIndex", i))
        .iterator();

    private static void markCovered(String key) {
        apiCoverage.put(key, true);
    }

    private static void register(String key) {
        apiCoverage.putIfAbsent(key, false);
    }

    // -------------------- Helpers --------------------
    private ChainBuilder authRequest() {
        register("/api/authenticate [POST]");

        return exec(
            http("Authenticate")
                .post("/api/authenticate")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(StringBody("{\n  \"username\": \"admin\",\n  \"password\": \"admin\"\n}"))
                .check(status().is(200))
                .check(jsonPath("$.id_token").saveAs("jwt_token"))
                .check(bodyString().saveAs("auth_body"))
        )
            .exitHereIfFailed()
            .exec(session -> {
                String token = session.getString("jwt_token");
                System.out.println("[DEBUG] JWT Token: " + token);
                System.out.println("[DEBUG] Auth Response Body: " + session.getString("auth_body"));
                markCovered("/api/authenticate [POST]");
                return session;
            })
            // Sanity check: ensure token exists in session
            .exec(session -> {
                if (session.contains("jwt_token") && session.getString("jwt_token") != null && !session.getString("jwt_token").isEmpty()) {
                    return session;
                }
                throw new IllegalStateException("JWT token missing from session");
            })
            // Verify authenticated access to /api/account
            .exec(
                http("Get account (authenticated)")
                    .get("/api/account")
                    .header("Authorization", "Bearer ${jwt_token}")
                    .header("Accept", "application/json")
                    .check(status().is(200))
            )
            .exec(session -> {
                markCovered("/api/account [GET]");
                return session;
            })
            .exitHereIfFailed();
    }

    private ChainBuilder createGratitudeEntry() {
        register("/api/gratitude-entries [POST]");

        return feed(uniqueIndexFeeder)
            .exec(session -> {
                ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
                int uniqueIndex = session.getInt("uniqueIndex");
                String date = nowUtc.plusDays(uniqueIndex).toLocalDate().toString();
                String timestamp = nowUtc.toString();
                String uniqueText = "Auto-Gatling " + UUID.randomUUID();
                return session.set("entry_date", date).set("entry_timestamp", timestamp).set("entry_text", uniqueText);
            })
            .exec(
                http("Create GratitudeEntry")
                    .post("/api/gratitude-entries")
                    .header("Authorization", "Bearer ${jwt_token}")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(
                        StringBody(
                            "{\n" +
                            "  \"date\": \"${entry_date}\",\n" +
                            "  \"entry\": \"${entry_text}\",\n" +
                            "  \"mood\": \"HAPPY\",\n" +
                            "  \"timestamp\": \"${entry_timestamp}\"\n" +
                            "}"
                        )
                    )
                    .check(status().is(201))
                    .check(header("Location").saveAs("created_gratitude_entry_url"))
                    .check(jsonPath("$.id").saveAs("created_gratitude_entry_id"))
                    .check(bodyString().saveAs("create_body"))
            )
            .exitHereIfFailed()
            .exec(session -> {
                System.out.println("[DEBUG] Created URL: " + session.getString("created_gratitude_entry_url"));
                System.out.println("[DEBUG] Created ID: " + session.getString("created_gratitude_entry_id"));
                System.out.println("[DEBUG] Create Body: " + session.getString("create_body"));
                markCovered("/api/gratitude-entries [POST]");
                return session;
            })
            .pause(Duration.ofMillis(250));
    }

    private ChainBuilder listGratitudeEntries() {
        register("/api/gratitude-entries [GET]");

        return exec(
            http("List GratitudeEntries")
                .get("/api/gratitude-entries")
                .header("Authorization", "Bearer ${jwt_token}")
                .header("Accept", "application/json")
                .check(status().is(200))
                .check(bodyString().saveAs("list_body"))
        )
            .exitHereIfFailed()
            .exec(session -> {
                System.out.println("[DEBUG] List Body length: " + session.getString("list_body").length());
                markCovered("/api/gratitude-entries [GET]");
                return session;
            })
            .pause(Duration.ofMillis(150));
    }

    private ChainBuilder getGratitudeEntry() {
        register("/api/gratitude-entries/{id} [GET]");

        return exec(
            http("Get GratitudeEntry")
                .get("${created_gratitude_entry_url}")
                .header("Authorization", "Bearer ${jwt_token}")
                .header("Accept", "application/json")
                .check(status().is(200))
                .check(bodyString().saveAs("get_body"))
        )
            .exitHereIfFailed()
            .exec(session -> {
                System.out.println("[DEBUG] Get Body: " + session.getString("get_body"));
                markCovered("/api/gratitude-entries/{id} [GET]");
                return session;
            })
            .pause(Duration.ofMillis(250));
    }

    private ChainBuilder deleteGratitudeEntry() {
        register("/api/gratitude-entries/{id} [DELETE]");

        return exec(
            http("Delete GratitudeEntry")
                .delete("${created_gratitude_entry_url}")
                .header("Authorization", "Bearer ${jwt_token}")
                .header("Accept", "application/json")
                .check(status().is(204))
        )
            .exitHereIfFailed()
            .exec(session -> {
                System.out.println("[DEBUG] Deleted ID: " + session.getString("created_gratitude_entry_id"));
                markCovered("/api/gratitude-entries/{id} [DELETE]");
                return session;
            })
            .pause(Duration.ofMillis(250));
    }

    private ChainBuilder optionalOpenApiDiscovery() {
        // Guarded to avoid 404s when the api-docs profile is not enabled
        final ChainBuilder noop = exec(session -> session);

        if (!enableApiDiscovery) {
            return noop;
        }

        register("/v3/api-docs [GET]");

        return exec(
            http("OpenAPI Docs")
                .get("/v3/api-docs")
                .header("Authorization", "Bearer ${jwt_token}")
                .header("Accept", "application/json")
                .check(status().is(200))
                .check(bodyString().saveAs("openapi_json"))
        )
            .exitHereIfFailed()
            .exec(session -> {
                String openapiJson = session.getString("openapi_json");
                System.out.println("[DEBUG] OpenAPI JSON length: " + (openapiJson != null ? openapiJson.length() : 0));
                // Naive path-method registration for coverage visibility (parsing kept minimal to avoid heavy deps)
                // You can enhance it by adding a JSON parser if desired.
                markCovered("/v3/api-docs [GET]");
                return session;
            })
            .pause(Duration.ofMillis(100));
    }

    private final ScenarioBuilder scn = scenario("API Coverage Scenario")
        .exec(authRequest())
        .pause(Duration.ofMillis(200))
        .exec(optionalOpenApiDiscovery())
        .pause(Duration.ofMillis(200))
        .exec(listGratitudeEntries())
        .pause(Duration.ofMillis(200))
        .exec(createGratitudeEntry())
        .exec(getGratitudeEntry())
        .exec(deleteGratitudeEntry());

    public ApiCoverageSimulation() {
        // Register the endpoints we intend to test in coverage (initialized as false)
        register("/api/account [GET]");
        register("/api/gratitude-entries [GET]");
        register("/api/gratitude-entries [POST]");
        register("/api/gratitude-entries/{id} [GET]");
        register("/api/gratitude-entries/{id} [DELETE]");
        // Register other typical endpoints so they appear in the summary even if not exercised
        register("/api/gratitude-entries/{id} [PUT]");
        register("/api/gratitude-entries/{id} [PATCH]");
        if (enableApiDiscovery) {
            register("/v3/api-docs [GET]");
        }

        setUp(scn.injectOpen(rampUsers(10).during(Duration.ofSeconds(10)))).protocols(httpProtocol);

        // Print coverage summary after simulation
        System.out.println("\n======== API Coverage Summary ========");
        apiCoverage.forEach((k, v) -> System.out.printf("%-40s : %s%n", k, v ? "✅ tested" : "❌ untested"));
        long covered = apiCoverage.values().stream().filter(Boolean::booleanValue).count();
        long total = apiCoverage.size();
        double pct = total == 0 ? 100.0 : ((100.0 * covered) / total);
        System.out.printf("✅ API Coverage: %d / %d (%.2f%%)%n", covered, total, pct);
    }
}
