package com.metatrope.jact.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.metatrope.jact.ActorSystem;
import com.metatrope.jact.remote.JoinMessage;
import com.metatrope.jact.remote.fake.FakeEndpoint;
import com.metatrope.jact.remote.fake.FakeTransport;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SerdesTest {
    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @MethodSource("serdesTestProvider")
    public void testSerialize(Envelope<?> env, String expected) throws IOException {
        Serdes serdes = new Serdes();
        String serializedAsString = serdes.serialize(env);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> m1 = (Map<String, Object>) mapper.readValue(expected, Map.class);
        Map<String, Object> m2 = (Map<String, Object>) mapper.readValue(serializedAsString, Map.class);
        assertEquals(m1, m2);
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @MethodSource("serdesTestProvider")
    public void testDeserialize(Envelope<?> expected, String serializedEnvelope) throws JsonMappingException, JsonProcessingException {
        Serdes serdes = new Serdes();
        Envelope<String> deserializedEnvelope = (Envelope<String>) serdes.deserialize(serializedEnvelope);
        assertEquals(expected.getMessageType(), deserializedEnvelope.getMessageType());
        assertEquals(expected.getMessageId(), deserializedEnvelope.getMessageId());
        assertEquals(expected.getSenderId(), deserializedEnvelope.getSenderId());
        assertEquals(expected.getReceiverId(), deserializedEnvelope.getReceiverId());
        assertEquals(expected.isAsk(), deserializedEnvelope.isAsk());
        assertEquals(expected.getPayload(), deserializedEnvelope.getPayload());
    }

    private static Stream<Arguments> serdesTestProvider() {
        var testActorSystem = new ActorSystem("test");
        var tell = MessageFactory.tell("sender", "receiver", "body");
        var ask = MessageFactory.ask("sender", "receiver", "body");
        var fwd = MessageFactory.forward("receiver2", tell);
        var reply = MessageFactory.reply(tell, "x");
        var poisonPill = MessageFactory.poison("receiver");
        var system = MessageFactory.system("local", "remote/$join", new JoinMessage(testActorSystem, new FakeTransport(testActorSystem, new FakeEndpoint())));
//@formatter:off        
        return Stream.of(
                Arguments.of(tell, String.format("""
                        {"id":"%s","messageType":"TELL","senderId":"sender","receiverId":"receiver","payload":"body","payloadType":"java.lang.String","ctx":{}}""", tell.getMessageId())), 
                Arguments.of(ask, String.format("""
                        {"id":"%s","messageType":"ASK","senderId":"sender","receiverId":"receiver","payload":"body","payloadType":"java.lang.String","ctx":{}}""", ask.getMessageId())), 
                Arguments.of(fwd, String.format("""
                        {"id":"%s","messageType":"TELL","senderId":"sender","receiverId":"receiver2","payload":"body","payloadType":"java.lang.String","ctx":{}}""", fwd.getMessageId())), 
                Arguments.of(reply, String.format("""
                        {"id":"%s","messageType":"REPLY","senderId":"receiver","receiverId":"sender","payload":"x","payloadType":"java.lang.String","ctx":{"replyToId":"%s"}}""", reply.getMessageId(), tell.getMessageId())),
                Arguments.of(poisonPill, String.format("""
                        {"id":"%s","messageType":"POISONPILL","senderId":null,"receiverId":"receiver","payload":null,"payloadType":null,"ctx":{}}""", poisonPill.getMessageId())), 
                Arguments.of(system, String.format("""
                        {"id":"%s","messageType":"SYSTEM","senderId":"local/$","receiverId":"remote/$join","payload":{"clientActorSystem":"test", "connectionParameters":null},"payloadType":"com.metatrope.jact.remote.JoinMessage","ctx":{}}""", system.getMessageId())));
//@formatter:on
    }
}
