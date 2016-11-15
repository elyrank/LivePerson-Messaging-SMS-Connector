package com.liveperson.tutorial.ws.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.liveperson.tutorial.ws.util.RestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author elyran
 * @since 11/15/16.
 */
@Component
public class SmsManager {
    private Map<String, SmsConsumer> phoneToConsumer = new ConcurrentHashMap<>();
    private Map<String, String> convToPhone = new ConcurrentHashMap<>();


    @Value("${lp.idp.uri}")
    private String idpUriFormat;

    @Value("${lp.idp.domain}")
    private String idpDomain;

    @Value("${lp.messaging.accountId}")
    protected String accountId;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ApplicationContext context;

    public String getPhone(String convId) {
        return convToPhone.get(convId);
    }

    public void addPhone(String convId, String phone) {
        convToPhone.put(convId, phone);
    }

    public SmsConsumer getOrComputeConsumer(String phoneNumber) {
        return phoneToConsumer.computeIfAbsent(phoneNumber, p -> context.getBean(SmsConsumer.class, p));
    }



}
