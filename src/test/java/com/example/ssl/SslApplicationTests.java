package com.example.ssl;

import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

//@SpringBootTest
class SslApplicationTests {

    public static SSLContext sslContext;
    public static HttpClient client;

    {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            ClassPathResource r = new ClassPathResource("cli.jks");
            keyStore.load(r.getInputStream(), "123456".toCharArray());
            sslContext = SSLContext.getInstance("TLS");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .executor(Executors.newScheduledThreadPool(50))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        } catch (Exception e) {
        }
    }

    @Test
    void contextLoads() throws Exception {
        ScheduledExecutorService es = Executors.newScheduledThreadPool(10);
        CountDownLatch l = new CountDownLatch(500);
        AtomicInteger al = new AtomicInteger();
        IntStream.range(0, 500).forEach(i -> es.submit(() -> {
            try {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .uri(URI.create("https://localhost:8080/hello"))
                        .GET()
                        .build(), HttpResponse.BodyHandlers.ofString());
                System.out.println(response.body());
            } catch (Exception e) {
            } finally {
                l.countDown();
                al.incrementAndGet();
            }
        }));
        l.await();
        System.out.println("al = " + al);
    }
}
