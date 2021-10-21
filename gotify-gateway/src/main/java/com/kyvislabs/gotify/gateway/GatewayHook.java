package com.kyvislabs.gotify.gateway;

import com.inductiveautomation.ignition.alarming.AlarmNotificationContext;
import com.inductiveautomation.ignition.alarming.common.ModuleMeta;
import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.services.ModuleServiceConsumer;
import com.kyvislabs.gotify.gateway.profile.GotifyNotificationProfileSettings;
import com.kyvislabs.gotify.gateway.profile.GotifyNotificationProfileType;
import com.kyvislabs.gotify.gateway.profile.GotifyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayHook extends AbstractGatewayModuleHook implements ModuleServiceConsumer {
    public static final String MODULE_ID = "com.kyvislabs.gotify";
    private final Logger logger = LoggerFactory.getLogger("Gotify.Gateway.Hook");

    private GatewayContext gatewayContext;
    private volatile AlarmNotificationContext notificationContext;

    @Override
    public void setup(GatewayContext gatewayContext) {
        this.gatewayContext = gatewayContext;
        BundleUtil.get().addBundle("GotifyNotification", getClass(), "GotifyNotification");

        gatewayContext.getModuleServicesManager().subscribe(AlarmNotificationContext.class, this);

        gatewayContext.getAlarmManager()
                .registerExtendedConfigProperties(ModuleMeta.MODULE_ID, GotifyProperties.CUSTOM_TITLE);

        gatewayContext.getAlarmManager()
                .registerExtendedConfigProperties(ModuleMeta.MODULE_ID, GotifyProperties.CUSTOM_MESSAGE);

        gatewayContext.getAlarmManager()
                .registerExtendedConfigProperties(ModuleMeta.MODULE_ID, GotifyProperties.CUSTOM_PRIORITY);

        gatewayContext.getUserSourceManager().registerContactType(GotifyNotificationProfileType.GOTIFY);

        try {
            gatewayContext.getSchemaUpdater().updatePersistentRecords(GotifyNotificationProfileSettings.META);
        } catch (Exception e) {
            logger.error("Error configuring internal database", e);
        }
    }

    @Override
    public void notifyLicenseStateChanged(LicenseState licenseState) {

    }

    @Override
    public void startup(LicenseState licenseState) {

    }

    @Override
    public void shutdown() {
        gatewayContext.getModuleServicesManager().unsubscribe(AlarmNotificationContext.class, this);

        if (notificationContext != null) {
            try {
                notificationContext.getAlarmNotificationManager().removeAlarmNotificationProfileType(
                        new GotifyNotificationProfileType());
            } catch (Exception e) {
                logger.error("Error removing notification profile.", e);
            }
        }

        BundleUtil.get().removeBundle("GotifyNotification");
        BundleUtil.get().removeBundle("GotifyNotificationProfileSettings");
    }

    @Override
    public void serviceReady(Class<?> serviceClass) {
        if (serviceClass == AlarmNotificationContext.class) {
            notificationContext = gatewayContext.getModuleServicesManager()
                    .getService(AlarmNotificationContext.class);

            try {
                notificationContext.getAlarmNotificationManager().addAlarmNotificationProfileType(
                        new GotifyNotificationProfileType());
            } catch (Exception e) {
                logger.error("Error adding notification profile.", e);
            }
        }
    }

    @Override
    public void serviceShutdown(Class<?> arg0) {
        notificationContext = null;
    }

    @Override
    public boolean isMakerEditionCompatible() {
        return true;
    }

    @Override
    public boolean isFreeModule() {
        return true;
    }
}
