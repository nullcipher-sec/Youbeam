package com.youbeam.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Cliente HTTP hacia el Streaming Server. Timeouts cortos para que un Streaming Server colgado no
 * deje hilos del Core esperando, y sin seguir redirecciones para que una respuesta 3xx no pueda
 * desviar la petición (con el ticket dentro) a otro destino.
 */
@Configuration
@EnableConfigurationProperties(StreamProperties.class)
public class StreamClientConfig {

    @Bean
    public RestClient streamRestClient(StreamProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
