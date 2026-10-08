package kr.suhsaechan.suhlogger.internal.mask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Entity;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** 리뷰 C2·M1·M4: toString() 경로로 마스킹을 우회하던 타입과 크기 상한 */
class ObjectTreesLeakTest {

    private final Masker masker = new Masker(List.of("password", "token", "authorization"), "****");

    public record Login(String username, String password) { }

    @Entity
    public static class User {
        Long id = 1L;
        String email = "a@b.c";
        String password = "$2a$hash";
        Team team = new Team();
        List<User> friends = new ArrayList<>();

        @Override
        public String toString() {
            return "User(password=" + password + ")";
        }
    }

    @Entity
    public static class Team {
        String name = "t";
    }

    public static class Wrapper {
        String note = "n";
        User owner = new User();
    }

    private String masked(Object value) {
        return String.valueOf(masker.mask(ObjectTrees.toTree(value)));
    }

    @Test
    void optionalIsUnwrapped() {
        String out = masked(Optional.of(new Login("u", "s3cret")));
        assertFalse(out.contains("s3cret"), out);
        assertTrue(out.contains("username=u"), out);
    }

    @Test
    void mapEntryIsUnwrapped() {
        String out = masked(new AbstractMap.SimpleEntry<>("k", new Login("u", "s3cret2")));
        assertFalse(out.contains("s3cret2"), out);
    }

    @Test
    void atomicReferenceIsUnwrapped() {
        String out = masked(new AtomicReference<>(new Login("u", "s3cret3")));
        assertFalse(out.contains("s3cret3"), out);
    }

    @Test
    void unknownPlatformContainerShowsTypeOnly() {
        // 사용자 객체를 감쌀 수 있는 플랫폼 타입은 toString()을 쓰지 않는다
        String out = String.valueOf(ObjectTrees.toTree(new java.util.concurrent.CompletableFuture<>()));
        assertTrue(out.startsWith("[java.util.concurrent.CompletableFuture"), out);
    }

    @Test
    void valueTypesKeepToString() {
        assertEquals("https://x.y", ObjectTrees.toTree(java.net.URI.create("https://x.y")));
        assertEquals("PT1S", ObjectTrees.toTree(java.time.Duration.ofSeconds(1)));
    }

    @Test
    void entityFieldsAreMaskedAndAssociationsNotOpened() {
        Map<?, ?> tree = (Map<?, ?>) ObjectTrees.toTree(new User());
        String out = String.valueOf(masker.mask(tree));
        assertFalse(out.contains("$2a$hash"), out);
        assertEquals("[Team]", tree.get("team"));
        assertEquals("[association]", tree.get("friends"));
        assertEquals("a@b.c", tree.get("email"));
    }

    @Test
    void nestedEntityInsideDtoDoesNotUseCodecButIsStillSafe() {
        assertTrue(ObjectTrees.toTree(new Wrapper()) instanceof Map);
        String out = masked(new Wrapper());
        assertFalse(out.contains("$2a$hash"), out);
    }

    @Test
    void largeCollectionsAreCapped() {
        List<Integer> big = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            big.add(i);
        }
        List<?> tree = (List<?>) ObjectTrees.toTree(big);
        assertEquals(ObjectTrees.MAX_ELEMENTS + 1, tree.size());
        assertEquals("(" + (10_000 - ObjectTrees.MAX_ELEMENTS) + " more)", tree.get(ObjectTrees.MAX_ELEMENTS));
    }
}
