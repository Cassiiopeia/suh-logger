package kr.suhsaechan.suhlogger.webflux;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
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
                    exchangeLogger.logResponse(method, uri, status != null ? status.value() : 200,
                            capture.bytes(), capture.total());
                });
    }

    @Override
    public int getOrder() {
        // Servlet 필터와 같이 가장 마지막에 실행 — 보안 필터가 끝낸 요청도 최종 상태로 기록
        return Ordered.LOWEST_PRECEDENCE;
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
