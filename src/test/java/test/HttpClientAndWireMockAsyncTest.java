package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

/**
 * Experimente mit HTTPClient und WireMock die auch als Beispiel zeigen wie
 * diese Bibliotheken verwendet werden können. Diese Tests sind mit asynchroner
 * Kommunikation.
 */
public class HttpClientAndWireMockAsyncTest extends HttpClientAndWireMockTestBase {

    /**
     * Bsp. für erfolgreiche asynchrone Get-Request-Respons-Kommunikation.
     */
    @Test
    public void okResponse() throws IOException, InterruptedException {
        configWireMockForOkResponse();

        String[] result = new String[1];

        var response = httpClient.sendAsync(createSimpleRequest(), BodyHandlers.ofString());
        response.thenAccept(r -> {
            assertEquals(200, r.statusCode());
            result[0] = r.body();
        });

        Thread.sleep(2000);
        assertEquals("Hahaha", result[0]);
    }

    /**
     * Bsp. für Timeout bei der asynchronen Get-Request-Respons-Kommunikation.
     */
    @Test
    public void timeout() throws IOException, InterruptedException, ExecutionException {
        configWireMockForLongOperation();

        var response = httpClient.sendAsync(createSimpleRequest(), BodyHandlers.ofString());
        String s = response.thenApply(r -> "").exceptionally(e -> {
            assertTrue(e.getCause() instanceof HttpTimeoutException);
            return "aaa";
        }).get();
        assertEquals("aaa", s);
    }

    /**
     * Bsp. für fehlgeschlagene asynchrone Get-Request-Respons-Kommunikation.
     * 
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void errorResponse() throws InterruptedException, ExecutionException {
        configWireMockForErrorResponse();

        var response = httpClient.sendAsync(createSimpleRequest(), BodyHandlers.ofString());
        String s = response.thenApply(this::extractBody).exceptionally(t -> "Hello WireMock").get();
        assertEquals("Hello WireMock", s);
    }

    /**
     * Bsp. für parallele asynchrone Aufrufe und der Kombination aller Ergebnisse zu
     * einer festen Reihenfolge. Die Reihenfolge geben die Aufrufe von thenCombine
     * vor.
     * 
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void asyncCallsWithCombine() throws InterruptedException, ExecutionException {
        configWireMockForCascade();

        var request1 = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/a")).GET().build();
        var request2 = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/b")).GET().build();
        var request3 = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/c")).GET().build();
        var response1 = httpClient.sendAsync(request1, BodyHandlers.ofString());
        var response2 = httpClient.sendAsync(request2, BodyHandlers.ofString());
        var response3 = httpClient.sendAsync(request3, BodyHandlers.ofString());

        var res = response1
                .thenApply(this::extractBody).thenCombine(response2.thenApply(this::extractBody)
                        .thenCombine(response3.thenApply(this::extractBody), String::concat), (a, b) -> a + " " + b)
                .get();

        assertEquals("Hello WorldWireMock", res);
    }

    /**
     * Bsp. für parallele asynchrone Aufrufe und der Kombination der ersten
     * Ergebnisse zu einer festen Reihenfolge.
     * 
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void asyncCallsWithEither() throws InterruptedException, ExecutionException {
        configWireMockForCascade();

        var request1 = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/a")).GET().build();
        var request2 = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/b")).GET().build();
        var request3 = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/c")).GET().build();
        var response1 = httpClient.sendAsync(request1, BodyHandlers.ofString());
        var response2 = httpClient.sendAsync(request2, BodyHandlers.ofString());
        var response3 = httpClient.sendAsync(request3, BodyHandlers.ofString());
        var res = response1.thenApply(this::extractBody)
                .thenCombineAsync(response2.applyToEitherAsync(response3, this::extractBody), (a, b) -> a + " " + b)
                .get();
        assertEquals("Hello WireMock", res);
    }

    private String extractBody(HttpResponse<String> response) {
        if (response.statusCode() != 200)
            throw new RuntimeException();
        return response.body();
    }
}
