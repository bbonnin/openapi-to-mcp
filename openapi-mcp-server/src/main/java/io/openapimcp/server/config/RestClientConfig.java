package io.openapimcp.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

@Configuration
public class RestClientConfig {

    private static final Logger log = LoggerFactory.getLogger(RestClientConfig.class);

    private List<HttpMessageConverter<?>> getMessageConverters() {
        MappingJackson2HttpMessageConverter httpMessageConverter = new MappingJackson2HttpMessageConverter();
        httpMessageConverter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                MediaType.valueOf("text/json")
        ));

        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(); // Pour éventuellement logguer la réponse brute

        return List.of(httpMessageConverter, stringConverter);
    }

    @Bean
    @Profile("!dev")
    public RestClient restClient(RestClient.Builder builder) {
        return builder
                .messageConverters(getMessageConverters())
                .build();
    }

    @Bean
    @Profile("dev")
    public RestClient unsafeRestClient(RestClient.Builder builder) throws NoSuchAlgorithmException, KeyManagementException {
        // Trust manager accepting all certificates
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { /* No check */ }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { /* No check */ }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                if (connection instanceof HttpsURLConnection httpsConnection) {
                    httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                    httpsConnection.setHostnameVerifier((hostname, session) -> true);
                }
            }
        };

        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        return builder
                .requestFactory(new BufferingClientHttpRequestFactory(requestFactory))
                .messageConverters(getMessageConverters())
                .requestInterceptor(loggingInterceptor())
                .build();
    }

    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {

            // --- LOG REQUEST ---
            log.info("-> REQUEST {} {}", request.getMethod(), request.getURI());
            request.getHeaders().forEach((k, v) -> log.info("Request header: {}={}", k, v));
            log.info("Request body: {}", new String(body, StandardCharsets.UTF_8));

            ClientHttpResponse response = execution.execute(request, body);

            // --- LOG RESPONSE ---
            log.info("<- RESPONSE Status: {}", response.getStatusCode());
            response.getHeaders().forEach((k, v) -> log.info("Response header: {}={}", k, v));

            String responseBody = new String(response.getBody().readAllBytes());
            log.info("Response body: {}", responseBody);

            return response;
        };
    }
}
