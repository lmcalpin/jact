package com.metatrope.jact.message;

import com.metatrope.jact.ActorRef;

import java.util.Map;

public class MessageFactory {
    public static <T, R> Envelope<T> tell(ActorRef<T, R> sender, ActorRef<T, R> recipient, T payload) {
        return new Envelope<T>(MessageType.TELL, sender, recipient, payload);
    }

    public static <T, R> Envelope<T> tell(ActorRef<T, R> sender, ActorRef<T, R> recipient, T payload, Map<String, String> ctx) {
        return new Envelope<T>(MessageType.TELL, sender, recipient, payload, ctx);
    }

    public static <T, R> Envelope<T> tell(String senderId, String recipientId, T payload) {
        return new Envelope<T>(MessageType.TELL, senderId, recipientId, payload);
    }

    public static <T, R> Envelope<T> ask(ActorRef<T, R> sender, ActorRef<T, R> recipient, T payload) {
        return new Envelope<T>(MessageType.ASK, sender, recipient, payload);
    }

    public static <T, R> Envelope<T> ask(ActorRef<T, R> sender, ActorRef<T, R> recipient, T payload, Map<String, String> ctx) {
        return new Envelope<T>(MessageType.ASK, sender, recipient, payload, ctx);
    }

    public static <T, R> Envelope<T> ask(String senderId, String recipientId, T payload) {
        return new Envelope<T>(MessageType.ASK, senderId, recipientId, payload);
    }

    public static <T, R> Envelope<R> reply(Envelope<T> envelope, R payload) {
        return new Envelope<R>(MessageType.REPLY, envelope.getReceiverId(), envelope.getSenderId(), payload, Map.of("replyToId", envelope.getMessageId()));
    }

    public static <T, R> Envelope<T> forward(ActorRef<T, R> recipient, Envelope<T> envelope) {
        addContext(envelope, "forwarded-by", envelope.getReceiverId());
        return new Envelope<T>(envelope.getMessageType(), envelope.getSenderId(), recipient.getName(), envelope.getPayload(), envelope.getContext());
    }

    public static <T, R> Envelope<T> forward(ActorRef<T, R> recipient, Envelope<T> envelope, Map<String, String> ctx) {
        Envelope<T> forwardedEnvelope = forward(recipient, envelope);
        forwardedEnvelope.getContext().putAll(ctx);
        return forwardedEnvelope;
    }

    public static <T, R> Envelope<T> forward(String recipientId, Envelope<T> envelope) {
        return new Envelope<T>(envelope.getMessageType(), envelope.getSenderId(), recipientId, envelope.getPayload(), envelope.getContext());
    }

    public static <T, R> Envelope<T> relay(String proxySenderId, String localReceiverId, Envelope<T> envelope) {
        return new Envelope<T>(envelope.getMessageType(), proxySenderId, localReceiverId, envelope.getPayload(), envelope.getContext());
    }

    public static <T, R> Envelope<T> poison(ActorRef<T, R> recipient) {
        return new Envelope<T>(MessageType.POISONPILL, null, recipient.getName(), null);
    }

    public static <T, R> Envelope<T> poison(String recipientId) {
        return new Envelope<T>(MessageType.POISONPILL, null, recipientId, null);
    }

    public static <T> Envelope<T> system(String actorSystemName, String recipientId, T message) {
        return new Envelope<T>(MessageType.SYSTEM, String.format("%s/$", actorSystemName), recipientId, message);
    }

    static <T> Envelope<T> create(MessageType type, String messageId, String senderId, String receiverId, T payload, Map<String, String> ctx) {
        Envelope<T> envelope = new Envelope<T>(type, messageId, senderId, receiverId, payload, ctx);
        return envelope;
    }

    private static <T> void addContext(Envelope<T> envelope, String key, String newValue) {
        String prevValue = envelope.getContext().get(key);
        if (prevValue != null) {
            envelope.getContext().put("prev-" + key, prevValue);
        }
        envelope.getContext().put(key, newValue);
    }
}
