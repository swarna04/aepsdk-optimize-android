/*
 Copyright 2021 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile;

import androidx.annotation.NonNull;

import com.adobe.marketing.mobile.optimize.ADBCountDownLatch;
import com.adobe.marketing.mobile.optimize.OptimizeTestConstants;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A third party extension class aiding for assertion against dispatched events, shared state
 * and XDM shared state.
 */

public class MonitorExtension extends Extension {

    private static final String SELF_TAG = "MonitorExtension";

    private static final Map<EventSpec, List<Event>> receivedEvents = new HashMap<>();
    private static final Map<EventSpec, ADBCountDownLatch> expectedEvents = new HashMap<>();
    private static ConfigurationMonitor configurationMonitor = null;

    protected MonitorExtension(final ExtensionApi extensionApi) {
        super(extensionApi);
    }

    @Override
    protected String getName() {
        return "MonitorExtension";
    }

    public static void registerExtension() {
        MobileCore.registerExtension(MonitorExtension.class, extensionError -> {
            if (extensionError == null) {
                return;
            }
            Log.error(OptimizeTestConstants.LOG_TAG, SELF_TAG,
                    "An error occurred while registering the Optimize extension: %s ", extensionError.getErrorName());
        });
    }

    @Override
    protected void onRegistered() {
        getApi().registerEventListener(EventType.WILDCARD, EventSource.WILDCARD, this::wildcardProcessor);
    }

    /**
     * Add an event to the list of expected events.
     * @param type the type of the event.
     * @param source the source of the event.
     * @param count the number of events expected to be received.
     */
    public static void setExpectedEvent(final String type, final String source, final int count) {
        EventSpec eventSpec = new EventSpec(source, type);
        expectedEvents.put(eventSpec, new ADBCountDownLatch(count));
    }

    public static Map<EventSpec, ADBCountDownLatch> getExpectedEvents() {
        return expectedEvents;
    }

    public static Map<EventSpec, List<Event>> getReceivedEvents() {
        return receivedEvents;
    }

    /**
     * Resets the map of received and expected events.
     */
    public static void reset() {
        Log.trace(OptimizeTestConstants.LOG_TAG, SELF_TAG, "Reset expected and received events.");
        receivedEvents.clear();
        expectedEvents.clear();
    }

    /**
     * Processor for all heard events.
     * If the event type is of this Monitor Extension, then
     * the action is performed per the event source.
     * All other events are added to the map of received events. If the event is in the map
     * of expected events, its latch is counted down.
     *
     * @param event incoming {@link Event} object to be processed.
     */
    public void wildcardProcessor(final Event event) {
        if (OptimizeTestConstants.EventType.MONITOR.equalsIgnoreCase(event.getType())) {
            if (OptimizeTestConstants.EventSource.SHARED_STATE_REQUEST.equalsIgnoreCase(event.getSource())) {
                processSharedStateRequest(event);
            } else if (OptimizeTestConstants.EventSource.XDM_SHARED_STATE_REQUEST.equalsIgnoreCase(event.getSource())) {
                processXDMSharedStateRequest(event);
            } else if (OptimizeTestConstants.EventSource.UNREGISTER.equalsIgnoreCase(event.getSource())) {
                processUnregisterRequest(event);
            }

            return;
        }

        EventSpec eventSpec = new EventSpec(event.getSource(), event.getType());

        Log.debug(OptimizeTestConstants.LOG_TAG, SELF_TAG, "Received and processing event " + eventSpec);

        if (!receivedEvents.containsKey(eventSpec)) {
            receivedEvents.put(eventSpec, new ArrayList<>());
        }

        receivedEvents.get(eventSpec).add(event);


        if (expectedEvents.containsKey(eventSpec)) {
            expectedEvents.get(eventSpec).countDown();
        }

        SharedStateResult sharedStateResult = getApi().getSharedState(
                "com.adobe.module.configuration",
                event,
                false,
                SharedStateResolution.LAST_SET
        );
        if (configurationMonitor != null) {
            configurationMonitor.call(sharedStateResult.getValue());
        }
    }

    /**
     * Processor which unregisters this extension.
     * @param event event incoming {@link Event} object to be processed.
     */
    private void processUnregisterRequest(final Event event) {
        Log.debug(OptimizeTestConstants.LOG_TAG, SELF_TAG, "Unregistering the Monitor Extension.");
        getApi().unregisterExtension();
    }

    /**
     * Processor which retrieves and dispatches the XDM shared state for the state owner specified
     * in the request.
     * @param event event incoming {@link Event} object to be processed.
     */
    private void processXDMSharedStateRequest(final Event event) {
        Map<String, Object> eventData = event.getEventData();

        if (eventData == null) {
            return;
        }

        String stateOwner = DataReader.optString(eventData, OptimizeTestConstants.EventDataKeys.STATE_OWNER, null);

        if (stateOwner == null) {
            return;
        }

        SharedStateResult sharedState = getApi().getXDMSharedState(stateOwner, event, false, SharedStateResolution.LAST_SET);

        Event responseEvent = new Event.Builder("Get Shared State Response", OptimizeTestConstants.EventType.MONITOR,
                OptimizeTestConstants.EventSource.SHARED_STATE_RESPONSE)
                .setEventData(sharedState == null ? null : sharedState.getValue())
                .inResponseToEvent(event)
                .build();

        MobileCore.dispatchEvent(responseEvent);
    }

    /**
     * Processor which retrieves and dispatches the shared state for the state owner specified
     * in the request.
     * @param event event incoming {@link Event} object to be processed.
     */
    private void processSharedStateRequest(final Event event) {
        Map<String, Object> eventData = event.getEventData();

        if (eventData == null) {
            return;
        }

        String stateOwner = DataReader.optString(eventData, OptimizeTestConstants.EventDataKeys.STATE_OWNER, null);

        if (stateOwner == null) {
            return;
        }

        SharedStateResult sharedState = getApi().getSharedState(stateOwner, event, false, SharedStateResolution.LAST_SET);

        Event responseEvent = new Event.Builder("Get Shared State Response", OptimizeTestConstants.EventType.MONITOR,
                OptimizeTestConstants.EventSource.SHARED_STATE_RESPONSE)
                .setEventData(sharedState == null ? null : sharedState.getValue())
                .inResponseToEvent(event)
                .build();

        MobileCore.dispatchEvent(responseEvent);
    }

    public static void configurationAwareness(ConfigurationMonitor callback) {
        configurationMonitor = callback;
    }

    public interface ConfigurationMonitor {
        void call(Map<String, Object> configurationState);
    }

    /**
     * Class defining {@link Event} specifications, contains Event's source and type.
     */
    public static class EventSpec {
        final String source;
        final String type;

        public EventSpec(final String source, final String type) {
            if (source == null || source.isEmpty()) {
                throw new IllegalArgumentException("Event Source cannot be null or empty.");
            }

            if (type == null || type.isEmpty()) {
                throw new IllegalArgumentException("Event Type cannot be null or empty.");
            }

            // Normalize strings
            this.source = source.toLowerCase();
            this.type = type.toLowerCase();
        }

        @NonNull
        @Override
        public String toString() {
            return "type '" + type + "' and source '" + source + "'";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            EventSpec eventSpec = (EventSpec) o;
            return Objects.equals(source, eventSpec.source) &&
                    Objects.equals(type, eventSpec.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, type);
        }
    }
}
