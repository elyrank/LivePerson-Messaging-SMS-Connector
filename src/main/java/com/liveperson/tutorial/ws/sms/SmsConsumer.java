package com.liveperson.tutorial.ws.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.liveperson.tutorial.ws.client.WsClient;
import com.liveperson.tutorial.ws.util.Requests;
import com.liveperson.tutorial.ws.util.RestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author elyran
 * @since 10/20/16.
 */
@Component
@Scope("prototype")
public class SmsConsumer {

    private static final Logger logger = LoggerFactory.getLogger(SmsConsumer.class);
    private String phoneNumber;

    @Value("${lp.messaging.api.version}")
    private int version;

    @Value("${lp.messaging.accountId}")
    protected String accountId;

    @Value("${lp.messaging.uri.format}")
    private String uriFormat;

    @Value("${lp.idp.uri}")
    private String idpUriFormat;

    @Value("${lp.idp.domain}")
    private String idpDomain;

    @Value("${lp.msg.domain}")
    private String messagingDomain;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SmsSender smsSender;

    @Autowired
    private ScheduledExecutorService executorService;

    protected WsClient wsClient;
    private ConsumerMessageHandler consumerMessageHandler;
    @Autowired
    private RestTemplate restTemplate;
    private AtomicLong req = new AtomicLong();
    private String convId;

    protected String consumerId;

    public SmsConsumer(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public SmsConsumer() {
    }

    @PostConstruct
    public void init() {
        try {
            final String token = clientAuth();
            wsClient = context.getBean(WsClient.class);
            consumerMessageHandler = new ConsumerMessageHandler(this, wsClient);
            String uri = String.format(uriFormat, messagingDomain, accountId, token, version);
            //add logger handler
            wsClient.addMessageHandler(node -> logger.info("consumer  received message: {}", node.toString()));
            //add message handler
            wsClient.addMessageHandler(consumerMessageHandler);
            //add tasks to run after connection is opened
            wsClient.addOnOpenHandler(session -> consumerMessageHandler.init());
            wsClient.connect(uri);
            logger.info("consumer  wsClient connected successfully to uri {}", uri);
        } catch (Exception e) {
            logger.error("failed to initialize wsClient", e);
        }
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    private String clientAuth() {
        //do login to idp
        final String url = String.format(idpUriFormat, idpDomain, accountId);
        final JsonNode jsonNode = RestUtil.postJsonNode(url,"", restTemplate);
        return jsonNode.get("jwt").asText();
    }


    private void resolveConversation(String convId, int reqId) {
        wsClient.send(Requests.resolveConversation(convId, reqId));
    }

    protected void sendSms(String convId, String message) {
        smsSender.sendSms(phoneNumber, message);
    }

    public void sendMessage(String message) {
        if (convId == null) {
            wsClient.send(Requests.consumerRequestConversation(accountId, req.incrementAndGet()));
            //do not do that in production! just a workaround to wait for the conversation to be created
            executorService.schedule(() -> send(message), 1, TimeUnit.SECONDS);
        } else {
            send(message);
        }

    }

    private void send(String message) {
        wsClient.send(Requests.publishContentEvent(convId, message, req.incrementAndGet()));
    }

    protected void setConvId(String convId) {
        this.convId = convId;
    }
}
