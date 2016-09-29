package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import org.springframework.cloud.aws.messaging.config.annotation.NotificationMessage;
import org.springframework.cloud.aws.messaging.config.annotation.NotificationSubject;
import org.springframework.cloud.aws.messaging.endpoint.NotificationStatus;
import org.springframework.cloud.aws.messaging.endpoint.annotation.NotificationMessageMapping;
import org.springframework.cloud.aws.messaging.endpoint.annotation.NotificationSubscriptionMapping;
import org.springframework.cloud.aws.messaging.endpoint.annotation.NotificationUnsubscribeConfirmationMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

/**
 *  Handle life-cycle events from Amazon. Closes the app in response to termination events.
 */
@Controller
@RequestMapping("/cs-media-shutdown-hook")
public class TerminationController {

    private static final FormattedLogger LOGGER = new FormattedLogger(TerminationController.class);

    @NotificationSubscriptionMapping
    public void handleSubscriptionMessage(NotificationStatus status) throws IOException {
        LOGGER.info("TERMINATION - Notification subscribed");
        status.confirmSubscription();
    }

    @NotificationMessageMapping
    public void handleNotificationMessage(@NotificationSubject String subject, @NotificationMessage String message) {
        LOGGER.info("TERMINATION - subject={}, message={}", subject, message);
        //System.exit(0);
    }

    @NotificationUnsubscribeConfirmationMapping
    public void handleUnsubscribeMessage(NotificationStatus status) {
        LOGGER.info("TERMINATION - Notification un-subscribed");
        status.confirmSubscription();
    }

}
