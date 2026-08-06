package com.ai.manager.system.iot;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import javax.net.ssl.SSLSession;

/**
 * 测试辅助：mock {@link HttpClient#send} 需要的 {@link HttpResponse} 实现，
 * 以及从 {@link HttpRequest.BodyPublisher} 读取请求体的工具。
 * <p>
 * 与 {@code client/BaiduPanClient} 同款 JDK HttpClient 模式，HTTP 层用 mock 隔离。
 */
public final class TestHttpSupport {

    private TestHttpSupport() {
    }

    /**
     * 构造一个固定状态码与 body 的 HttpResponse。
     */
    public static <T> HttpResponse<T> response(HttpRequest request, int statusCode, T body) {
        return new StubHttpResponse<>(request, statusCode, body);
    }

    /**
     * 读取 BodyPublisher 全部字节（用于断言 multipart / JSON 请求体）。
     */
    public static byte[] readBody(HttpRequest.BodyPublisher publisher) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CompletableFuture<Void> done = new CompletableFuture<>();
        publisher.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                out.writeBytes(bytes);
            }

            @Override
            public void onError(Throwable throwable) {
                done.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                done.complete(null);
            }
        });
        done.join();
        return out.toByteArray();
    }

    /**
     * 最小可用的 {@link HttpResponse} 桩。
     */
    private static final class StubHttpResponse<T> implements HttpResponse<T> {
        private final HttpRequest request;
        private final int statusCode;
        private final T body;
        private final HttpHeaders headers;

        private StubHttpResponse(HttpRequest request, int statusCode, T body) {
            this.request = request;
            this.statusCode = statusCode;
            this.body = body;
            this.headers = HttpHeaders.of(new HashMap<>(), (name, value) -> true);
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return headers;
        }

        @Override
        public T body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
