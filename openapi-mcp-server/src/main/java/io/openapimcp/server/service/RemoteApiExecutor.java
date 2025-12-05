package io.openapimcp.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

@Component
public class RemoteApiExecutor {

    private static final Logger log = LoggerFactory.getLogger(RemoteApiExecutor.class);

    private final RestClient restClient;

    public RemoteApiExecutor(RestClient restClient) {
        this.restClient = restClient;
    }

    public RemoteApiResponse executeApiCall(String url, String httpMethod, Object body) {
        try {
            HttpMethod method = HttpMethod.valueOf(httpMethod.toUpperCase());
            RestClient.RequestBodySpec request = restClient.method(method).uri(url);

            if (method == POST || method == PUT || method == PATCH) {
                request = request.body(body != null ? body : "");
            }

            return request.exchange((req, resp) -> {
                String respBody = new String(resp.getBody().readAllBytes());
                return new RemoteApiResponse(
                    resp.getStatusCode().value(),
                        respBody);
            });

        } catch (HttpStatusCodeException e) {
            return new RemoteApiResponse(e.getStatusCode().value(), e.getResponseBodyAsString());

        } catch (Exception e) {
            log.error("Remote API query problem", e);
            return new RemoteApiResponse(500, "Remote API error: " + e.getMessage());
        }
    }
}
