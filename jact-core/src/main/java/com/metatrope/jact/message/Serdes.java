package com.metatrope.jact.message;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer/deserializer for objects that are persisted to a message queue.
 */
public class Serdes {
    private final ObjectMapper mapper;

    @SuppressWarnings("serial")
    public class EnvelopeSerializer extends StdSerializer<Envelope<?>> {

        @SuppressWarnings("unchecked")
        public EnvelopeSerializer() {
            this((Class<Envelope<?>>) (Object) Envelope.class);
        }

        public EnvelopeSerializer(Class<Envelope<?>> t) {
            super(t);
        }

        @Override
        public void serialize(Envelope<?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeStartObject();
            jgen.writeStringField("id", value.getMessageId());
            jgen.writeStringField("messageType", value.getMessageType().name());
            jgen.writeStringField("senderId", value.getSenderId());
            jgen.writeStringField("receiverId", value.getReceiverId());
            jgen.writeObjectField("payload", value.getPayload());
            jgen.writeStringField("payloadType", value.getPayload() != null ? value.getPayloadType().getName() : null);
            jgen.writeObjectField("ctx", value.getContext());
            jgen.writeEndObject();
        }
    }

    @SuppressWarnings("serial")
    public class EnvelopeDeserializer extends StdDeserializer<Envelope<?>> {

        @SuppressWarnings("unchecked")
        public EnvelopeDeserializer() {
            this((Class<Envelope<?>>) (Object) Envelope.class);
        }

        public EnvelopeDeserializer(Class<Envelope<?>> t) {
            super(t);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Envelope<?> deserialize(JsonParser parser, DeserializationContext ctxt) throws JsonProcessingException, IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            MessageType messageType = MessageType.valueOf(node.get("messageType").asText());
            String messageId = node.get("id").asText();
            String senderId = asNullableString(node, "senderId");
            String receiverId = asNullableString(node, "receiverId");
            JsonNode payloadNode = node.get("payload");
            String payloadType = asNullableString(node, "payloadType");
            Object payload = null;
            if (payloadType != null) {
                try {
                    payload = mapper.convertValue(payloadNode, Class.forName(payloadType));
                } catch (IllegalArgumentException | ClassNotFoundException e) {
                    throw new IOException(e);
                }
            }
            Map<String, String> additionalParams = null;
            JsonNode additionalParamsNode = node.get("ctx");
            if (!additionalParamsNode.isNull()) {
                ObjectMapper mapper = new ObjectMapper();
                additionalParams = (Map<String, String>) mapper.convertValue(additionalParamsNode, Map.class);
            }
            return MessageFactory.create(messageType, messageId, senderId, receiverId, payload, additionalParams);
        }

        private String asNullableString(JsonNode node, String childAttribute) {
            return (node.get(childAttribute).isNull()) ? null : node.get(childAttribute).asText();
        }
    }

    public Serdes() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(new EnvelopeSerializer());
        module.addDeserializer(Envelope.class, new EnvelopeDeserializer());
        mapper.registerModule(module);
        this.mapper = mapper;
    }

    public String serialize(Envelope<?> envelope) {
        try {
            return mapper.writeValueAsString(envelope);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Envelope<?> deserialize(String messageBody) {
        try {
            return mapper.readValue(messageBody, Envelope.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
