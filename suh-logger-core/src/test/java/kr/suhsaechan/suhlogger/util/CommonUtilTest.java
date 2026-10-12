package kr.suhsaechan.suhlogger.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import org.junit.jupiter.api.Test;

/** 2.x부터 있던 CommonUtil 공개 static 메서드의 계약 고정 */
class CommonUtilTest {

    @Test
    void sensitiveMatchingIsCaseInsensitiveContains() {
        assertTrue(CommonUtil.isSensitive("UserPassword", List.of("password")));
        assertFalse(CommonUtil.isSensitive("username", List.of("password")));
        assertFalse(CommonUtil.isSensitive(null, List.of("password")));
        assertFalse(CommonUtil.isSensitive("password", null));
        assertFalse(CommonUtil.isSensitive("password", List.of()));
        assertTrue(CommonUtil.isSensitiveField("token", List.of("TOKEN")), "deprecated 별칭도 같은 결과");
    }

    @Test
    void maskValueFallsBackToDefault() {
        assertEquals("****", CommonUtil.getMaskValue(null));
        SuhLoggerProperties.MaskingConfig c = new SuhLoggerProperties.MaskingConfig();
        c.setMaskValue("[x]");
        assertEquals("[x]", CommonUtil.getMaskValue(c));
        c.setMaskValue(null);
        assertEquals("****", CommonUtil.getMaskValue(c));
    }

    @Test
    void maskParametersReplacesOnlySensitiveKeys() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "suh");
        params.put("password", "p@ss");
        Map<String, Object> out = CommonUtil.maskParameters(params, List.of("pass"), "###");
        assertEquals("suh", out.get("name"));
        assertEquals("###", out.get("password"));
        assertEquals("p@ss", params.get("password"), "원본 맵은 바꾸지 않는다");
        assertSame(params, CommonUtil.maskParameters(params, List.of(), "###"));
        assertNull(CommonUtil.maskParameters(null, List.of("a"), "#"));
        assertEquals("****", CommonUtil.maskParameters(params, List.of("name"), null).get("name"));
    }

    @Test
    void maskHeadersReplacesOnlySensitiveKeys() {
        Map<String, String> h = Map.of("Authorization", "Bearer x", "Accept", "json");
        Map<String, String> out = CommonUtil.maskHeaders(h, List.of("authorization"), "***");
        assertEquals("***", out.get("Authorization"));
        assertEquals("json", out.get("Accept"));
        assertSame(h, CommonUtil.maskHeaders(h, null, "***"));
    }

    @Test
    void safeSerializationHandlesNullPrimitivesAndNesting() {
        assertNull(CommonUtil.makeSafeForSerialization(null));
        assertEquals("s", CommonUtil.makeSafeForSerialization("s"));
        Map<String, Object> nested = new HashMap<>();
        nested.put("v", new Vector<>(List.of(1)));
        Object safe = CommonUtil.makeSafeForSerialization(nested);
        Object inner = ((Map<?, ?>) safe).get("v");
        assertEquals("Vector", ((Map<?, ?>) inner).get("_type"), "맵 안의 Vector도 안전 변환");
        Object[] arr = (Object[]) CommonUtil.makeSafeForSerialization(List.of(new File("a.txt")));
        assertEquals("File", ((Map<?, ?>) arr[0]).get("_type"));
        int[] primitives = {1, 2};
        assertSame(primitives, CommonUtil.makeSafeForSerialization(primitives), "원시 배열은 그대로");
    }

    @Test
    void fileInfoCoversExistingAndMissing() {
        Map<String, Object> missing = CommonUtil.extractFileInfo(new File("/definitely/not/here.txt"));
        assertEquals(false, missing.get("exists"));
        assertEquals(-1, missing.get("length"));
        Map<String, Object> exists = CommonUtil.extractFileInfo(new File("build.gradle").exists()
                ? new File("build.gradle") : new File(System.getProperty("java.io.tmpdir")));
        assertEquals(true, exists.get("exists"));
    }

    @Test
    void vectorInfoReportsShape() {
        Map<String, Object> info = CommonUtil.extractVectorInfo(new Vector<>(List.of("a", "b")));
        assertEquals(2, info.get("size"));
        assertEquals("String", info.get("elementType"));
        assertEquals("unknown", CommonUtil.extractVectorInfo(new Vector<>()).get("elementType"));
    }

    @Test
    void excludedClassMatchingUsesNameOrFragment() {
        assertTrue(CommonUtil.isExcludedClass("x", List.of("java.lang.String")));
        assertTrue(CommonUtil.isExcludedClass("x", List.of("lang.Str")));
        assertFalse(CommonUtil.isExcludedClass("x", List.of("Integer")));
        assertFalse(CommonUtil.isExcludedClass(null, List.of("a")));
        assertFalse(CommonUtil.isExcludedClass("x", null));
    }

    @Test
    void createSafeMapMasksSensitiveFieldsAndSummarizesCollections() {
        class Holder {
            String id = "1";
            String password = "p@ss";
            List<String> tags = new ArrayList<>(List.of("a", "b"));
            Map<String, String> meta = Map.of("k", "v");
        }
        SuhLoggerProperties.MaskingConfig masking = new SuhLoggerProperties.MaskingConfig();
        Map<String, Object> out = CommonUtil.createSafeMap(new Holder(), masking);
        assertEquals("****", out.get("password"));
        assertEquals("1", out.get("id"));
        assertEquals("[Collection: 2 items]", out.get("tags"));
        assertEquals("[Map: 1 entries]", out.get("meta"));
        assertFalse(out.containsKey("_toString"), "마스킹 중에는 원문이 담긴 toString을 남기지 않는다");
        assertTrue(CommonUtil.createSafeMap(null).isEmpty());
        masking.setEnabled(false);
        assertTrue(CommonUtil.createSafeMap(new Holder(), masking).containsKey("_toString"), "끄면 2.x처럼 남긴다");
    }

    @Test
    void jtsDetectionIsByNameWithoutTheLibrary() {
        assertFalse(CommonUtil.isJTSGeometryType(null));
        assertFalse(CommonUtil.isJTSGeometryType("plain"));
        assertFalse(CommonUtil.isMultipartFileType("plain"));
    }
}
