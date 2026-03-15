package com.delta.ingestion.testutils;

public class TestDataFactory {
    public static String createJsonCustomer(String id, String email) {
        return String.format(
                "{\"external_id\":\"%s\",\"name\":\"Test User\",\"email\":\"%s\",\"country_code\":\"US\",\"status_code\":\"ACTIVE\"}",
                id, email
        );
    }

    public static String createJsonArray(int count) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            sb.append(createJsonCustomer("EXT_" + i, "test" + i + "@delta.com"));
            if (i < count - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }
}
