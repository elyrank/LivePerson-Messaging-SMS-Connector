package com.liveperson.tutorial.ws.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.liveperson.tutorial.ws.util.JsonUtil;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.websocket.DecodeException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author elyran
 * @since 11/15/16.
 */
@Component
public class SmsSender {

    private static final Logger logger = LoggerFactory.getLogger(SmsSender.class);
    public static final String PREFIX = "@@LP@@";

    @Value("${lp.messaging.sms.url}")
    private String urlFormat;


    @Value("${telestax.AuthToken}")
    private String authToken;


    @Value("${telestax.AccountSid}")
    private String accountSid;


    @Autowired
    RestTemplate restTemplate;

    @Value("${telestax.phone.number}")
    private String smsConnectorNumber;

    public void sendSms(String phoneNumber, String msg) {
        final String url = String.format(urlFormat, accountSid);
        String formattedMsg = msg;
        if (msg.startsWith(PREFIX)) {
            formattedMsg = formatMsg(msg);
        }
        final JsonNode responseNode = postObject(url, formattedMsg, smsConnectorNumber, phoneNumber);
    }

    private String formatMsg(String msg) {
        final String jsonStr = msg.substring(PREFIX.length());
        try {
            final JsonNode node = JsonUtil.CODEC.decode(jsonStr);
            final String content = node.get("content").asText();
            final List<String> texts = node.findValuesAsText("text");
            StringBuilder sb = new StringBuilder();
            sb.append(content).append("\n");
            for (String text : texts) {
                sb.append("\n").append(text);
            }
            return sb.toString();
        } catch (DecodeException e) {
            e.printStackTrace();
        }
        return msg;
    }


    public JsonNode postObject(String url, String message, String from, String to) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        try {
            map.add("From", URLEncoder.encode(from, "UTF-8"));
            map.add("To", URLEncoder.encode(to, "UTF-8"));
            map.add("Body", URLEncoder.encode(message, "UTF-8"));
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            headers.add(HttpHeaders.AUTHORIZATION, "Basic " + getAuthorizationToken("jean.deruelle@gmail.com", "restcomm2016"));
            final ResponseEntity<JsonNode> exchange = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(map, headers), JsonNode.class);
             logger.info("sent SMS {}", map);
             logger.info("SMS response {}", exchange.getBody());
            return exchange.getBody();
        } catch (Exception e) {
            logger.error("failed to send sms: ", e);
        }
        return null;
    }

    private String getAuthorizationToken(String username, String password) {
        byte[] usernamePassBytes = (username + ":" + password).getBytes(Charset.forName("UTF-8"));
        return Base64.encode(usernamePassBytes);
    }
}
