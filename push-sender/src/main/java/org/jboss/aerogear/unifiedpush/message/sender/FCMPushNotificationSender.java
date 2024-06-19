/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.unifiedpush.message.sender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import org.jboss.aerogear.unifiedpush.api.AndroidVariant;
import org.jboss.aerogear.unifiedpush.api.Variant;
import org.jboss.aerogear.unifiedpush.api.VariantType;
import org.jboss.aerogear.unifiedpush.message.InternalUnifiedPushMessage;
import org.jboss.aerogear.unifiedpush.message.Priority;
import org.jboss.aerogear.unifiedpush.message.UnifiedPushMessage;
import org.jboss.aerogear.unifiedpush.message.apns.APNs;
import org.jboss.aerogear.unifiedpush.message.token.TokenLoaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Message.Builder;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
@Service
@Qualifier(value = VariantType.ANDROIDQ)
public class FCMPushNotificationSender implements PushNotificationSender {

    private FirebaseApp app;
    private FirebaseMessaging firebaseMessaging;
    @PostConstruct
    public void init() {
        FirebaseOptions options;
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            logger.info("init, inside try cardentials{}==========", credentials);
            options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId("c-retail") // Set your Firebase project ID here
                    .setDatabaseUrl("https://c-retail.firebaseio.com")
                    .build();
        } catch (IOException e) {
            logger.info("init, inside catch e={}=========", e.getMessage());
            throw new RuntimeException(e);
        }

