# Gotify Notification Module

An Ignition module that adds support for sending alarm notifications through Gotify. Gotify is a self hosted server for sending and receiving push messages.

See [Gotify](https://gotify.net/) for more details on the service.

Open Source
---------------

The Gotify module is an open source project distributed under the Apache 2.0 license. Please feel free to download the source code and contribute. 

Getting Started
---------------

1. Download the latest version module (.modl) from [releases](https://github.com/Kyvis-Labs/ignition-gotify-module/releases)
2. Install the module into Ignition 8.1+
3. Add a new alarm notification profile in the Gateway configuration section (under Alarming > Notification)
4. Select Gotify Notification
5. Enter your Gotify base URL (see below for more details)
6. Add your Gotify api token to the user's contact info
7. Ensure your user is in the on-call roster
8. Use the Gotify profile in your alarm notification pipeline
9. Enjoy!

Obtaining API Token and User Key
---------------

To get started pushing notifications from Ignition via Gotify, you'll first need to set up a Gotify server.  Please check for instructions at [Gotify](https://gotify.net/).

User Contact Info
---------------

The module adds a new contact info type, called `Gotify API Token`. This token is unique to each user, and is configured in the Gotify server.

Example User Device Name:  ```EE3TdhUCJtdmCCp```

Make sure to set the contact info for each user you want to notify.

Notification Block Properties
---------------

The profile has 5 properties you can set in the notification block in the alarm notification pipeline:

| Property            | Description                                                                                |
| :-------------------| :------------------------------------------------------------------------------------------|
| `Message`           | The message to send, if no custom alarm message is defined.                                |
| `Throttled Message` | The message to send if throttling is turned.                                               |
| `Title`             | The title of the message in the Gotify app.                                              |
| `Priority`          | The priority of the message, affects how the message is presented to the user.             |                                        |
| `Test Mode`         | Test mode. When true the message is not sent to Gotify but logged in the console.        |

### `Message`
The `Message` property defines the message to send. The message is dynamic using Ignition's Expression language. Defaults to:

```At {eventTime|hh:mm:ss}, alarm "{name}" at "{displayPath}" transitioned to {eventState}.```

### `Throttled Message`
The `Throttled Message` property defines the throttled message to send when consolidation is turned on. The message is dynamic using Ignition's Expression language. Defaults to:

```{alarmEvents.Count} alarm events have occurred.```

### `Title`
The `Title` property defines the title of the message in the Gotify app. The title is optional. If empty, the app's name in Gotify is used.

### `Priority`
The `Priority` property defines the priority of the message. Priority mapping is client dependent, but for the Android App, here is how priorities are mapped

| Notification                                  | Gotify Priority  |
| :---------------------------------------------| :----------------|
| `No Notification`                             | 0                |
| `Icon in notification bar`                    | 1-3              |
| `Icon in notification bar + sound`            | 4-7              |
| `Icon in notification bar + sound + vibration` | 8+              |

### `Content Type`
The `Content Type` property tells clients how to render the message.  Text will be displayed as is, and Markdown will be parsed and rendered by the client.  Markdown support varies by client.

### `Click URL`
The `Click URL` property sets a property in the message that the client applications can use to open an url when the notification is clicked.

### `Intent URL`
The `Click URL` property sets a property in the message that the client applications can use to open an intent when the notification was delivered.

### `Test Mode`
The `Test Mode` property defines the whether or not to run in test mode. If false, the message is sent normally. If true, the message will only be logged through the Ignition console.

Tag Alarm Properties
---------------

The module provides 3 additional alarm properties on each alarm.  They allow per alarm customization of the message.  If a property is set, it overrides the notification block setting.  The property value is dynamic using Ignition's Expression language.

| Property           | Description                                                                                                                        |
| :------------------| :----------------------------------------------------------------------------------------------------------------------------------|
| Custom Title       | If specified, will be used for the Gotify message title. If blank, the title defined in the notification block will be used.       |
| Custom Message     | If specified, will be used for the Gotify message body. If blank, the message defined in the notification block will be used.      |
| Custom Priority    | If specified, will be used for the Gotify message priority. If blank, the priority defined in the notification block will be used. |