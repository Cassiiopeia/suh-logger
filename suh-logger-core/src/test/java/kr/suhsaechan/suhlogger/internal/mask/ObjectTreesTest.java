package kr.suhsaechan.suhlogger.internal.mask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ObjectTreesTest {

    public record Token(String refreshToken, LocalDate issued) { }

    public static class Login {
        private final String username = "suh";
        private final String password = "p@ss";
        private final Token token = new Token("RT", LocalDate.of(2026, 1, 1));
        private static final String IGNORED = "static";
    }

    static class Node {
        String name = "n";
        List<Node> children = new ArrayList<>();
    }

    @Test
    void pojoAndRecordBecomeMaps() {
        Map<?, ?> tree = (Map<?, ?>) ObjectTrees.toTree(new Login());
        assertEquals("suh", tree.get("username"));
        assertEquals("RT", ((Map<?, ?>) tree.get("token")).get("refreshToken"));
        assertEquals(LocalDate.of(2026, 1, 1), ((Map<?, ?>) tree.get("token")).get("issued"));
        assertTrue(!tree.containsKey("IGNORED"));
    }

    @Test
    void cyclesAreCut() {
        Node n = new Node();
        n.children.add(n);
        Map<?, ?> tree = (Map<?, ?>) ObjectTrees.toTree(n);
        assertEquals(List.of("[CIRCULAR]"), tree.get("children"));
    }

    @Test
    void maskedDtoHidesNestedToken() {
        Masker masker = new Masker(List.of("password", "token"), "****");
        String out = String.valueOf(masker.mask(ObjectTrees.toTree(new Login())));
        assertTrue(!out.contains("p@ss") && !out.contains("RT"), out);
        assertTrue(out.contains("username=suh"), out);
    }

    @Test
    void excludedClassesAreNotExpanded() {
        kr.suhsaechan.suhlogger.config.SuhLoggerProperties p = new kr.suhsaechan.suhlogger.config.SuhLoggerProperties();
        p.setExcludedClasses(List.of(Login.class.getName()));
        kr.suhsaechan.suhlogger.util.SuhLogger.setProperties(p);
        try {
            Map<?, ?> tree = (Map<?, ?>) ObjectTrees.toTree(Map.of("login", new Login()));
            Map<?, ?> marker = (Map<?, ?>) tree.get("login");
            assertEquals("EXCLUDED_CLASS", marker.get("_type"));
            assertTrue(!String.valueOf(marker).contains("p@ss"), String.valueOf(marker));
        } finally {
            kr.suhsaechan.suhlogger.util.SuhLogger.setProperties(null);
        }
    }
}
