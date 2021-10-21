package com.kyvislabs.gotify.gateway.profile;

import com.inductiveautomation.ignition.alarming.notification.AlarmNotificationProfileRecord;
import com.inductiveautomation.ignition.gateway.audit.AuditProfileRecord;
import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import simpleorm.dataset.SFieldFlags;

public class GotifyNotificationProfileSettings extends PersistentRecord {

    public static final RecordMeta<GotifyNotificationProfileSettings> META =
            new RecordMeta<>(
                    GotifyNotificationProfileSettings.class,
                    "GotifyNotificationProfileSettings"
            );
    public static final IdentityField Id = new IdentityField(META);
    public static final LongField ProfileId = new LongField(META, "ProfileId");
    public static final ReferenceField<AlarmNotificationProfileRecord> Profile = new ReferenceField<>(
            META,
            AlarmNotificationProfileRecord.META,
            "Profile",
            ProfileId);

    public static final StringField ServerUrl = new StringField(META, "ServerUrl", SFieldFlags.SMANDATORY);

    static final Category API = new Category("GotifyNotificationProfileSettings.Category.API", 1)
            .include(ServerUrl);

    public static final LongField AuditProfileId = new LongField(META, "AuditProfileId");
    public static final ReferenceField<AuditProfileRecord> AuditProfile = new ReferenceField<>(
            META, AuditProfileRecord.META, "AuditProfile", AuditProfileId);

    static final Category Auditing = new Category("GotifyNotificationProfileSettings.Category.Auditing", 2)
            .include(AuditProfile);

    static {
        Profile.getFormMeta().setVisible(false);
    }

    @Override
    public RecordMeta<?> getMeta() {
        return META;
    }

    public String getAuditProfileName() {
        AuditProfileRecord rec = findReference(AuditProfile);
        return rec == null ? null : rec.getName();
    }

    public String getServerUrl() {
        return getString(ServerUrl);
    }
}

