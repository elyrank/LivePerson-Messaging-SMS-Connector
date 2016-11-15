package com.liveperson.tutorial.ws.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author elyran
 * @since 11/15/16.
 */
@RestController
@RequestMapping(("sms"))
public class SmsMessageController {

    private static final Logger logger = LoggerFactory.getLogger(SmsMessageController.class);
    @Autowired
    private SmsManager smsManager;


    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> handle(@RequestParam("Body") String message, @RequestParam("From") String phoneNumber) {
        try {
            final SmsConsumer consumer = smsManager.getOrComputeConsumer(phoneNumber);
            consumer.sendMessage(message);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("failed to handle message ",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



}
