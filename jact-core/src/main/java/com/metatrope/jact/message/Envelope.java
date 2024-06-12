package com.metatrope.jact.message;

import com.metatrope.jact.ActorRef;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A message sent to an <code>Actor</code>.
 */
public class Envelope<T> {
    private final String senderId;
    private final String receiverId;
    private final MessageType messageType;
    private final String messageId;
    private final T payload;
    private final Class<T> clazz;
    private final String originalSenderId;
    private Map<String, String> context;

    Envelope(MessageType messageType, ActorRef<?, ?> sender, ActorRef<T, ?> receiver, T payload) {
        this(messageType, actorId(sender), actorId(receiver), payload);
    }

    Envelope(MessageType messageType, String senderId, String receiverId, T payload) {
        this(messageType, UUID.randomUUID().toString(), senderId, receiverId, payload, null);
    }

    Envelope(MessageType messageType, String senderId, String receiverId, T payload, Map<String, String> context) {
        this(messageType, UUID.randomUUID().toString(), senderId, receiverId, payload, context);
    }

    Envelope(MessageType messageType, ActorRef<?, ?> sender, ActorRef<T, ?> receiver, T payload, Map<String, String> context) {
        this(messageType, actorId(sender), actorId(receiver), payload, context);
    }

    @SuppressWarnings("unchecked")
    Envelope(MessageType messageType, String messageId, String senderId, String receiverId, T payload, Map<String, String> context) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageType = messageType;
        this.messageId = messageId;
        this.payload = payload;
        this.clazz = payload != null ? (Class<T>) payload.getClass() : null;
        this.originalSenderId = senderId;
        this.context = context;
    }

    private static String actorId(ActorRef<?, ?> ref) {
        if (ref == null)
            return null;
        return ref.getName();
    }

    public String getSenderId() {
        return senderId;
    }

    public String getOriginalSenderId() {
        return originalSenderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getMessageId() {
        return messageId;
    }

    public T getPayload() {
        return payload;
    }

    public Class<T> getPayloadType() {
        return clazz;
    }

    public String getReplyToId() {
        return this.context.get("replyToId");
    }

    public boolean isAsk() {
        return getMessageType() == MessageType.ASK || getMessageType() == MessageType.SYSTEM;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public Map<String, String> getContext() {
        if (context == null) {
            context = new HashMap<>();
        }
        return context;
    }

    @Override
    public String toString() {
        return String.format("Message:: id=%s type=%s from=%s to=%s ctx=%s", messageId, messageType, senderId, receiverId, context);
    }
}
