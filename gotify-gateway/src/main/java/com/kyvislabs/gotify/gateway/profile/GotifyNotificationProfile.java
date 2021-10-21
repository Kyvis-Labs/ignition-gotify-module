package com.kyvislabs.gotify.gateway.profile;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.inductiveautomation.ignition.alarming.common.notification.BasicNotificationProfileProperty;
import com.inductiveautomation.ignition.alarming.common.notification.NotificationProfileProperty;
import com.inductiveautomation.ignition.alarming.notification.AlarmNotificationProfile;
import com.inductiveautomation.ignition.alarming.notification.AlarmNotificationProfileRecord;
import com.inductiveautomation.ignition.alarming.notification.NotificationContext;
import com.inductiveautomation.ignition.common.TypeUtilities;
import com.inductiveautomation.ignition.common.WellKnownPathTypes;
import com.inductiveautomation.ignition.common.alarming.AlarmEvent;
import com.inductiveautomation.ignition.common.config.FallbackPropertyResolver;
import com.inductiveautomation.ignition.common.expressions.parsing.Parser;
import com.inductiveautomation.ignition.common.expressions.parsing.StringParser;
import com.inductiveautomation.ignition.common.model.ApplicationScope;
import com.inductiveautomation.ignition.common.model.values.QualifiedValue;
import com.inductiveautomation.ignition.common.sqltags.model.types.DataQuality;
import com.inductiveautomation.ignition.common.user.ContactInfo;
import com.inductiveautomation.ignition.common.user.ContactType;
import com.inductiveautomation.ignition.gateway.audit.AuditProfile;
import com.inductiveautomation.ignition.gateway.audit.AuditRecord;
import com.inductiveautomation.ignition.gateway.audit.AuditRecordBuilder;
import com.inductiveautomation.ignition.gateway.expressions.AlarmEventCollectionExpressionParseContext;
import com.inductiveautomation.ignition.gateway.expressions.FormattedExpressionParseContext;
import com.inductiveautomation.ignition.gateway.localdb.persistence.PersistenceSession;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.model.ProfileStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class GotifyNotificationProfile implements AlarmNotificationProfile {

    private final GatewayContext context;
    private String auditProfileName, profileName, serverUrl;
    private final ScheduledExecutorService executor;
    private volatile ProfileStatus profileStatus = ProfileStatus.UNKNOWN;
    private Logger logger;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GotifyNotificationProfile(final GatewayContext context,
                                     final AlarmNotificationProfileRecord profileRecord,
                                     final GotifyNotificationProfileSettings settingsRecord) {
        this.context = context;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.profileName = profileRecord.getName();
        this.serverUrl = settingsRecord.getServerUrl();

        this.logger = Logger.getLogger(String.format("Gotify.%s.Profile", this.profileName));

        try (PersistenceSession session = context.getPersistenceInterface().getSession(settingsRecord.getDataSet())) {
            auditProfileName = settingsRecord.getAuditProfileName();
        } catch (Exception e) {
            logger.error("Error retrieving notification profile details.", e);
        }

    }

    @Override
    public String getName() {
        return profileName;
    }

    @Override
    public Collection<NotificationProfileProperty<?>> getProperties() {
        return Lists.newArrayList(
                GotifyProperties.MESSAGE,
                GotifyProperties.THROTTLED_MESSAGE,
                GotifyProperties.TITLE,
                GotifyProperties.PRIORITY,
                GotifyProperties.CONTENT_TYPE,
                GotifyProperties.CLICK_URL,
                GotifyProperties.INTENT_URL,
                GotifyProperties.TEST_MODE
        );
    }

    @Override
    public ProfileStatus getStatus() {
        return profileStatus;
    }

    @Override
    public Collection<ContactType> getSupportedContactTypes() {
        return Lists.newArrayList(GotifyNotificationProfileType.GOTIFY);
    }

    @Override
    public void onShutdown() {
        executor.shutdown();
    }

    @Override
    public void onStartup() {
        profileStatus = ProfileStatus.RUNNING;
    }

    @Override
    public void sendNotification(final NotificationContext notificationContext) {
        executor.execute(() -> {
            Collection<ContactInfo> contactInfos =
                    Collections2.filter(notificationContext.getUser().getContactInfo(), new IsGotifyContactInfo());

            String message = evaluateStringExpression(notificationContext, GotifyProperties.MESSAGE);
            String title = evaluateStringExpression(notificationContext, GotifyProperties.TITLE);
            Integer priority = evaluateIntegerExpression(notificationContext,GotifyProperties.PRIORITY);
            boolean testMode = notificationContext.getOrDefault(GotifyProperties.TEST_MODE);
            Map extras = buildExtras(notificationContext);

            boolean success = true;
            if (testMode) {
                logger.info(
                        String.format("THIS PROFILE IS RUNNING IN TEST MODE. The following WOULD have been sent:\nMessage: %s, Title=%s",
                                message, title)
                );

                notificationContext.notificationDone();
                return;
            }

            try {
                HttpClient httpClient = HttpClient.newHttpClient();
                for (ContactInfo contactInfo : contactInfos) {
                    String token = contactInfo.getValue();
                    final var messageToSend = new Message(title,message,priority,extras);

                    logger.info(objectMapper.writeValueAsString(messageToSend));

                    logger.debug(
                            String.format("Attempting to send an alarm notification to %s via %s [message=%s, title=%s]",
                                    notificationContext.getUser(),
                                    token,
                                    message,
                                    title)
                    );

                    String gotifyUrl = String.format("%s/message?token=%s", serverUrl, token);
                    final var bodyData = objectMapper.writeValueAsString(messageToSend);
                    final var request = HttpRequest.newBuilder()
                            .uri(URI.create(gotifyUrl))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(bodyData))
                            .build();

                    try {
                        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                        if (!(response.statusCode() >= 200 && response.statusCode() <= 399)) {
                            logger.error("Error sending notification: status code=" + response.statusCode() + ", response=" + response.body());
                        }
                    } catch (IOException e) {
                        logger.error("Unable to send notification", e);
                        success = false;
                    } catch (InterruptedException e) {
                        logger.error("Unable to send notification", e);
                        success = false;
                    }

                    audit(success, String.format("Gotify message to %s", token), notificationContext);
                }
            } catch (IOException ex) {
                logger.error("Unable to send notification", ex);
            }

            notificationContext.notificationDone();
        });
    }

    private void audit(boolean success, String eventDesc, NotificationContext notificationContext) {
        logger.info("auditing to %s".format(auditProfileName));
        if (!StringUtils.isBlank(auditProfileName)) {
            try {
                AuditProfile p = context.getAuditManager().getProfile(auditProfileName);
                if (p == null) {
                    return;
                }
                List<AlarmEvent> alarmEvents = notificationContext.getAlarmEvents();
                for (AlarmEvent event : alarmEvents) {
                    AuditRecord r = new AuditRecordBuilder()
                            .setAction(eventDesc)
                            .setActionTarget(
                                    event.getSource().extend(WellKnownPathTypes.Event, event.getId().toString())
                                            .toString())
                            .setActionValue(success ? "SUCCESS" : "FAILURE")
                            .setActor(notificationContext.getUser().getPath().toString())
                            .setActorHost(profileName)
                            .setOriginatingContext(ApplicationScope.GATEWAY)
                            .setOriginatingSystem("Alarming")
                            .setStatusCode(success ? DataQuality.GOOD_DATA.getIntValue() : 0)
                            .setTimestamp(new Date())
                            .build();
                    p.audit(r);
                }
            } catch (Exception e) {
                logger.error("Error auditing event.", e);
            }
        }
    }

    private String evaluateStringExpression(NotificationContext notificationContext, BasicNotificationProfileProperty property) {
        Parser parser = new StringParser();

        FallbackPropertyResolver resolver =
                new FallbackPropertyResolver(context.getAlarmManager().getPropertyResolver());

        FormattedExpressionParseContext parseContext =
                new FormattedExpressionParseContext(
                        new AlarmEventCollectionExpressionParseContext(resolver, notificationContext.getAlarmEvents()));

        String expressionString = null;

        if (property.equals(GotifyProperties.MESSAGE)) {
            String customMessage = notificationContext.getAlarmEvents().get(0).get(GotifyProperties.CUSTOM_MESSAGE);
            boolean isThrottled = notificationContext.getAlarmEvents().size() > 1;

            if (isThrottled || StringUtils.isBlank(customMessage)) {
                expressionString = isThrottled ?
                        notificationContext.getOrDefault(GotifyProperties.THROTTLED_MESSAGE) :
                        notificationContext.getOrDefault(GotifyProperties.MESSAGE);
            } else {
                expressionString = customMessage;
            }
        } else if (property.equals(GotifyProperties.TITLE)) {
            String customTitle = notificationContext.getAlarmEvents().get(0).get(GotifyProperties.CUSTOM_TITLE);
            if (StringUtils.isBlank(customTitle)) {
                expressionString = notificationContext.getOrDefault(GotifyProperties.TITLE);
            } else {
                expressionString = customTitle;
            }
        } else {
            expressionString = (String) notificationContext.getOrDefault(property);
        }

        if (expressionString == null) {
            return null;
        }

        String evaluated = expressionString;
        try {
            QualifiedValue value = parser.parse(expressionString, parseContext).execute();
            if (value.getQuality().isGood()) {
                evaluated = TypeUtilities.toString(value.getValue());
            }
        } catch (Exception e) {
            logger.error(String.format("Error parsing expression '%s'.", expressionString, e));
        }

        logger.trace(String.format("%s evaluated to '%s'.", property.toString(), evaluated));

        return evaluated;
    }

    private Integer evaluateIntegerExpression(NotificationContext notificationContext, BasicNotificationProfileProperty property) {
        Parser parser = new StringParser();

        FallbackPropertyResolver resolver =
                new FallbackPropertyResolver(context.getAlarmManager().getPropertyResolver());

        FormattedExpressionParseContext parseContext =
                new FormattedExpressionParseContext(
                        new AlarmEventCollectionExpressionParseContext(resolver, notificationContext.getAlarmEvents()));

        if (property.equals(GotifyProperties.PRIORITY)) {
            String customPriority = notificationContext.getAlarmEvents().get(0).get(GotifyProperties.CUSTOM_PRIORITY);

            if (StringUtils.isBlank(customPriority)) {
                logger.info(String.format("Block Priority - %s evaluated to '%s'.", property, TypeUtilities.toString(notificationContext.getOrDefault(GotifyProperties.PRIORITY))));
                return notificationContext.getOrDefault(GotifyProperties.PRIORITY);
            } else {
                try {
                    QualifiedValue value = parser.parse(customPriority, parseContext).execute();
                    if (value.getQuality().isGood()) {
                        Integer evaluated = TypeUtilities.toInteger(value.getValue());
                        logger.info(String.format("Custom Priority - %s evaluated to '%s'.", property, TypeUtilities.toString(evaluated)));
                        return TypeUtilities.toInteger(value.getValue());
                    }
                } catch (Exception e) {
                    logger.error(String.format("Error parsing expression '%s'.", customPriority, e));
                }
            }
        }
        logger.info(String.format("Default Priority - %s evaluated to '%s'.", property, TypeUtilities.toString(notificationContext.getOrDefault(property))));
        return (Integer) notificationContext.getOrDefault(property);
    }
    /**
     * A {@link Predicate} that returns true if a {@link ContactInfo}'s {@link ContactType} is Console.
     */
    private static class IsGotifyContactInfo implements Predicate<ContactInfo> {
        @Override
        public boolean apply(ContactInfo contactInfo) {
            return GotifyNotificationProfileType.GOTIFY.getContactType().equals(contactInfo.getContactType());
        }
    }

    private static class Message{
        private String message;
        private String title;
        private int priority;
        private Map extras;

        public Message(String title, String message, int priority, Map extras) {
            this.message = message;
            this.priority = priority;
            this.title = title;
            this.extras = extras;

        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Map getExtras() { return this.extras; }
        public void setExtras(Map extras) { this.extras = extras;}

    }

    private Map buildExtras(final NotificationContext notificationContext) {
        HashMap map = new HashMap();
        String contentType = evaluateStringExpression(notificationContext, GotifyProperties.CONTENT_TYPE);
        if (StringUtils.isNotEmpty(contentType)) {
            map.put("client::display",new HashMap(){{ put("contentType",contentType);}});
        }

        String clickUrl = evaluateStringExpression(notificationContext, GotifyProperties.CLICK_URL);
        if (StringUtils.isNotEmpty(clickUrl)) {
            map.put("client::notification",new HashMap(){{ put("click",new HashMap(){{ put("url",clickUrl);}});}});
        }

        String intentUrl = evaluateStringExpression(notificationContext, GotifyProperties.INTENT_URL);
        if (StringUtils.isNotEmpty(intentUrl)) {
            map.put("android::action",new HashMap(){{ put("onReceive",new HashMap(){{ put("intentUrl",intentUrl);}});}});
        }
        return map;
    }

}
