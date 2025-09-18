package me.suhsaechan.suhlogger.util;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * 공통 유틸리티 클래스
 */
public class commonUtil {
    
    /**
     * 객체를 JSON 직렬화 가능한 안전한 형태로 변환
     * 특히 MultipartFile, JTS Geometry와 같은 직렬화 불가능 객체를 처리
     */
    public static Object makeSafeForSerialization(Object obj) {
        return makeSafeForSerialization(obj, null);
    }
    
    /**
     * 객체를 JSON 직렬화 가능한 안전한 형태로 변환 (제외 클래스 목록 포함)
     * 특히 MultipartFile, JTS Geometry와 같은 직렬화 불가능 객체를 처리
     */
    public static Object makeSafeForSerialization(Object obj, List<String> excludedClasses) {
        if (obj == null) {
            return null;
        }
        
        // 설정 기반 제외 클래스 체크
        if (excludedClasses != null && isExcludedClass(obj, excludedClasses)) {
            return createExcludedClassInfo(obj);
        }
        
        // InputStream은 항상 안전한 맵으로 대체
        if (obj instanceof InputStream) {
            Map<String, Object> result = new HashMap<>();
            result.put("_type", "InputStream");
            result.put("_class", obj.getClass().getName());
            return result;
        }
        
        // 클래스 이름에 MultipartFile이 포함된 객체는 안전하게 처리
        // 단, excludedClasses에 포함된 경우는 제외
        String className = obj.getClass().getName();
        if (className.contains("MultipartFile")) {
            // excludedClasses에 포함되어 있으면 제외된 클래스 정보만 반환
            if (excludedClasses != null && isExcludedClass(obj, excludedClasses)) {
                return createExcludedClassInfo(obj);
            }
            return extractMultipartFileInfo(obj);
        }
        
        // Vector 객체 처리
        if (obj instanceof Vector) {
            // excludedClasses에 포함되어 있으면 제외된 클래스 정보만 반환
            if (excludedClasses != null && isExcludedClass(obj, excludedClasses)) {
                return createExcludedClassInfo(obj);
            }
            return extractVectorInfo((Vector<?>) obj);
        }
        
        // File 객체 처리
        if (obj instanceof File) {
            // excludedClasses에 포함되어 있으면 제외된 클래스 정보만 반환
            if (excludedClasses != null && isExcludedClass(obj, excludedClasses)) {
                return createExcludedClassInfo(obj);
            }
            return extractFileInfo((File) obj);
        }
        
        // JTS Geometry 객체 처리 (Point, Polygon 등)
        if (className.contains("org.locationtech.jts.geom") || isJTSGeometryType(obj)) {
            return extractJTSGeometryInfo(obj);
        }
        
        // Map의 경우 각 값을 안전하게 처리
        if (obj instanceof Map) {
            Map<Object, Object> original = (Map<Object, Object>) obj;
            Map<Object, Object> safe = new HashMap<>();
            
            for (Map.Entry<Object, Object> entry : original.entrySet()) {
                safe.put(entry.getKey(), makeSafeForSerialization(entry.getValue(), excludedClasses));
            }
            
            return safe;
        }
        
        // 컬렉션의 경우 각 항목을 안전하게 처리
        if (obj instanceof Collection) {
            Collection<?> original = (Collection<?>) obj;
            Object[] safe = new Object[original.size()];
            
            int i = 0;
            for (Object item : original) {
                safe[i++] = makeSafeForSerialization(item, excludedClasses);
            }
            
            return safe;
        }
        
        // 배열의 경우 각 항목을 안전하게 처리
        if (obj.getClass().isArray()) {
            try {
                Object[] array = (Object[]) obj;
                Object[] safe = new Object[array.length];
                
                for (int i = 0; i < array.length; i++) {
                    safe[i] = makeSafeForSerialization(array[i], excludedClasses);
                }
                
                return safe;
            } catch (ClassCastException e) {
                // 원시 타입 배열인 경우 그대로 반환
                return obj;
            }
        }
        
        // 일반 객체의 경우, toString() 결과에서 "MultipartFile"이 포함되어 있으면 안전하게 처리
        String toString = obj.toString();
        if (toString.contains("MultipartFile")) {
            return createSafeMap(obj);
        }
        
        return obj;
    }
    
