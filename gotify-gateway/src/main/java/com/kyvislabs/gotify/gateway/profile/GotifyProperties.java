package com.kyvislabs.gotify.gateway.profile;

import com.inductiveautomation.ignition.alarming.common.notification.BasicNotificationProfileProperty;
import com.inductiveautomation.ignition.common.alarming.config.AlarmProperty;
import com.inductiveautomation.ignition.common.alarming.config.BasicAlarmProperty;
import com.inductiveautomation.ignition.common.config.ConfigurationProperty;
import com.inductiveautomation.ignition.common.i18n.LocalizedString;

import java.util.ArrayList;
import java.util.List;

import static com.inductiveautomation.ignition.common.BundleUtil.i18n;

public class GotifyProperties {

    public static final BasicNotificationProfileProperty<String> MESSAGE = new BasicNotificationProfileProperty<>(
            "message",
            "GotifyNotification." + "Properties.Message.DisplayName",
            null,
            String.class
    );

    public static final BasicNotificationProfileProperty<String> THROTTLED_MESSAGE =
            new BasicNotificationProfileProperty<>(
                    "throttledMessage",
                    "GotifyNotification." + "Properties.ThrottledMessage.DisplayName",
                    null,
                    String.class
            );

    public static final BasicNotificationProfileProperty<Long> TIME_BETWEEN_NOTIFICATIONS =
            new BasicNotificationProfileProperty<>(
                    "delayBetweenContact",
                    "GotifyNotification." + "Properties.TimeBetweenNotifications.DisplayName",
                    null,
                    Long.class
            );

    public static final BasicNotificationProfileProperty<String> TITLE = new BasicNotificationProfileProperty<>(
            "title",
            "GotifyNotification." + "Properties.Title.DisplayName",
            null,
            String.class
    );

    public static final BasicNotificationProfileProperty<String> CONTENT_TYPE = new BasicNotificationProfileProperty<>(
            "content_type",
            "GotifyNotification.Properties.ContentType.DisplayName",
            null,
            String.class
    );

    public static final BasicNotificationProfileProperty<String> CLICK_URL = new BasicNotificationProfileProperty<>(
            "click_url",
            "GotifyNotification.Properties.ClickUrl.DisplayName",
            null,
            String.class
    );

    public static final BasicNotificationProfileProperty<String> INTENT_URL = new BasicNotificationProfileProperty<>(
            "intent_url",
            "GotifyNotification.Properties.IntentUrl.DisplayName",
            null,
            String.class
    );

    public static final BasicNotificationProfileProperty<Integer> PRIORITY = new BasicNotificationProfileProperty<>(
            "priority",
            "GotifyNotification." + "Properties.Priority.DisplayName",
            null,
            Integer.class
    );

    public static final BasicNotificationProfileProperty<Boolean> TEST_MODE = new BasicNotificationProfileProperty<>(
            "testMode",
            "GotifyNotification." + "Properties.TestMode.DisplayName",
            null,
            Boolean.class
    );

    /**
     * EXTENDED CONFIG - These are different than the properties above, they are registered for each alarm through the
     * extended config system
     **/

    public static AlarmProperty<String> CUSTOM_TITLE = new BasicAlarmProperty<>("CustomGotifyTitle",
            String.class, "",
            "GotifyNotification.Properties.ExtendedConfig.CustomTitle",
            "GotifyNotification.Properties.ExtendedConfig.Category",
            "GotifyNotification.Properties.ExtendedConfig.CustomTitle.Desc", true, false);

    public static AlarmProperty<String> CUSTOM_MESSAGE = new BasicAlarmProperty<>("CustomGotifyMessage",
            String.class, "",
            "GotifyNotification.Properties.ExtendedConfig.CustomMessage",
            "GotifyNotification.Properties.ExtendedConfig.Category",
            "GotifyNotification.Properties.ExtendedConfig.CustomMessage.Desc", true, false);

    public static AlarmProperty<String> CUSTOM_PRIORITY = new BasicAlarmProperty<>("CustomGotifyPriority",
            String.class, "",
            "GotifyNotification.Properties.ExtendedConfig.CustomPriority",
            "GotifyNotification.Properties.ExtendedConfig.Category",
            "GotifyNotification.Properties.ExtendedConfig.CustomPriority.Desc", true, false);

    static {
        MESSAGE.setExpressionSource(true);
        MESSAGE.setDefaultValue(i18n("GotifyNotification." + "Properties.Message.DefaultValue"));

        THROTTLED_MESSAGE.setExpressionSource(true);
        THROTTLED_MESSAGE.setDefaultValue(i18n("GotifyNotification." + "Properties.ThrottledMessage.DefaultValue"));

        TIME_BETWEEN_NOTIFICATIONS.setExpressionSource(true);
        TIME_BETWEEN_NOTIFICATIONS.setDefaultValue(i18n("GotifyNotification."
                + "Properties.TimeBetweenNotifications.DefaultValue"));

        TITLE.setExpressionSource(true);

        PRIORITY.setDefaultValue(5);
        List<ConfigurationProperty.Option<String>> contentTypes = new ArrayList<>();
        contentTypes.add(new ConfigurationProperty.Option<>("text/plain", new LocalizedString("GotifyNotification."
                + "Properties.ContentType.Plain")));
        contentTypes.add(new ConfigurationProperty.Option<>("text/markdown", new LocalizedString("GotifyNotification."
               + "Properties.ContentType.Markdown")));

        CONTENT_TYPE.setDefaultValue("text/plain");
        CONTENT_TYPE.setOptions(contentTypes);

        TEST_MODE.setDefaultValue(false);
        List<ConfigurationProperty.Option<Boolean>> options = new ArrayList<>();
        options.add(new ConfigurationProperty.Option<>(true, new LocalizedString("words.yes")));
        options.add(new ConfigurationProperty.Option<>(false, new LocalizedString("words.no")));
        TEST_MODE.setOptions(options);
    }

}
