package test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.junit.BeforeClass;
import org.junit.ClassRule;

public class HttpClientAndWireMockTestBase {
    @ClassRule
    public static final WireMockRule wireMockRule = new WireMockRule();

    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";

    protected static final URI uri = URI.create("http://localhost:8080/test?queryParam=value");
    protected static final URI uriAuth = URI.create("http://localhost:8080/auth");

    private static Authenticator myAuthenticator = new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            System.out.println("Authenticator was invoked");
            return new PasswordAuthentication(USERNAME, PASSWORD.toCharArray());
        }
    };

    private static CookieHandler myCookiHandler = new CookieHandler() {

        @Override
        public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
        }

        @Override
        public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
            System.out.println("CookieHandler was invoked");
            return Map.of("Cookie", List.of("cookie=0123456"));
        }
    };

    /**
     * Der Client sollte wiederverwendet werden, da bei seiner Erzeugung ein
     * Connectionpool erzeugt wird und so auch Verbindungen wiederverwendet werden.
     */
    protected static HttpClient httpClient;

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        httpClient = HttpClient.newBuilder().authenticator(myAuthenticator).cookieHandler(myCookiHandler)
                // .connectTimeout(Duration.ofMillis(500))
                // .followRedirects(HttpClient.Redirect.NORMAL)
                // .version(Version.HTTP_2)
                .build();
    }

    protected HttpRequest createSimpleRequest() {
        return HttpRequest.newBuilder().uri(uri).header("header", "header_value").timeout(Duration.ofMillis(500)).GET()
                .build();
    }

    protected HttpRequest createPostRequest() {
        return HttpRequest.newBuilder().uri(uri).header("header", "post").timeout(Duration.ofMillis(500)).POST(BodyPublishers.ofString("body")).build();
    }

    protected HttpRequest createAuthenticationRequest() {
        return HttpRequest.newBuilder().uri(uriAuth)
                /*
                 * Die Authentifizierung wird durch WireMock nicht vom Authenticator
                 * angefordert. Daher muss sie hier als Header direkt hinzugefügt werden.
                 * 
                 * gefundener Hinweis dazu
                 * (https://stackoverflow.com/questions/54208945/java-11-httpclient-not-sending-
                 * basic-authentication): As of the current JDK 11, HttpClient does not send
                 * Basic credentials until challenged for them with a WWW-Authenticate header
                 * from the server. Further, the only type of challenge it understands is for
                 * Basic authentication. The relevant JDK code is here (complete with TODO for
                 * supporting more than Basic auth) if you'd like to take a look.
                 *
                 * In the meantime, my remedy has been to bypass HttpClient's authentication API
                 * and to create and send the Basic Authorization header myself:
                 */
                .header("Authorization", basicAuth(USERNAME, PASSWORD)).timeout(Duration.ofMillis(500)).GET().build();
    }

    private static String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    protected void configWireMockForOkResponse() {
        /*
        * Muss hier urlPathEqualTo statt urlEqualTo sein, da sonst der QueryParam der URL mit geprüft wird und enthalten sein muss.
        */
        stubFor(get(urlPathEqualTo("/test")).withHeader("header", containing("value"))
                .withCookie("cookie", matching(".*12345.*")).withQueryParam("queryParam", equalTo("value"))
                .willReturn(ok("Hahaha").withHeader("header", "return")));
    }

    protected void configWireMockForPost() {
        stubFor(post(urlPathEqualTo("/test")).withRequestBody(equalTo("body")).withHeader("header", containing("post"))
                           .willReturn(ok("ok").withHeader("header", "ok")));
    }

    protected void configWireMockForAuthenticationAndOkResponse() {
        stubFor(get(urlEqualTo("/auth")).withBasicAuth(USERNAME, PASSWORD).willReturn(ok("Hahaha")));
    }

    protected void configWireMockForErrorResponse() {
        stubFor(get(urlPathEqualTo("/test")).willReturn(serverError()));
    }

    protected void configWireMockForLongOperation() {
        stubFor(get(urlPathEqualTo("/test")).willReturn(aResponse().withStatus(200).withFixedDelay(1000)));
    }

    protected void configWireMockForOkPost() {
        stubFor(post(urlPathEqualTo("/test")).willReturn(serverError()));
    }

    protected void configWireMockForCascade() {
        stubFor(get(urlPathEqualTo("/a")).willReturn(ok("Hello")));
        stubFor(get(urlPathEqualTo("/b")).willReturn(ok("World").withFixedDelay(2000)));
        stubFor(get(urlPathEqualTo("/c")).willReturn(ok("WireMock")));
    }
}