    /**
     * MultipartFile 객체에서 중요 정보를 추출
     */
    public static Map<String, Object> extractMultipartFileInfo(Object multipartFile) {
        Map<String, Object> info = new HashMap<>();
        info.put("_type", "MultipartFile");
        
        try {
            // 리플렉션을 사용하여 MultipartFile 메서드 호출
            try {
                Object fileName = multipartFile.getClass().getMethod("getOriginalFilename").invoke(multipartFile);
                info.put("fileName", fileName);
            } catch (Exception e) {
                info.put("fileName", "unknown");
            }
            
            try {
                Object contentType = multipartFile.getClass().getMethod("getContentType").invoke(multipartFile);
                info.put("contentType", contentType);
            } catch (Exception e) {
                info.put("contentType", "unknown");
            }
            
            try {
                Object size = multipartFile.getClass().getMethod("getSize").invoke(multipartFile);
                info.put("size", size);
            } catch (Exception e) {
                info.put("size", -1);
            }
            
            try {
                Object isEmpty = multipartFile.getClass().getMethod("isEmpty").invoke(multipartFile);
                info.put("isEmpty", isEmpty);
            } catch (Exception e) {
                info.put("isEmpty", "unknown");
            }
            
        } catch (Exception e) {
            info.put("error", "정보 추출 실패: " + e.getMessage());
        }
        
        return info;
    }
    
    /**
     * Vector 객체에서 중요 정보를 추출
     */
    public static Map<String, Object> extractVectorInfo(Vector<?> vector) {
        Map<String, Object> info = new HashMap<>();
        info.put("_type", "Vector");
        info.put("size", vector.size());
        info.put("capacity", vector.capacity());
        info.put("isEmpty", vector.isEmpty());
        
        // 첫 번째 요소의 타입 정보 (있는 경우)
        if (!vector.isEmpty()) {
            Object firstElement = vector.get(0);
            if (firstElement != null) {
                info.put("elementType", firstElement.getClass().getSimpleName());
            } else {
                info.put("elementType", "null");
            }
        } else {
            info.put("elementType", "unknown");
        }
        
        return info;
    }
    
    /**
     * File 객체에서 중요 정보를 추출
     */
    public static Map<String, Object> extractFileInfo(File file) {
        Map<String, Object> info = new HashMap<>();
        info.put("_type", "File");
        info.put("name", file.getName());
        info.put("path", file.getPath());
        info.put("absolutePath", file.getAbsolutePath());
        info.put("exists", file.exists());
        info.put("isFile", file.isFile());
        info.put("isDirectory", file.isDirectory());
        
        if (file.exists()) {
            info.put("length", file.length());
            info.put("lastModified", file.lastModified());
            info.put("canRead", file.canRead());
            info.put("canWrite", file.canWrite());
        } else {
            info.put("length", -1);
            info.put("lastModified", -1);
            info.put("canRead", false);
            info.put("canWrite", false);
        }
        
        return info;
    }
    
