package com.garyzhangscm.cwms.allocationwire;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.*;

/** Per-response dictionary; no candidate filtering, sorting or stock caching. */
public final class CandidateWireCodec {
    public static final String FORMAT = "allocation-candidates-v1";
    private static final String CONTEXT = CandidateWireCodec.class.getName();
    private static final List<String> FIELDS = Arrays.asList("item", "itemPackageType", "inventoryStatus");
    private final ObjectMapper source;
    private final ObjectMapper rowMapper;

    public CandidateWireCodec(ObjectMapper source, Class<?> inventoryType) {
        this.source = source;
        // Never change the application's mapper or its legacy API serialization.
        this.rowMapper = source.copy().addMixIn(inventoryType, ReferenceFields.class);
    }

    public ObjectNode encode(List<?> inventories) throws IOException {
        Dictionary context = new Dictionary(source);
        JsonNode records = source.readTree(rowMapper.writer().withAttribute(CONTEXT, context).writeValueAsBytes(inventories));
        ObjectNode envelope = source.createObjectNode();
        envelope.put("format", FORMAT);
        envelope.set("records", records);
        ObjectNode metadata = envelope.putObject("metadata");
        for (String field : FIELDS) metadata.set(field, context.values.get(field));
        return envelope;
    }

    public <T> List<T> decode(JsonNode envelope, Class<T> inventoryType) throws IOException {
        if (envelope == null || !FORMAT.equals(envelope.path("format").asText())
                || !envelope.path("records").isArray() || !envelope.path("metadata").isObject()) {
            throw new IOException("Unsupported or malformed allocation candidate response");
        }
        for (String field : FIELDS) {
            if (!envelope.path("metadata").path(field).isArray()) throw new IOException("Missing candidate metadata: " + field);
        }
        List<T> inventories = new ArrayList<>();
        for (JsonNode record : envelope.get("records")) {
            if (!record.isObject()) throw new IOException("Invalid candidate record");
            ObjectNode restored = record.deepCopy();
            for (String field : FIELDS) {
                JsonNode reference = record.get(field);
                // Preserve null and omitted properties exactly.
                if (reference == null || reference.isNull()) continue;
                JsonNode values = envelope.get("metadata").get(field);
                if (!reference.isIntegralNumber() || !reference.canConvertToInt()
                        || reference.intValue() < 0 || reference.intValue() >= values.size()
                        || !values.get(reference.intValue()).isObject()) {
                    throw new IOException("Invalid candidate metadata reference: " + field);
                }
                restored.set(field, values.get(reference.intValue()));
            }
            // A fresh POJO graph for every row preserves legacy mutation isolation.
            inventories.add(source.treeToValue(restored, inventoryType));
        }
        return inventories;
    }

    private abstract static class ReferenceFields {
        @JsonSerialize(using = ReferenceSerializer.class) public abstract Object getItem();
        @JsonSerialize(using = ReferenceSerializer.class) public abstract Object getItemPackageType();
        @JsonSerialize(using = ReferenceSerializer.class) public abstract Object getInventoryStatus();
    }

    public static final class ReferenceSerializer extends StdSerializer<Object> implements ContextualSerializer {
        private final String field;
        public ReferenceSerializer() { this(null); }
        private ReferenceSerializer(String field) { super(Object.class); this.field = field; }
        @Override public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property) {
            return new ReferenceSerializer(property.getName());
        }
        @Override public void serialize(Object value, JsonGenerator output, SerializerProvider provider) throws IOException {
            Dictionary context = (Dictionary) provider.getAttribute(CONTEXT);
            if (context == null) throw new IOException("Candidate dictionary is unavailable");
            output.writeNumber(context.reference(field, value));
        }
    }

    private static final class Dictionary {
        private final ObjectMapper mapper;
        private final Map<String, ArrayNode> values = new LinkedHashMap<>();
        private final Map<String, Map<JsonNode, Integer>> byValue = new HashMap<>();
        private final Map<String, IdentityHashMap<Object, Integer>> byIdentity = new HashMap<>();
        Dictionary(ObjectMapper mapper) {
            this.mapper = mapper;
            for (String field : FIELDS) {
                values.put(field, mapper.createArrayNode());
                byValue.put(field, new HashMap<>());
                byIdentity.put(field, new IdentityHashMap<>());
            }
        }
        int reference(String field, Object value) {
            Integer index = byIdentity.get(field).get(value);
            if (index != null) return index;
            JsonNode node = mapper.valueToTree(value);
            // Compare complete serialized metadata, never only the database ID.
            index = byValue.get(field).get(node);
            if (index == null) {
                index = values.get(field).size();
                values.get(field).add(node);
                byValue.get(field).put(node, index);
            }
            byIdentity.get(field).put(value, index);
            return index;
        }
    }
}
