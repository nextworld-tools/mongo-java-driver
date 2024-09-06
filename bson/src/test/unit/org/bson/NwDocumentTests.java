/*
 * NEXTWORLD CONFIDENTIAL
 *
 * Nextworld
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Nextworld and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Nextworld
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Nextworld.
 *
 * Copyright 2017 (c) Nextworld - All rights reserved.
 */
package org.bson;

import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class NwDocumentTests {

    private Document document;

    @BeforeEach
    void setUp() {
        document = new Document();
    }

    @Test
    void testConstructors() {
        Document doc1 = new Document("key", "value");
        Assertions.assertEquals("value", doc1.get("key"));

        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", 2);
        Document doc2 = new Document(map);
        Assertions.assertEquals("value1", doc2.get("key1"));
        Assertions.assertEquals(2, doc2.get("key2"));
    }

    @Test
    void testAppend() {
        document.append("key1", "value1").append("key2", 2);
        Assertions.assertEquals("value1", document.get("key1"));
        Assertions.assertEquals(2, document.get("key2"));
    }

    @Test
    void testGet() {
        document.put("key", "value");
        Assertions.assertEquals("value", document.get("key", String.class));
        Assertions.assertEquals("default", document.get("nonexistent", "default"));
    }

    @Test
    void testGetEmbedded() {
        Document nestedDoc = new Document("nestedKey", "nestedValue");
        document.put("key", nestedDoc);
        Assertions.assertEquals("nestedValue", document.getEmbedded(Arrays.asList("key", "nestedKey"), String.class));
        Assertions.assertEquals("default", document.getEmbedded(Arrays.asList("nonexistent", "key"), "default"));
    }

    @Test
    void testGetInteger() {
        document.put("key", 42);
        Assertions.assertEquals(Integer.valueOf(42), document.getInteger("key"));
        Assertions.assertEquals(0, document.getInteger("nonexistent", 0));
    }

    @Test
    void testGetLong() {
        document.put("key", 42L);
        Assertions.assertEquals(Long.valueOf(42L), document.getLong("key"));
        Assertions.assertEquals(0L, document.getLong("nonexistent", 0L));
    }

    @Test
    void testGetDouble() {
        document.put("key", 42.0);
        Assertions.assertEquals(42.0, document.getDouble("key"), 0.001);
    }

    @Test
    void testGetString() {
        document.put("key", "value");
        Assertions.assertEquals("value", document.getString("key"));
        Assertions.assertEquals("default", document.getString("nonexistent", "default"));
    }

    @Test
    void testGetBoolean() {
        document.put("key", true);
        Assertions.assertTrue(document.getBoolean("key"));
        Assertions.assertFalse(document.getBoolean("nonexistent", false));
    }

    @Test
    void testGetObjectId() {
        ObjectId id = new ObjectId();
        document.put("key", id);
        Assertions.assertEquals(id, document.getObjectId("key"));
    }

    @Test
    void testGetDate() {
        Date date = new Date();
        document.put("key", date);
        Assertions.assertEquals(date, document.getDate("key"));
    }

    @Test
    void testGetList() {
        List<String> list = Arrays.asList("a", "b", "c");
        document.put("key", list);
        Assertions.assertEquals(list, document.getList("key", String.class));
        Assertions.assertEquals(Collections.emptyList(), document.getList("nonexistent", String.class, Collections.emptyList()));
    }

    @Test
    void testToJson() {
        document.put("key", "value");
        String json = document.toJson();
        Assertions.assertTrue(json.contains("\"key\""));
        Assertions.assertTrue(json.substring(json.indexOf("\"key\"") + "\"key\"".length()).contains("\"value\""));
    }

    @Test
    void testGetDocumentAsMap() {
        document.put("key", "value");
        LinkedHashMap<String, Object> map = document.getDocumentAsMap();
        Assertions.assertEquals("value", map.get("key"));
    }

    @Test
    void testSetDocumentAsMap() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("key", "value");
        document.setDocumentAsMap(map);
        Assertions.assertEquals("value", document.get("key"));
    }

    @Test
    void testGetDocument() {
        Document nestedDoc = new Document("nestedKey", "nestedValue");
        document.put("key", nestedDoc);
        Assertions.assertEquals(nestedDoc, document.getDocument("key"));
    }

    @Test
    void testGetDocumentList() {
        List<Document> docList = Arrays.asList(
                new Document("key1", "value1"),
                new Document("key2", "value2")
        );
        document.put("key", docList);
        Assertions.assertEquals(docList, document.getDocumentList("key"));
    }

    @Test
    void testGetBigDecimal() {
        BigDecimal bigDecimal = new BigDecimal("42.42");
        document.put("key", bigDecimal);
        Assertions.assertEquals(bigDecimal, document.getBigDecimal("key"));

        Decimal128 decimal128 = new Decimal128(bigDecimal);
        document.put("key128", decimal128);
        Assertions.assertEquals(bigDecimal, document.getBigDecimal("key128"));
    }

    @Test
    void testGenerateCheckSum() {
        long checksum1 = Document.generateCheckSum(42L);
        long checksum2 = Document.generateCheckSum(42);
        Assertions.assertEquals(checksum1, checksum2);

        long checksum3 = Document.generateCheckSum(new BigDecimal("42.00"));
        long checksum4 = Document.generateCheckSum(new Decimal128(new BigDecimal("42.00")));
        Assertions.assertEquals(checksum3, checksum4);
    }

    @Test
    void testGetCRC32Checksum() {
        byte[] bytes = "test".getBytes();
        long checksum = Document.getCRC32Checksum(bytes);
        Assertions.assertTrue(checksum != 0);
    }

    @Test
    void testMapMethods() {
        document.put("key1", "value1");
        document.put("key2", "value2");

        Assertions.assertEquals(2, document.size());
        Assertions.assertFalse(document.isEmpty());
        Assertions.assertTrue(document.containsKey("key1"));
        Assertions.assertTrue(document.containsValue("value1"));

        document.remove("key1");
        Assertions.assertFalse(document.containsKey("key1"));

        document.clear();
        Assertions.assertTrue(document.isEmpty());

        Map<String, String> someMap = new HashMap<String, String>() {{
            put("key1", "value1");
            put("key2", "value2");
            put("key3", "value3");
            put("key4", "value4");
        }};

        document.putAll(someMap);
        Assertions.assertEquals(4, document.size());

        Set<String> keySet = document.keySet();
        Assertions.assertTrue(keySet.contains("key3") && keySet.contains("key4"));

        Collection<Object> values = document.values();
        Assertions.assertTrue(values.contains("value3") && values.contains("value4"));

        Set<Map.Entry<String, Object>> entrySet = document.entrySet();
        Assertions.assertEquals(4, entrySet.size());
    }

    @Test
    void testEqualsAndHashCode() {
        Document doc1 = new Document("key", "value");
        Document doc2 = new Document("key", "value");
        Document doc3 = new Document("key", "different");

        Assertions.assertEquals(doc1, doc2);
        Assertions.assertNotEquals(doc1, doc3);
        Assertions.assertEquals(doc1.hashCode(), doc2.hashCode());
        Assertions.assertNotEquals(doc1.hashCode(), doc3.hashCode());
    }
}
