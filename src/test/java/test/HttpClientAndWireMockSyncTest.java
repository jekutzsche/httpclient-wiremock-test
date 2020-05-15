package test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import org.junit.Test;

/**
 * Experimente mit HTTPClient und WireMock die auch als Beispiel zeigen wie
 * diese Bibliotheken verwendet werden können. Diese Tests sind mit synchroner
 * Kommunikation.
 */
public class HttpClientAndWireMockSyncTest extends HttpClientAndWireMockTestBase {

    /**
     * Bsp. für erfolgreiche Get-Request-Respons-Kommunikation.
     */
    @Test
    public void okRequest() throws IOException, InterruptedException {
        configWireMockForOkResponse();

        var response = httpClient.send(createSimpleRequest(), BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("Hahaha", response.body());
        assertEquals("return", response.headers().firstValue("header").get());
    }

    /**
     * Bsp. für erfolgreiche Post-Request-Respons-Kommunikation.
     */
    @Test
    public void postRequest() throws IOException, InterruptedException {
        configWireMockForPost();

        var response = httpClient.send(createPostRequest(), BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("ok", response.body());
        assertEquals("ok", response.headers().firstValue("header").get());
    }

    /**
     * Bsp. für erfolgreiche Get-Request-Respons-Kommunikation mit
     * Authentifizierung.
     */
    @Test
    public void authentication() throws IOException, InterruptedException {
        configWireMockForAuthenticationAndOkResponse();

        var response = httpClient.send(createAuthenticationRequest(), BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("Hahaha", response.body());
    }

    /**
     * Bsp. für fehlgeschlagene Get-Request-Respons-Kommunikation.
     */
    @Test
    public void errorResponse() throws IOException, InterruptedException {
        configWireMockForErrorResponse();

        var response = httpClient.send(createSimpleRequest(), BodyHandlers.ofString());
        assertEquals(500, response.statusCode());
        assertEquals("", response.body());
    }

    /**
     * Bsp. für Timeout bei der Get-Request-Respons-Kommunikation.
     */
    @Test
    public void timeout() throws IOException, InterruptedException {
        configWireMockForLongOperation();

        try {
            httpClient.send(createSimpleRequest(), BodyHandlers.ofString());
            fail("HttpTimeoutException expected");
        } catch (HttpTimeoutException e) {
        }

        var request = HttpRequest.newBuilder().uri(uri).timeout(Duration.ofMillis(1500)).GET().build();
        var response = httpClient.send(request, BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    /**
     * Bsp. für das Verhalten von WireMock wenn eine nicht konfigurierte URL
     * aufgerufen wird. Hier muss ein neuer WireMockServer gestartet werden, da der
     * von der Rule gestartete beim Beenden auf unbekannte Requests prüft und den
     * Test mit einer Exception beendet, wenn solche vorhanden sind.
     */
    @Test
    public void wireMockExceptionIfUnknownRequests() throws IOException, InterruptedException {
        var wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8081));
        wireMockServer.start();

        var request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8081/unknown")).GET().build();
        var response = httpClient.send(request, BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertFalse(wireMockServer.findUnmatchedRequests().getRequests().isEmpty());

        wireMockServer.stop();
    }
}
