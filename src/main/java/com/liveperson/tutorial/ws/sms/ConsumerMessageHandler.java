package com.liveperson.tutorial.ws.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.liveperson.tutorial.ws.client.JsonMessageHandler;
import com.liveperson.tutorial.ws.client.WsClient;
import com.liveperson.tutorial.ws.util.Requests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author elyran
 * @since 10/26/16.
 */
public class ConsumerMessageHandler implements JsonMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerMessageHandler.class);

    private SmsConsumer smsConsumer;
    private WsClient wsClient;

    public ConsumerMessageHandler(SmsConsumer smsConsumer, WsClient wsClient) {
        this.smsConsumer = smsConsumer;
        this.wsClient = wsClient;
    }

    private AtomicInteger reqId = new AtomicInteger(1);

    private Map<String, Conversation> conversations = new ConcurrentHashMap<>();


    public void init() {
        wsClient.send(Requests.getUserProfile(reqId.incrementAndGet()));
    }

    @Override
    public void onMessage(JsonNode node) {
        final String type = node.get("type").asText();
        switch (type) {

            case Requests.GET_USER_PROFILE + Requests.RESPONSE:
                handleGetUserProfile(node);
                break;
            case Requests.EX_CONVERSATION_CHANGE_NOTIFICATION:
                handleConversations(node);
                break;
            case Requests.ONLINE_EVENT_DISTRIBUTION:
                handleMessagingEvents(node);
                break;

        }
    }

    private void handleMessagingEvents(JsonNode node) {
        final JsonNode body = node.get("body");
        final String convId = body.get("dialogId").asText();
        final JsonNode seqNode = body.get("sequence");
        final Conversation conversation = conversations.get(convId);
        if (seqNode != null && conversation != null && conversation.compareAndSetSequence(seqNode.asInt())) {
            final int sequence = seqNode.asInt();
            //check if it is my message - and respond only if not
            if (!body.get("originatorId").asText().equals(smsConsumer.consumerId)) {
                sendRead(convId, sequence);
                sendAccept(convId, sequence);
                final JsonNode event = body.get("event");
                final String eventType = event.get("type").asText();
                if (eventType.equals("ContentEvent")) {
                    final String message = event.get("message").asText();
                    sendSms(convId, message);
                }
            }
        }
    }

    private void handleGetUserProfile(JsonNode node) {
        smsConsumer.setConsumerId(node.findPath("userId").asText());
        wsClient.send(Requests.subscribeConversationsList(smsConsumer.consumerId, new String[]{"OPEN"}, reqId.incrementAndGet(), smsConsumer.accountId));
    }


    private void sendSms(String convId, String message) {
        final String response = message.trim();
        smsConsumer.sendSms(convId, response);

    }

    private void sendAccept(String convId, int sequence) {
        Requests.publishAcceptStatusEvent(convId, "ACCEPT", new int[]{sequence}, reqId.incrementAndGet());
    }

    private void sendRead(String convId, int sequence) {
        Requests.publishAcceptStatusEvent(convId, "READ", new int[]{sequence}, reqId.incrementAndGet());
    }

    private void handleConversations(JsonNode node) {
        final JsonNode changes = node.findPath("changes");
        if (changes.isArray()) {
            for (JsonNode change : changes) {
                final String type = change.get("type").asText();
                final String convId = change.findPath("convId").asText();
                if (type.equals("UPSERT")) {
                    final Conversation conversation = new Conversation();
                    final Conversation existing = conversations.putIfAbsent(convId, conversation);
                    if (existing == null) {
                        final int sequence = change.findPath("sequence").asInt();
                        conversation.compareAndSetSequence(sequence);
                        smsConsumer.setConvId(convId);
                    }
                } else if (type.equals("DELETE")) {
                    conversations.remove(convId);
                }

            }

        }
    }


}