    /**
     * 객체가 제외 클래스 목록에 포함되는지 확인
     */
    public static boolean isExcludedClass(Object obj, List<String> excludedClasses) {
        if (obj == null || excludedClasses == null || excludedClasses.isEmpty()) {
            return false;
        }
        
        String className = obj.getClass().getName();
        
        for (String excludedClass : excludedClasses) {
            if (className.equals(excludedClass) || className.contains(excludedClass)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 제외된 클래스에 대한 기본 정보 생성
     */
    public static Map<String, Object> createExcludedClassInfo(Object obj) {
        Map<String, Object> info = new HashMap<>();
        info.put("_type", "EXCLUDED_CLASS");
        info.put("_class", obj.getClass().getName());
        info.put("_simpleName", obj.getClass().getSimpleName());
        info.put("_toString", obj.toString());
        return info;
    }
    
    /**
     * 객체가 JTS Geometry 타입인지 확인
     */
    public static boolean isJTSGeometryType(Object obj) {
        if (obj == null) {
            return false;
        }
        
        String className = obj.getClass().getName();
        
        // JTS Geometry 클래스들 확인
        return className.contains("org.locationtech.jts.geom") ||
               className.contains("Point") && className.contains("geom") ||
               className.contains("Polygon") && className.contains("geom") ||
               className.contains("LineString") && className.contains("geom") ||
               className.contains("Geometry") && className.contains("jts");
    }
    
    /**
     * JTS Geometry 객체에서 중요 정보를 추출 (순환 참조 방지)
     */
    public static Map<String, Object> extractJTSGeometryInfo(Object geometry) {
        Map<String, Object> info = new HashMap<>();
        info.put("_type", "JTS_Geometry");
        info.put("_class", geometry.getClass().getSimpleName());
        
        try {
            // Point 객체인 경우 좌표 정보 추출
            if (geometry.getClass().getSimpleName().contains("Point")) {
                try {
                    Object x = geometry.getClass().getMethod("getX").invoke(geometry);
                    Object y = geometry.getClass().getMethod("getY").invoke(geometry);
                    info.put("x", x);
                    info.put("y", y);
                    info.put("longitude", x);
                    info.put("latitude", y);
                } catch (Exception e) {
                    info.put("coordinates", "좌표 추출 실패: " + e.getMessage());
                }
            }
            
            // SRID (좌표계) 정보 추출
            try {
                Object srid = geometry.getClass().getMethod("getSRID").invoke(geometry);
                info.put("srid", srid);
            } catch (Exception e) {
                info.put("srid", "SRID 추출 실패");
            }
            
            // WKT (Well-Known Text) 형식으로 변환 시도
            try {
                Object wkt = geometry.getClass().getMethod("toText").invoke(geometry);
                info.put("wkt", wkt);
            } catch (Exception e) {
                info.put("wkt", "WKT 변환 실패");
            }
            
            // 기하학적 속성들
            try {
                Object isEmpty = geometry.getClass().getMethod("isEmpty").invoke(geometry);
                info.put("isEmpty", isEmpty);
            } catch (Exception e) {
                // 무시
            }
            
            try {
                Object isValid = geometry.getClass().getMethod("isValid").invoke(geometry);
                info.put("isValid", isValid);
            } catch (Exception e) {
                // 무시
            }
            
        } catch (Exception e) {
            info.put("error", "JTS Geometry 정보 추출 실패: " + e.getMessage());
            info.put("toString", geometry.toString());
        }
        
        return info;
    }
    
    /**
     * 객체가 MultipartFile 타입인지 확인
     */
    public static boolean isMultipartFileType(Object obj) {
        if (obj == null) {
            return false;
        }
        
        // 클래스 이름으로 확인
        String className = obj.getClass().getName();
        if (className.contains("MultipartFile")) {
            return true;
        }
        
        // 인터페이스로 확인
        for (Class<?> iface : obj.getClass().getInterfaces()) {
            if (iface.getName().contains("MultipartFile")) {
                return true;
            }
        }
        
        // toString()으로 확인
        String toString = obj.toString();
        return toString.contains("MultipartFile");
    }
    
    /**
     * 객체를 안전하게 Map으로 변환
     */
    public static Map<String, Object> createSafeMap(Object obj) {
        if (obj == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("_class", obj.getClass().getName());
        result.put("_toString", obj.toString());
        
        // 리플렉션을 사용하여 필드 값 추출 시도
        try {
            for (Field field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();
                
                try {
                    Object value = field.get(obj);
                    
                    // 특수한 필드 타입 처리
                    if (value == null) {
                        result.put(fieldName, null);
                    } else if (value instanceof InputStream) {
                        result.put(fieldName, "[InputStream]");
                    } else if (isMultipartFileType(value)) {
                        result.put(fieldName, extractMultipartFileInfo(value));
                    } else if (isJTSGeometryType(value)) {
                        result.put(fieldName, extractJTSGeometryInfo(value));
                    } else if (value instanceof Collection) {
                        result.put(fieldName, "[Collection: " + ((Collection<?>) value).size() + " items]");
                    } else if (value.getClass().isArray()) {
                        try {
                            Object[] array = (Object[]) value;
                            result.put(fieldName, "[Array: " + array.length + " items]");
                        } catch (ClassCastException e) {
                            result.put(fieldName, "[Primitive Array]");
                        }
                    } else if (value instanceof Map) {
                        result.put(fieldName, "[Map: " + ((Map<?, ?>) value).size() + " entries]");
                    } else {
                        // 기본 타입이나 String은 직접 포함
                        result.put(fieldName, value);
                    }
                } catch (Exception e) {
                    result.put(fieldName, "[접근 불가: " + e.getMessage() + "]");
                }
            }
        } catch (Exception e) {
            result.put("_error", "필드 추출 실패: " + e.getMessage());
        }
        
        return result;
    }
}
