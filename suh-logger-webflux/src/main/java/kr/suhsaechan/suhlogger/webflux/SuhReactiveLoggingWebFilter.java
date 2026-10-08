package kr.suhsaechan.suhlogger.webflux;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.internal.http.HttpExchangeLogger;
import org.reactivestreams.Publisher;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

/**
 * WebFlux 응답 로깅 WebFilter. Servlet 필터와 같은 출력 규칙(HttpExchangeLogger)을 쓴다.
 * 본문은 클라이언트로 그대로 흘려보내면서 로그용으로 최대 maxResponseBodySize 바이트까지만 복사한다 —
 * 큰 응답을 통째로 메모리에 쌓지 않기 위해.
 */
public class SuhReactiveLoggingWebFilter implements WebFilter, Ordered {

    private final HttpExchangeLogger exchangeLogger;

    public SuhReactiveLoggingWebFilter(SuhLoggerProperties properties) {
        this.exchangeLogger = new HttpExchangeLogger(properties);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String uri = exchange.getRequest().getURI().getPath();
        if (!exchangeLogger.isEnabled() || exchangeLogger.isExcluded(uri)) {
            return chain.filter(exchange);
        }
        String method = exchange.getRequest().getMethod().name();
        // 요청 ID는 로그 줄과 응답 헤더에만 넣는다 — MDC는 스레드 로컬이라 리액티브 체인을 따라가지 않는다
        SuhLoggerProperties.RequestIdConfig rid = exchangeLogger.requestIdConfig();
        String requestId = null;
        if (rid.isEnabled()) {
            requestId = exchange.getRequest().getHeaders().getFirst(rid.getHeader());
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }
            exchange.getResponse().getHeaders().set(rid.getHeader(), requestId);
        }
        String finalRequestId = requestId;
        long start = System.nanoTime();
        BodyCapture capture = new BodyCapture(Math.max(exchangeLogger.maxBodySize(), 0));
        ServerHttpResponse decorated = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return super.writeWith(Flux.from(body).doOnNext(capture::copy));
            }

            @Override
            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                return super.writeAndFlushWith(Flux.from(body)
                        .map(inner -> Flux.from(inner).doOnNext(capture::copy)));
            }
        };
        return chain.filter(exchange.mutate().response(decorated).build())
                // 완료·에러·취소 어느 경우든 한 번 로깅 (WebFlux에는 finally 블록이 없다)
                .doFinally(signal -> {
                    HttpStatusCode status = decorated.getStatusCode();
                    int code = status != null ? status.value() : 200;
                    // 에러 신호는 WebExceptionHandler가 상태를 정하기 전이라 아직 200일 수 있다 — 500으로 보정
                    if (signal == SignalType.ON_ERROR && code < 400) {
                        code = 500;
                    }
                    exchangeLogger.logResponse(method, uri, code,
                            capture.bytes(), capture.total(), (System.nanoTime() - start) / 1_000_000, finalRequestId);
                });
    }

    @Override
    public int getOrder() {
        // Servlet 필터와 같은 규칙: 기본 가장 마지막, suh-logger.filter-order로 변경
        return exchangeLogger.filterOrder();
    }

    /** 로그용 앞부분만 보관 — 읽기 위치를 건드리지 않아 클라이언트 전송에 영향이 없다 */
    private static final class BodyCapture {

        private final int limit;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final AtomicLong total = new AtomicLong();

        BodyCapture(int limit) {
            this.limit = limit;
        }

        synchronized void copy(DataBuffer buffer) {
            total.addAndGet(buffer.readableByteCount());
            try (DataBuffer.ByteBufferIterator it = buffer.readableByteBuffers()) {
                while (it.hasNext() && out.size() < limit) {
                    ByteBuffer bb = it.next();
                    int n = Math.min(bb.remaining(), limit - out.size());
                    byte[] chunk = new byte[n];
                    bb.get(chunk);
                    out.write(chunk, 0, n);
                }
            }
        }

        synchronized byte[] bytes() {
            return out.toByteArray();
        }

        long total() {
            return total.get();
        }
    }
}