        // Initialize FirebaseApp with the FirebaseOptions
        this.app = FirebaseApp.initializeApp(options, "cretail");
        this.firebaseMessaging = FirebaseMessaging.getInstance(this.app);
        logger.info("init, options={} firebaseMessaging={} FirebaseApp={}=========", options, this.firebaseMessaging, this.app);
    }

    private final Logger logger = LoggerFactory.getLogger(FCMPushNotificationSender.class);

    /**
     * Sends FCM notifications ({@link UnifiedPushMessage}) to all devices, that are represented by
     * the {@link List} of tokens for the given {@link AndroidVariant}.
     */
    @Override
    public void sendPushMessage(Variant variant, Collection<String> tokens, UnifiedPushMessage pushMessage, String pushMessageInformationId, NotificationSenderCallback callback) {
        logger.info("sendPushMessage, firebaseMessaging={} FirebaseApp={}=========", firebaseMessaging, app);
        // no need to send empty list
        if (tokens.isEmpty()) {
            logger.info("sendPushMessage, token is empty=========");
            return;
        }

        final List<String> pushTargets = new ArrayList<>(tokens);
        org.jboss.aerogear.unifiedpush.message.Message message = pushMessage.getMessage();
        final APNs apns = message.getApns();
        final String msgTitle = (apns != null) ? apns.getTitle() : "";
        Notification notification = Notification.builder()
                .setBody(message.getAlert())
                .setTitle(msgTitle)
                .build();

           /*
        The Message defaults to a Normal priority.  High priority is used
        by FCM to wake up devices in Doze mode as well as apps in AppStandby
        mode.  This has no effect on devices older than Android 6.0
        */

        com.google.firebase.messaging.AndroidConfig.Priority priority = message.getPriority() ==  Priority.HIGH ?
                AndroidConfig.Priority.HIGH :
                AndroidConfig.Priority.NORMAL;

        com.google.firebase.messaging.AndroidConfig.Builder androidConfig = AndroidConfig.builder()
                .setPriority(priority);

        // if present, apply the time-to-live metadata:
        int ttl = pushMessage.getConfig().getTimeToLive();
        if (ttl != -1) {
            androidConfig.setTtl(ttl);
        }


        // push targets can be registration IDs OR topics (starting /topic/), but they can't be mixed.
        if (pushTargets.get(0).startsWith(TokenLoaderUtils.TOPIC_PREFIX)) {
            logger.info("sendPushMessage, start with topic=========");
            Builder firebaseMessageBuilder = prepareFireBaseMessage(message, notification, androidConfig, pushMessageInformationId);
            logger.info("sendPushMessage, firebaseMessageBuilder={}=========", firebaseMessageBuilder);
            // send out a message to a batch of devices...
            processFCM(pushTargets, firebaseMessageBuilder, callback);
            return;
        }

        logger.info("sendPushMessage, start with multicastBuilder=========");
        MulticastMessage.Builder multicastBuilder = prepareFireBaseMulticastMessage(message, notification, androidConfig, pushMessageInformationId);
        logger.info("sendPushMessage, multicast={}=========", multicastBuilder);
        processFirebaseMultiCast(pushTargets, multicastBuilder, callback);
    }

    private Builder prepareFireBaseMessage(org.jboss.aerogear.unifiedpush.message.Message message, Notification notification, AndroidConfig.Builder androidConfig, String pushMessageInformationId) {
        // payload builder:
        Builder fcmBuilder = Message.builder();

        // add the "recognized" keys...
        fcmBuilder.putData("alert", message.getAlert());
        fcmBuilder.putData("sound", message.getSound());
        fcmBuilder.putData("badge", String.valueOf(message.getBadge()));
        fcmBuilder.setNotification(notification);


        fcmBuilder.setAndroidConfig(androidConfig.build());

        // iterate over the missing keys:
        message.getUserData().keySet()
                .forEach(key -> fcmBuilder.putData(key, String.valueOf(message.getUserData().get(key))));

        //add the aerogear-push-id
        fcmBuilder.putData(InternalUnifiedPushMessage.PUSH_MESSAGE_ID, pushMessageInformationId);
        return fcmBuilder;
    }
    private MulticastMessage.Builder prepareFireBaseMulticastMessage(org.jboss.aerogear.unifiedpush.message.Message message, Notification notification, AndroidConfig.Builder androidConfig, String pushMessageInformationId) {
        // payload builder:
        MulticastMessage.Builder multicastBuilder = MulticastMessage.builder();

        // add the "recognized" keys...
        multicastBuilder.putData("alert", message.getAlert());
        multicastBuilder.putData("sound", message.getSound());
        multicastBuilder.putData("badge", String.valueOf(message.getBadge()));
        multicastBuilder.setNotification(notification);


        multicastBuilder.setAndroidConfig(androidConfig.build());

        // iterate over the missing keys:
        message.getUserData().keySet()
                .forEach(key -> multicastBuilder.putData(key, String.valueOf(message.getUserData().get(key))));

        //add the aerogear-push-id
        multicastBuilder.putData(InternalUnifiedPushMessage.PUSH_MESSAGE_ID, pushMessageInformationId);
        return multicastBuilder;

    }


    /**
     * Process the HTTP POST to the FCM infrastructure for the given list of registrationIDs.
     */
    private void processFCM(List<String> pushTargets, Builder firebaseMessageBuilder, NotificationSenderCallback callback) {
        // send it out.....
        try {
            logger.info("Sending transformed FCM payload: {}", firebaseMessageBuilder.build());

            for (String topic : pushTargets) {
                logger.info(String.format("Sent push notification to FCM topic: %s", topic));
                firebaseMessageBuilder.setTopic(topic);
                String result = firebaseMessaging.send(firebaseMessageBuilder.build());
                //SendResponse result = sender.sendNoRetry(fcmBuilder, topic);
                logger.info("Response from FCM topic request======= {}", result);
            }
            logger.debug("Message batch to FCM has been submitted");
            callback.onSuccess();
        } catch (Exception e) {
        // FCM exceptions:
        callback.onError(String.format("Error sending payload to FCM server: %s", e.getMessage()));
        }
    }

    private void processFirebaseMultiCast(List<String> pushTargets, MulticastMessage.Builder firebaseMulticastMessage, NotificationSenderCallback callback) {
        // send it out.....
        try {
            logger.info("Sending transformed FCM payload: {}", firebaseMulticastMessage);
            logger.info(String.format("Sent push notification to FCM Server for %d registrationIDs", pushTargets.size()));

            firebaseMulticastMessage.addAllTokens(pushTargets);


            BatchResponse multicastResult = firebaseMessaging.sendMulticast(firebaseMulticastMessage.build());
            logger.info("Response from FCM request: {}", multicastResult);
            // after sending, let's identify the inactive/invalid registrationIDs and trigger their deletion:
            handleMulticastResponses(multicastResult, pushTargets);
            callback.onSuccess();
        } catch (Exception e) {
            // FCM exceptions:
            callback.onError(String.format("Error sending payload to FCM server: %s", e.getMessage()));
        }
    }

    private void handleMulticastResponses(BatchResponse multicastResult, List<String> registrationIDs) {

        // get the FCM send responses for all of the client devices:
        final List<SendResponse> responses = multicastResult.getResponses();

        // read the responses:
        int i = 0;
        for (SendResponse response : responses) {
            // use the current index to access the individual responses
            logger.info("handleMulticastResponses, current multicast response========={}", response);
            FirebaseMessagingException exception = response.getException();
            MessagingErrorCode messagingErrorCode = exception.getMessagingErrorCode();
            if (messagingErrorCode != null) {
                logger.info("Processing {} error code from FCM response, for registration ID: {}", messagingErrorCode, registrationIDs.get(i));
            }
            i++;
        }
    }
}
