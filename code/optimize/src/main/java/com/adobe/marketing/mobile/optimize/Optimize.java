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

package com.adobe.marketing.mobile.optimize;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Public class containing APIs for the Optimize extension.
 */
public class Optimize {
    public final static Class<? extends Extension> EXTENSION = OptimizeExtension.class;
    private static final String SELF_TAG = "Optimize";
    private static boolean isPropositionsResponseListenerRegistered = false;

    private Optimize() {}

    /**
     * Returns the version of the {@code Optimize} extension.
     *
     * @return {@link String} containing the current installed version of this extension.
     */
    @NonNull
    public static String extensionVersion() {
        return OptimizeConstants.EXTENSION_VERSION;
    }

    /**
     * Registers the extension with the Mobile Core.
     * <p>
     * Note: This method should be called only once in your application class.
     */
    @Deprecated
    public static void registerExtension() {
        MobileCore.registerExtension(OptimizeExtension.class, extensionError -> {
            if (extensionError == null) {
                return;
            }
            Log.error(OptimizeConstants.LOG_TAG, SELF_TAG,
                    "An error occurred while registering the Optimize extension: %s ", extensionError.getErrorName());
        });
    }

    /**
     * This API dispatches an Event for the Edge network extension to fetch decision propositions, for the provided decision scopes list, from the decisioning services enabled in the Experience Edge network.
     * <p>
     * The returned decision propositions are cached in-memory in the Optimize SDK extension and can be retrieved using {@link #getPropositions(List, AdobeCallback)} API.
     *
     * @param decisionScopes {@code List<DecisionScope>} containing scopes for which offers need to be updated.
     * @param xdm {@code Map<String, Object>} containing additional XDM-formatted data to be sent in the personalization query request.
     * @param data {@code Map<String, Object>} containing additional free-form data to be sent in the personalization query request.
     */
    public static void updatePropositions(@NonNull final List<DecisionScope> decisionScopes,
                                          @Nullable final Map<String, Object> xdm,
                                          @Nullable final Map<String, Object> data) {
        if (OptimizeUtils.isNullOrEmpty(decisionScopes)) {
            Log.warning(OptimizeConstants.LOG_TAG, SELF_TAG, "Cannot update propositions, provided list of decision scopes is null or empty.");
            return;
        }

        final List<DecisionScope> validScopes = new ArrayList<>();
        for (final DecisionScope scope: decisionScopes) {
            if (!scope.isValid()) {
                continue;
            }
            validScopes.add(scope);
        }

        if (validScopes.size() == 0) {
            Log.warning(OptimizeConstants.LOG_TAG, SELF_TAG, "Cannot update propositions, provided list of decision scopes has no valid scope.");
            return;
        }

        final List<Map<String, Object>> flattenedDecisionScopes = new ArrayList<>();
        for (final DecisionScope scope: validScopes) {
            flattenedDecisionScopes.add(scope.toEventData());
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(OptimizeConstants.EventDataKeys.REQUEST_TYPE, OptimizeConstants.EventDataValues.REQUEST_TYPE_UPDATE);
        eventData.put(OptimizeConstants.EventDataKeys.DECISION_SCOPES, flattenedDecisionScopes);

        if (!OptimizeUtils.isNullOrEmpty(xdm)) {
            eventData.put(OptimizeConstants.EventDataKeys.XDM, xdm);
        }

        if (!OptimizeUtils.isNullOrEmpty(data)) {
            eventData.put(OptimizeConstants.EventDataKeys.DATA, data);
        }

        final Event event = new Event.Builder(OptimizeConstants.EventNames.UPDATE_PROPOSITIONS_REQUEST,
                OptimizeConstants.EventType.OPTIMIZE,
                OptimizeConstants.EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(event);
    }

    /**
     * This API retrieves the previously fetched propositions, for the provided decision scopes, from the in-memory extension propositions cache.
     *
     * @param decisionScopes {@code List<DecisionScope>} containing scopes for which offers need to be requested.
     * @param callback {@code AdobeCallbackWithError<Map<DecisionScope, Proposition>>} which will be invoked when decision propositions are retrieved from the local cache.
     */
    public static void getPropositions(@NonNull final List<DecisionScope> decisionScopes,
                                       @NonNull final AdobeCallback<Map<DecisionScope, Proposition>> callback) {
        if (OptimizeUtils.isNullOrEmpty(decisionScopes)) {
            Log.warning(OptimizeConstants.LOG_TAG, SELF_TAG, "Cannot get propositions, provided list of decision scopes is null or empty.");
            failWithError(callback, AdobeError.UNEXPECTED_ERROR);
            return;
        }

        final List<DecisionScope> validScopes = new ArrayList<>();
        for (final DecisionScope scope: decisionScopes) {
            if (!scope.isValid()) {
                continue;
            }
            validScopes.add(scope);
        }

        if (validScopes.size() == 0) {
            Log.warning(OptimizeConstants.LOG_TAG, SELF_TAG, "Cannot update propositions, provided list of decision scopes has no valid scope.");
            failWithError(callback, AdobeError.UNEXPECTED_ERROR);
            return;
        }

        final List<Map<String, Object>> flattenedDecisionScopes = new ArrayList<>();
        for (final DecisionScope scope : validScopes) {
            flattenedDecisionScopes.add(scope.toEventData());
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(OptimizeConstants.EventDataKeys.REQUEST_TYPE, OptimizeConstants.EventDataValues.REQUEST_TYPE_GET);
        eventData.put(OptimizeConstants.EventDataKeys.DECISION_SCOPES, flattenedDecisionScopes);

        final Event event = new Event.Builder(OptimizeConstants.EventNames.GET_PROPOSITIONS_REQUEST,
                OptimizeConstants.EventType.OPTIMIZE,
                OptimizeConstants.EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEventWithResponseCallback(event, OptimizeConstants.DEFAULT_RESPONSE_CALLBACK_TIMEOUT,  new AdobeCallbackWithError<Event>() {
            @Override
            public void fail(final AdobeError adobeError) {
                failWithError(callback, adobeError);
            }

            @Override
            public void call(final Event event) {
                try {
                    final Map<String, Object> eventData = event.getEventData();
                    if (OptimizeUtils.isNullOrEmpty(eventData)) {
                        failWithError(callback, AdobeError.UNEXPECTED_ERROR);
                        return;
                    }

                    if (eventData.containsKey(OptimizeConstants.EventDataKeys.RESPONSE_ERROR)) {
                        final int errorCode = DataReader.getInt(eventData, OptimizeConstants.EventDataKeys.RESPONSE_ERROR);
                        failWithError(callback, OptimizeUtils.convertToAdobeError(errorCode));
                        return;
                    }

                    final List<Map<String, Object>> propositionsList;
                    propositionsList = DataReader.getTypedListOfMap(Object.class, eventData, OptimizeConstants.EventDataKeys.PROPOSITIONS);
                    final Map<DecisionScope, Proposition> propositionsMap = new HashMap<>();
                    if (propositionsList != null) {
                        for (final Map<String, Object> propositionData : propositionsList) {
                            final Proposition proposition = Proposition.fromEventData(propositionData);
                            if (proposition != null && !OptimizeUtils.isNullOrEmpty(proposition.getScope())) {
                                final DecisionScope scope = new DecisionScope(proposition.getScope());
                                propositionsMap.put(scope, proposition);
                            }
                        }
                    }
                    callback.call(propositionsMap);
                } catch (DataReaderException e) {
                    failWithError(callback, AdobeError.UNEXPECTED_ERROR);
                }
            }
        });
    }

    /**
     * This API registers a permanent callback which is invoked whenever the Edge extension dispatches a response Event received from the Experience Edge Network upon a personalization query.
     * <p>
     * The personalization query requests can be triggered by the {@link Optimize#updatePropositions(List, Map, Map)} API, Edge extension {@code sendEvent(ExperienceEvent, EdgeCallback)} API or launch consequence rules.
     *
     * @param callback {@code AdobeCallbackWithError<Map<DecisionScope, Proposition>>} which will be invoked when decision propositions are received from the Edge network.
     */
    public static void onPropositionsUpdate(@NonNull final AdobeCallback<Map<DecisionScope, Proposition>> callback) {
        // Only register the listener once
        if (!isPropositionsResponseListenerRegistered) {
            MobileCore.registerEventListener(OptimizeConstants.EventType.OPTIMIZE, OptimizeConstants.EventSource.NOTIFICATION, new AdobeCallbackWithError<Event>() {
                @Override
                public void fail(final AdobeError error) {
                }

                @Override
                public void call(final Event event) {
                    final Map<String, Object> eventData = event.getEventData();
                    if (OptimizeUtils.isNullOrEmpty(eventData)) {
                        return;
                    }

                    final List<Map<String, Object>> propositionsList;
                    try {
                        propositionsList = DataReader.getTypedListOfMap(Object.class, eventData, OptimizeConstants.EventDataKeys.PROPOSITIONS);

                        final Map<DecisionScope, Proposition> propositionsMap = new HashMap<>();
                        if (propositionsList != null) {
                            for (final Map<String, Object> propositionData : propositionsList) {
                                final Proposition proposition = Proposition.fromEventData(propositionData);
                                if (proposition != null && !OptimizeUtils.isNullOrEmpty(proposition.getScope())) {
                                    final DecisionScope scope = new DecisionScope(proposition.getScope());
                                    propositionsMap.put(scope, proposition);
                                }
                            }
                        }

                        if (!propositionsMap.isEmpty()) {
                            callback.call(propositionsMap);
                        }
                    } catch (DataReaderException ignored) {
                    }
                }
            });
            isPropositionsResponseListenerRegistered = true;
        }
    }

    /**
     * Clears the client-side in-memory propositions cache.
     */
    public static void clearCachedPropositions() {
        final Event event = new Event.Builder(OptimizeConstants.EventNames.CLEAR_PROPOSITIONS_REQUEST,
                                            OptimizeConstants.EventType.OPTIMIZE,
                                            OptimizeConstants.EventSource.REQUEST_RESET).build();
        MobileCore.dispatchEvent(event);
    }

    /**
     * Invokes fail method with the provided {@code error}, if the callback is an instance of {@code AdobeCallbackWithError}.
     *
     * @param callback can be an instance of {@link AdobeCallback} or {@link AdobeCallbackWithError}.
     * @param error {@link AdobeError} indicating the error name and code.
     */
    private static void failWithError(final AdobeCallback<?> callback, final AdobeError error) {

        final AdobeCallbackWithError<?> callbackWithError = callback instanceof AdobeCallbackWithError ?
                (AdobeCallbackWithError<?>) callback : null;

        if (callbackWithError != null) {
            callbackWithError.fail(error);
        }
    }

    @VisibleForTesting
    static void setIsPropositionsResponseListenerRegistered(final boolean status) {
        isPropositionsResponseListenerRegistered = status;
    }
}