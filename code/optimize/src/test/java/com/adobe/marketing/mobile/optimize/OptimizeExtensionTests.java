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

import android.util.Base64;

import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionEventListener;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.services.Log;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.runner.RunWith;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(MockitoJUnitRunner.Silent.class)
@SuppressWarnings("unchecked")
public class OptimizeExtensionTests {
    private OptimizeExtension extension;

    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;

    @Before
    public void setup() {
        extension = new OptimizeExtension(mockExtensionApi);
    }

    @Test
    public void test_getName() {
        // test
        final String extensionName = extension.getName();
        assertEquals("getName should return the correct extension name.", "com.adobe.optimize", extensionName);
    }

    @Test
    public void test_getVersion() {
        // test
        final String extensionVersion = extension.getVersion();
        assertEquals("getVersion should return the correct extension version.", "1.0.1", extensionVersion);
    }

    @Test
    public void test_registration() {
        // setup
        clearInvocations(mockExtensionApi);

        // test
        extension = new OptimizeExtension(mockExtensionApi);
        extension.onRegistered();

        // verify
        verify(mockExtensionApi, Mockito.times(1)).registerEventListener(
                eq("com.adobe.eventType.optimize"),
                eq("com.adobe.eventSource.requestContent"),
                any(ExtensionEventListener.class));
        verify(mockExtensionApi, Mockito.times(1)).registerEventListener(
                eq("com.adobe.eventType.edge"),
                eq("personalization:decisions"),
                any(ExtensionEventListener.class));
        verify(mockExtensionApi, Mockito.times(1)).registerEventListener(
                eq("com.adobe.eventType.edge"),
                eq("com.adobe.eventSource.errorResponseContent"),
                any(ExtensionEventListener.class));
        verify(mockExtensionApi, Mockito.times(1)).registerEventListener(
                eq("com.adobe.eventType.optimize"),
                eq("com.adobe.eventSource.requestReset"),
                any(ExtensionEventListener.class));
        verify(mockExtensionApi, Mockito.times(1)).registerEventListener(
                eq("com.adobe.eventType.generic.identity"),
                eq("com.adobe.eventSource.requestReset"),
                any(ExtensionEventListener.class));
    }

    @Test
    public void testReadyForEvent_configurationSet() {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .build();

        assertTrue(extension.readyForEvent(testEvent));
    }

    @Test
    public void testReadyForEvent_configurationNotSet() {
        // setup
        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .build();

        assertFalse(extension.readyForEvent(testEvent));
    }

    @Test
    public void testReadyForEvent_OptimizeResetContentEvent() {
        // setup
        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestReset")
                .build();

        assertTrue(extension.readyForEvent(testEvent));
    }

    @Test
    public void testReadyForEvent_EdgeEvent() {
        // setup
        final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                .build();

        assertTrue(extension.readyForEvent(testEvent));
    }

    @Test
    public void testHandleOptimizeRequestContent_nullEvent() {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        // test
        extension.handleOptimizeRequestContent(null);

        // verify
        verifyNoInteractions(mockExtensionApi);
    }

    @Test
    public void testHandleOptimizeRequestContent_nullEventData() {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(null)
                .build();


        // test
        extension.handleOptimizeRequestContent(testEvent);

        // verify
        // verify
        verifyNoInteractions(mockExtensionApi);
    }

    @Test
    public void testHandleOptimizeRequestContent_emptyEventData() {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(new HashMap<>())
                .build();


        // test
        extension.handleOptimizeRequestContent(testEvent);

        // verify
        verifyNoInteractions(mockExtensionApi);
    }

    @Test
    public void testHandleOptimizeRequestContent_invalidRequestType() {
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        Map<String, Object> eventData = new HashMap<String, Object>() {
            {
                put("requesttype", "unknown");
            }
        };
        Event testEvent = new Event.Builder("Optimize Get Propositions Request",
                "com.adobe.eventType.optimize",
                "com.adobe.eventSource.requestContent")
                .setEventData(eventData)
                .build();

        // test
        extension.handleOptimizeRequestContent(testEvent);

        // verify
        verifyNoInteractions(mockExtensionApi);
    }

    @Test
    public void testHandleOptimizeRequestContent_handleUpdatePropositions_validDecisionScope() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(anyString(), anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final DecisionScope testScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope.toEventData());
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();


            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
            assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());

            final Map<String, Object> query = (Map<String, Object>) dispatchedEvent.getEventData().get("query");
            assertNotNull(query);
            final Map<String, Object> queryPersonalization = (Map<String, Object>) query.get("personalization");
            assertNotNull(queryPersonalization);
            final List<String> schemas = (List<String>) queryPersonalization.get("schemas");
            assertNotNull(schemas);
            assertEquals(7, schemas.size());
            assertEquals(OptimizeExtension.supportedSchemas, schemas);
            final List<String> scopes = (List<String>) queryPersonalization.get("decisionScopes");
            assertNotNull(scopes);
            assertEquals(1, scopes.size());
            assertEquals(testScope.getName(), scopes.get(0));

            final Map<String, Object> xdm = (Map<String, Object>) dispatchedEvent.getEventData().get("xdm");
            assertNotNull(xdm);
            assertEquals(1, xdm.size());
            assertEquals("personalization.request", xdm.get("eventType"));

            final Map<String, Object> data = (Map<String, Object>) dispatchedEvent.getEventData().get("data");
            assertNull(data);

            final String datasetId = (String) dispatchedEvent.getEventData().get("datasetId");
            assertNull(datasetId);
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleUpdatePropositions_validDecisionScopeWithXdmAndDataAndDatasetId() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(anyString(), anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                    put("optimize.datasetId", "111111111111111111111111");
                }
            });

            final DecisionScope testScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope.toEventData());
                }
            });
            testEventData.put("xdm", new HashMap<String, Object>() {
                {
                    put("myXdmKey", "myXdmValue");
                }
            });
            testEventData.put("data", new HashMap<String, Object>() {
                {
                    put("myKey", "myValue");
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
            assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());

            final Map<String, Object> query = (Map<String, Object>) dispatchedEvent.getEventData().get("query");
            assertNotNull(query);
            final Map<String, Object> queryPersonalization = (Map<String, Object>) query.get("personalization");
            assertNotNull(queryPersonalization);
            final List<String> schemas = (List<String>) queryPersonalization.get("schemas");
            assertNotNull(schemas);
            assertEquals(7, schemas.size());
            assertEquals(OptimizeExtension.supportedSchemas, schemas);
            final List<String> scopes = (List<String>) queryPersonalization.get("decisionScopes");
            assertNotNull(scopes);
            assertEquals(1, scopes.size());
            assertEquals(testScope.getName(), scopes.get(0));

            final Map<String, Object> xdm = (Map<String, Object>) dispatchedEvent.getEventData().get("xdm");
            assertNotNull(xdm);
            assertEquals(2, xdm.size());
            assertEquals("personalization.request", xdm.get("eventType"));
            assertEquals("myXdmValue", xdm.get("myXdmKey"));

            final Map<String, Object> data = (Map<String, Object>) dispatchedEvent.getEventData().get("data");
            assertNotNull(data);
            assertEquals(1, data.size());
            assertEquals("myValue", data.get("myKey"));

            final String datasetId = (String) dispatchedEvent.getEventData().get("datasetId");
            assertEquals("111111111111111111111111", datasetId);
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleUpdatePropositions_validDecisionScopeWithXdmAndDataAndNoDatasetId() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(anyString(), anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final DecisionScope testScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope.toEventData());
                }
            });
            testEventData.put("xdm", new HashMap<String, Object>() {
                {
                    put("myXdmKey", "myXdmValue");
                }
            });
            testEventData.put("data", new HashMap<String, Object>() {
                {
                    put("myKey", "myValue");
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
            assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());

            final Map<String, Object> query = (Map<String, Object>) dispatchedEvent.getEventData().get("query");
            assertNotNull(query);
            final Map<String, Object> queryPersonalization = (Map<String, Object>) query.get("personalization");
            assertNotNull(queryPersonalization);
            final List<String> schemas = (List<String>) queryPersonalization.get("schemas");
            assertNotNull(schemas);
            assertEquals(7, schemas.size());
            assertEquals(OptimizeExtension.supportedSchemas, schemas);
            final List<String> scopes = (List<String>) queryPersonalization.get("decisionScopes");
            assertNotNull(scopes);
            assertEquals(1, scopes.size());
            assertEquals(testScope.getName(), scopes.get(0));

            final Map<String, Object> xdm = (Map<String, Object>) dispatchedEvent.getEventData().get("xdm");
            assertNotNull(xdm);
            assertEquals(2, xdm.size());
            assertEquals("personalization.request", xdm.get("eventType"));
            assertEquals("myXdmValue", xdm.get("myXdmKey"));

            final Map<String, Object> data = (Map<String, Object>) dispatchedEvent.getEventData().get("data");
            assertNotNull(data);
            assertEquals(1, data.size());
            assertEquals("myValue", data.get("myKey"));

            final String datasetId = (String) dispatchedEvent.getEventData().get("datasetId");
            assertNull(datasetId);
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleUpdatePropositions_multipleValidDecisionScopes() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(anyString(), anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final DecisionScope testScope1 = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final DecisionScope testScope2 = new DecisionScope("myMbox");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope1.toEventData());
                    add(testScope2.toEventData());
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();


            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
            assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());

            final Map<String, Object> query = (Map<String, Object>) dispatchedEvent.getEventData().get("query");
            assertNotNull(query);
            final Map<String, Object> queryPersonalization = (Map<String, Object>) query.get("personalization");
            assertNotNull(queryPersonalization);
            final List<String> schemas = (List<String>) queryPersonalization.get("schemas");
            assertNotNull(schemas);
            assertEquals(7, schemas.size());
            assertEquals(OptimizeExtension.supportedSchemas, schemas);
            final List<String> scopes = (List<String>) queryPersonalization.get("decisionScopes");
            assertNotNull(scopes);
            assertEquals(2, scopes.size());
            assertEquals(testScope1.getName(), scopes.get(0));
            assertEquals(testScope2.getName(), scopes.get(1));

            final Map<String, Object> xdm = (Map<String, Object>) dispatchedEvent.getEventData().get("xdm");
            assertNotNull(xdm);
            assertEquals(1, xdm.size());
            assertEquals("personalization.request", xdm.get("eventType"));

            final Map<String, Object> data = (Map<String, Object>) dispatchedEvent.getEventData().get("data");
            assertNull(data);

            final String datasetId = (String) dispatchedEvent.getEventData().get("datasetId");
            assertNull(datasetId);
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_UpdatePropositions_configurationNotAvailable() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {

            // setup
            final DecisionScope testScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope.toEventData());
                }
            });

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(0)).dispatch(any());
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString()), times(1));
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleUpdatePropositions_noDecisionScopes() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class);
             MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            base64MockedStatic.when(() -> Base64.decode(anyString(), anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>());

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(0)).dispatch(any());
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString()), times(2));
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleUpdatePropositions_invalidDecisionScope() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class);
             MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            base64MockedStatic.when(() -> Base64.decode(anyString(), anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final DecisionScope testScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoiIn0=");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope.toEventData());
                }
            });

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(0)).dispatch(any());
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString(), any()), times(2));
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString()), times(1));
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleUpdatePropositions_validAndInvalidDecisionScopes() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(anyString(), anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final DecisionScope testScope1 = new DecisionScope("eyJhY3Rpdml0eUlkIjoiIiwicGxhY2VtZW50SWQiOiJ4Y29yZTpvZmZlci1wbGFjZW1lbnQ6MTExMTExMTExMTExMTExMSJ9");
            final DecisionScope testScope2 = new DecisionScope("myMbox");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "updatepropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope1.toEventData());
                    add(testScope2.toEventData());
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Update Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();


            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
            assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());

            final Map<String, Object> query = (Map<String, Object>) dispatchedEvent.getEventData().get("query");
            assertNotNull(query);
            final Map<String, Object> queryPersonalization = (Map<String, Object>) query.get("personalization");
            assertNotNull(queryPersonalization);
            final List<String> schemas = (List<String>) queryPersonalization.get("schemas");
            assertNotNull(schemas);
            assertEquals(7, schemas.size());
            assertEquals(OptimizeExtension.supportedSchemas, schemas);
            final List<String> scopes = (List<String>) queryPersonalization.get("decisionScopes");
            assertNotNull(scopes);
            assertEquals(1, scopes.size());
            assertEquals(testScope2.getName(), scopes.get(0));

            final Map<String, Object> xdm = (Map<String, Object>) dispatchedEvent.getEventData().get("xdm");
            assertNotNull(xdm);
            assertEquals(1, xdm.size());
            assertEquals("personalization.request", xdm.get("eventType"));

            final Map<String, Object> data = (Map<String, Object>) dispatchedEvent.getEventData().get("data");
            assertNull(data);

            final String datasetId = (String) dispatchedEvent.getEventData().get("datasetId");
            assertNull(datasetId);
        }
    }

    @Test
    public void testHandleEdgeResponse_validProposition() throws Exception{
        // setup
        final Map<String, Object> edgeResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_RESPONSE_VALID.json"), HashMap.class);
        final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                .setEventData(edgeResponseData)
                .build();

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleEdgeResponse(testEvent);

        // verify
        verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.notification", dispatchedEvent.getSource());

        final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
        assertNotNull(propositionsList);
        assertEquals(1, propositionsList.size());

        final Map<String, Object> propositionsData = propositionsList.get(0);
        assertNotNull(propositionsData);
        final Proposition proposition = Proposition.fromEventData(propositionsData);
        assertNotNull(proposition);

        assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", proposition.getId());
        assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", proposition.getScope());
        assertTrue(proposition.getScopeDetails().isEmpty());
        assertEquals(1, proposition.getOffers().size());

        final Offer offer = proposition.getOffers().get(0);
        assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
        assertEquals("10", offer.getEtag());
        assertEquals("https://ns.adobe.com/experience/offer-management/content-component-html", offer.getSchema());
        assertEquals(OfferType.HTML, offer.getType());
        assertEquals("<h1>This is a HTML content</h1>", offer.getContent());
        assertEquals(1, offer.getCharacteristics().size());
        assertEquals("true", offer.getCharacteristics().get("testing"));
        assertNull(offer.getLanguage());

        final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
        assertNotNull(cachedPropositions);
        assertEquals(1, cachedPropositions.size());
        final DecisionScope cachedScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
        assertEquals(proposition, cachedPropositions.get(cachedScope));
    }

    @Test
    public void testHandleEdgeResponse_validPropositionFromTargetWithClickTracking() throws Exception {
        // setup
        final Map<String, Object> edgeResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_RESPONSE_VALID_TARGET_WITH_CLICK_TRACKING.json"), HashMap.class);
        final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                .setEventData(edgeResponseData)
                .build();

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleEdgeResponse(testEvent);

        // verify
        verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.notification", dispatchedEvent.getSource());

        final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
        assertNotNull(propositionsList);
        assertEquals(1, propositionsList.size());

        final Map<String, Object> propositionsData = propositionsList.get(0);
        assertNotNull(propositionsData);
        final Proposition proposition = Proposition.fromEventData(propositionsData);
        assertNotNull(proposition);

        assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTExMTExIiwiZXhwZXJpZW5jZUlkIjoiMCJ9", proposition.getId());
        assertEquals("myMbox", proposition.getScope());
        assertNotNull(proposition.getScopeDetails());

        final Map<String, Object> scopeDetails = proposition.getScopeDetails();
        assertNotNull(scopeDetails);
        assertEquals(5, scopeDetails.size());
        assertEquals("TGT", scopeDetails.get("decisionProvider"));
        final Map<String, Object> activity = (Map<String, Object>)scopeDetails.get("activity");
        assertNotNull(activity);
        assertEquals(1, activity.size());
        assertEquals("111111", activity.get("id"));
        Map<String, Object> experience = (Map<String, Object>)scopeDetails.get("experience");
        assertNotNull(experience);
        assertEquals(1, experience.size());
        assertEquals("0", experience.get("id"));
        final List<Map<String, Object>> strategies = (List<Map<String, Object>>)scopeDetails.get("strategies");
        assertNotNull(strategies);
        assertEquals(2, strategies.size());
        final Map<String, Object> strategy0 = strategies.get(0);
        assertNotNull(strategy0);
        assertEquals(3, strategy0.size());
        assertEquals("entry", strategy0.get("step"));
        assertEquals("0", strategy0.get("algorithmID"));
        assertEquals("0", strategy0.get("trafficType"));

        final Map<String, Object> strategy1 = strategies.get(1);
        assertNotNull(strategy1);
        assertEquals(3, strategy1.size());
        assertEquals("display", strategy1.get("step"));
        assertEquals("0", strategy1.get("algorithmID"));
        assertEquals("0", strategy1.get("trafficType"));

        final Map<String, Object> characteristics = (Map<String, Object>)scopeDetails.get("characteristics");
        assertNotNull(characteristics);
        assertEquals(2, characteristics.size());
        assertEquals("SGFZpwAqaqFTayhAT2xsgzG3+2fw4m+O9FK8c0QoOHfxVkH1ttT1PGBX3/jV8a5uFF0fAox6CXpjJ1PGRVQBjHl9Zc6mRxY9NQeM7rs/3Es1RHPkzBzyhpVS6eg9q+kw", characteristics.get("stateToken"));
        final Map<String, Object> eventTokens = (Map<String, Object>)characteristics.get("eventTokens");
        assertNotNull(eventTokens);
        assertEquals(2, eventTokens.size());
        assertEquals("MmvRrL5aB4Jz36JappRYg2qipfsIHvVzTQxHolz2IpSCnQ9Y9OaLL2gsdrWQTvE54PwSz67rmXWmSnkXpSSS2Q==", eventTokens.get("display"));
        assertEquals("EZDMbI2wmAyGcUYLr3VpmA==", eventTokens.get("click"));

        assertEquals(1, proposition.getOffers().size());
        final Offer offer = proposition.getOffers().get(0);
        assertEquals("0", offer.getId());
        assertNull(offer.getEtag());
        assertEquals("https://ns.adobe.com/personalization/json-content-item", offer.getSchema());
        assertEquals(OfferType.JSON, offer.getType());
        assertEquals("{\"device\":\"mobile\"}", offer.getContent());
        assertNull(offer.getCharacteristics());
        assertNull(offer.getLanguage());

        final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
        assertNotNull(cachedPropositions);
        assertEquals(1, cachedPropositions.size());
        final DecisionScope cachedScope = new DecisionScope("myMbox");
        assertEquals(proposition, cachedPropositions.get(cachedScope));
    }

    @Test
    public void testHandleEdgeResponse_nullEvent() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            extension.handleEdgeResponse(null);

            // verify
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString(), any()), times(1));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            assertNotNull(cachedPropositions);
            assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeResponse_nullEventData(){
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                    .setEventData(null)
                    .build();

            // test
            extension.handleEdgeResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString(), any()), times(1));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            assertNotNull(cachedPropositions);
            assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeResponse_emptyEventData() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                    .setEventData(new HashMap<>())
                    .build();

            // test
            extension.handleEdgeResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString(), any()), times(1));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            assertNotNull(cachedPropositions);
            assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeResponse_emptyProposition() throws Exception{
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Map<String, Object> edgeResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_RESPONSE_EMPTY_PAYLOAD.json"), HashMap.class);
            final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                    .setEventData(edgeResponseData)
                    .build();

            // test
            extension.handleEdgeResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString()), times(1));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            assertNotNull(cachedPropositions);
            assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeResponse_unsupportedItemInProposition() throws Exception{
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Map<String, Object> edgeResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_RESPONSE_UNSUPPORTED_ITEM_IN_PAYLOAD.json"), HashMap.class);
            final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                    .setEventData(edgeResponseData)
                    .build();

            // test
            extension.handleEdgeResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString()), times(2));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            assertNotNull(cachedPropositions);
            assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeResponse_missingEventHandleInData() throws Exception{
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Map<String, Object> edgeResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_RESPONSE_MISSING_EVENT_HANDLE.json"), HashMap.class);
            final Event testEvent = new Event.Builder("AEP Response Event Handle", "com.adobe.eventType.edge", "personalization:decisions")
                    .setEventData(edgeResponseData)
                    .build();

            // test
            extension.handleEdgeResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString()), times(1));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            assertNotNull(cachedPropositions);
            assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeErrorResponse() throws Exception{
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Map<String, Object> edgeErrorResponseData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_EDGE_ERROR_RESPONSE.json"), HashMap.class);
            final Event testEvent = new Event.Builder("AEP Error Response", "com.adobe.eventType.edge", "com.adobe.eventSource.errorResponseContent")
                    .setEventData(edgeErrorResponseData)
                    .build();

            // test
            extension.handleEdgeErrorResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.warning(anyString(), anyString(), anyString(), any()), times(1));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            assertNotNull(cachedPropositions);
            assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeErrorResponse_nullEvent() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // test
            extension.handleEdgeErrorResponse(null);

            // verify
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString(), any()), times(1));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            assertNotNull(cachedPropositions);
            assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeErrorResponse_nullEventData() {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Event testEvent = new Event.Builder("AEP Error Response", "com.adobe.eventType.edge", "com.adobe.eventSource.errorResponseContent")
                    .setEventData(null)
                    .build();

            // test
            extension.handleEdgeErrorResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString(), any()), times(1));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            assertNotNull(cachedPropositions);
            assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleEdgeErrorResponse_emptyEventData(){
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Event testEvent = new Event.Builder("AEP Error Response", "com.adobe.eventType.edge", "com.adobe.eventSource.errorResponseContent")
                    .setEventData(new HashMap<>())
                    .build();

            // test
            extension.handleEdgeErrorResponse(testEvent);

            // verify
            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString(), any()), times(1));

            final Map<DecisionScope, Proposition> cachedPropositions = extension.getCachedPropositions();
            assertNotNull(cachedPropositions);
            assertTrue(cachedPropositions.isEmpty());
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleGetPropositions_decisionScopeInCache() throws Exception{
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(anyString(), anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));
            // setup
            setConfigurationSharedState(new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
            final Proposition testProposition = Proposition.fromEventData(testPropositionData);
            assertNotNull(testProposition);
            final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
            cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
            extension.setCachedPropositions(cachedPropositions);

            final DecisionScope testScope = new DecisionScope("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "getpropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope.toEventData());
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Get Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
            assertEquals("com.adobe.eventSource.responseContent", dispatchedEvent.getSource());
            assertEquals(testEvent.getUniqueIdentifier(), dispatchedEvent.getResponseID());

            final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
            assertNotNull(propositionsList);
            assertEquals(1, propositionsList.size());

            final Map<String, Object> propositionData = propositionsList.get(0);
            assertNotNull(propositionData);
            final Proposition proposition = Proposition.fromEventData(propositionData);
            assertNotNull(proposition);

            assertEquals("de03ac85-802a-4331-a905-a57053164d35", proposition.getId());
            assertEquals("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", proposition.getScope());
            assertTrue(proposition.getScopeDetails().isEmpty());
            assertEquals(1, proposition.getOffers().size());

            Offer offer = proposition.getOffers().get(0);
            assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
            assertEquals("10", offer.getEtag());
            assertEquals("https://ns.adobe.com/experience/offer-management/content-component-html", offer.getSchema());
            assertEquals(OfferType.HTML, offer.getType());
            assertEquals("<h1>This is a HTML content</h1>", offer.getContent());
            assertNull(offer.getLanguage());
            assertNull(offer.getCharacteristics());
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleGetPropositions_notAllDecisionScopesInCache() throws Exception{
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(anyString(), anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));
            // setup
            setConfigurationSharedState(new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
            final Proposition testProposition = Proposition.fromEventData(testPropositionData);
            assertNotNull(testProposition);
            final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
            cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
            extension.setCachedPropositions(cachedPropositions);

            final DecisionScope testScope1 = new DecisionScope("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final DecisionScope testScope2 = new DecisionScope("myMbox");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "getpropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope1.toEventData());
                    add(testScope2.toEventData());
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Get Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
            assertEquals("com.adobe.eventSource.responseContent", dispatchedEvent.getSource());
            assertEquals(testEvent.getUniqueIdentifier(), dispatchedEvent.getResponseID());

            final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
            assertNotNull(propositionsList);
            assertEquals(1, propositionsList.size());

            final Map<String, Object> propositionData = propositionsList.get(0);
            assertNotNull(propositionData);
            final Proposition proposition = Proposition.fromEventData(propositionData);
            assertNotNull(proposition);

            assertEquals("de03ac85-802a-4331-a905-a57053164d35", proposition.getId());
            assertEquals("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", proposition.getScope());
            assertTrue(proposition.getScopeDetails().isEmpty());
            assertEquals(1, proposition.getOffers().size());

            Offer offer = proposition.getOffers().get(0);
            assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
            assertEquals("10", offer.getEtag());
            assertEquals("https://ns.adobe.com/experience/offer-management/content-component-html", offer.getSchema());
            assertEquals(OfferType.HTML, offer.getType());
            assertEquals("<h1>This is a HTML content</h1>", offer.getContent());
            assertNull(offer.getLanguage());
            assertNull(offer.getCharacteristics());
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleGetPropositions_noDecisionScopeInCache() throws Exception {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(anyString(), anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
            final Proposition testProposition = Proposition.fromEventData(testPropositionData);
            assertNotNull(testProposition);
            final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
            cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
            extension.setCachedPropositions(cachedPropositions);

            final DecisionScope testScope1 = new DecisionScope("myMbox1");
            final DecisionScope testScope2 = new DecisionScope("myMbox2");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "getpropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope1.toEventData());
                    add(testScope2.toEventData());
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Get Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
            assertEquals("com.adobe.eventSource.responseContent", dispatchedEvent.getSource());
            assertEquals(testEvent.getUniqueIdentifier(), dispatchedEvent.getResponseID());

            final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
            assertNotNull(propositionsList);
            assertEquals(0, propositionsList.size());
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleGetPropositions_missingDecisionScopesList() throws Exception {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(anyString(), anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
            final Proposition testProposition = Proposition.fromEventData(testPropositionData);
            assertNotNull(testProposition);
            final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
            cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
            extension.setCachedPropositions(cachedPropositions);

            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "getpropositions");

            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Get Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
            assertEquals("com.adobe.eventSource.responseContent", dispatchedEvent.getSource());
            assertEquals(testEvent.getUniqueIdentifier(), dispatchedEvent.getResponseID());

            final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
            assertNull(propositionsList);

            final String error = (String) dispatchedEvent.getEventData().get("responseerror");
            assertEquals(AdobeError.UNEXPECTED_ERROR.getErrorName(), error);
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleGetPropositions_emptyCachedPropositions(){
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic.when(() -> Base64.decode(anyString(), anyInt()))
                    .thenAnswer((Answer<byte[]>) invocation -> java.util.Base64.getDecoder().decode((String) invocation.getArguments()[0]));

            // setup
            setConfigurationSharedState(new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
            extension.setCachedPropositions(cachedPropositions);

            final DecisionScope testScope = new DecisionScope("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            final Map<String, Object> testEventData = new HashMap<>();
            testEventData.put("requesttype", "getpropositions");
            testEventData.put("decisionscopes", new ArrayList<Map<String, Object>>() {
                {
                    add(testScope.toEventData());
                }
            });
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            final Event testEvent = new Event.Builder("Optimize Get Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(testEventData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            final Event dispatchedEvent = eventCaptor.getValue();
            assertEquals("com.adobe.eventType.optimize", dispatchedEvent.getType());
            assertEquals("com.adobe.eventSource.responseContent", dispatchedEvent.getSource());
            assertEquals(testEvent.getUniqueIdentifier(), dispatchedEvent.getResponseID());

            final List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) dispatchedEvent.getEventData().get("propositions");
            assertNotNull(propositionsList);
            assertEquals(0, propositionsList.size());
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleTrackPropositions_validPropositionInteractionsForDisplay() throws Exception{
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> optimizeTrackRequestData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_OPTIMIZE_TRACK_REQUEST_VALID_DISPLAY.json"), HashMap.class);
        final Event testEvent = new Event.Builder("Optimize Track Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(optimizeTrackRequestData)
                .build();

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleOptimizeRequestContent(testEvent);

        // verify
        verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());
        final Map<String, Object> eventData = dispatchedEvent.getEventData();
        assertNotNull(eventData);
        final Map<String, Object> propositionInteractionsXdm = (Map<String, Object>)eventData.get("xdm");
        assertNotNull(propositionInteractionsXdm);
        assertEquals("decisioning.propositionDisplay", propositionInteractionsXdm.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionInteractionsXdm.get("_experience");
        assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        assertNotNull(propositionInteractionDetailsList);
        assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = propositionInteractionDetailsList.get(0);
        assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", propositionInteractionDetailsMap.get("id"));
        assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>)propositionInteractionDetailsMap.get("scopeDetails");
        assertNotNull(scopeDetails);
        assertTrue(scopeDetails.isEmpty());
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("xcore:personalized-offer:1111111111111111", items.get(0).get("id"));
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleTrackPropositions_validPropositionInteractionsForTap() throws Exception{
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
            }
        });

        final Map<String, Object> optimizeTrackRequestData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_OPTIMIZE_TRACK_REQUEST_VALID_TAP.json"), HashMap.class);
        final Event testEvent = new Event.Builder("Optimize Track Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(optimizeTrackRequestData)
                .build();

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleOptimizeRequestContent(testEvent);

        // verify
        verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());
        final Map<String, Object> eventData = dispatchedEvent.getEventData();
        assertNotNull(eventData);
        final Map<String, Object> propositionInteractionsXdm = (Map<String, Object>)eventData.get("xdm");
        assertNotNull(propositionInteractionsXdm);
        assertEquals("decisioning.propositionInteract", propositionInteractionsXdm.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionInteractionsXdm.get("_experience");
        assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        assertNotNull(propositionInteractionDetailsList);
        assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = propositionInteractionDetailsList.get(0);
        assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9", propositionInteractionDetailsMap.get("id"));
        assertEquals("myMbox", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>)propositionInteractionDetailsMap.get("scopeDetails");
        assertNotNull(scopeDetails);
        assertEquals(4, scopeDetails.size());
        assertEquals("TGT", scopeDetails.get("decisionProvider"));
        final Map<String, Object> sdActivity = (Map<String, Object>)scopeDetails.get("activity");
        assertEquals("125589", sdActivity.get("id"));
        final Map<String, Object> sdExperience = (Map<String, Object>)scopeDetails.get("experience");
        assertEquals("0", sdExperience.get("id"));
        final List<Map<String, Object>> sdStrategies = (List<Map<String, Object>>)scopeDetails.get("strategies");
        assertNotNull(sdStrategies);
        assertEquals(1, sdStrategies.size());
        assertEquals("0", sdStrategies.get(0).get("algorithmID"));
        assertEquals("0", sdStrategies.get(0).get("trafficType"));
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("246315", items.get(0).get("id"));
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleTrackPropositions_validPropositionInteractionsWithDatasetIdInConfig() throws Exception{
        // setup
        setConfigurationSharedState(new HashMap<String, Object>() {
            {
                put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                put("optimize.datasetId", "111111111111111111111111");
            }
        });

        final Map<String, Object> optimizeTrackRequestData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_OPTIMIZE_TRACK_REQUEST_VALID_TAP.json"), HashMap.class);
        final Event testEvent = new Event.Builder("Optimize Track Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                .setEventData(optimizeTrackRequestData)
                .build();

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleOptimizeRequestContent(testEvent);

        // verify
        verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

        final Event dispatchedEvent = eventCaptor.getValue();
        assertEquals("com.adobe.eventType.edge", dispatchedEvent.getType());
        assertEquals("com.adobe.eventSource.requestContent", dispatchedEvent.getSource());
        final Map<String, Object> eventData = dispatchedEvent.getEventData();
        assertNotNull(eventData);
        final String datasetId = (String)eventData.get("datasetId");
        assertEquals("111111111111111111111111", datasetId);
        final Map<String, Object> propositionInteractionsXdm = (Map<String, Object>)eventData.get("xdm");
        assertNotNull(propositionInteractionsXdm);
        assertEquals("decisioning.propositionInteract", propositionInteractionsXdm.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionInteractionsXdm.get("_experience");
        assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        assertNotNull(propositionInteractionDetailsList);
        assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = propositionInteractionDetailsList.get(0);
        assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9", propositionInteractionDetailsMap.get("id"));
        assertEquals("myMbox", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>)propositionInteractionDetailsMap.get("scopeDetails");
        assertNotNull(scopeDetails);
        assertEquals(4, scopeDetails.size());
        assertEquals("TGT", scopeDetails.get("decisionProvider"));
        final Map<String, Object> sdActivity = (Map<String, Object>)scopeDetails.get("activity");
        assertEquals("125589", sdActivity.get("id"));
        final Map<String, Object> sdExperience = (Map<String, Object>)scopeDetails.get("experience");
        assertEquals("0", sdExperience.get("id"));
        final List<Map<String, Object>> sdStrategies = (List<Map<String, Object>>)scopeDetails.get("strategies");
        assertNotNull(sdStrategies);
        assertEquals(1, sdStrategies.size());
        assertEquals("0", sdStrategies.get(0).get("algorithmID"));
        assertEquals("0", sdStrategies.get(0).get("trafficType"));
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("246315", items.get(0).get("id"));
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleTrackPropositions_configurationNotAvailable() throws Exception{
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Map<String, Object> optimizeTrackRequestData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_OPTIMIZE_TRACK_REQUEST_VALID_DISPLAY.json"), HashMap.class);
            final Event testEvent = new Event.Builder("Optimize Track Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(optimizeTrackRequestData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(0)).dispatch(any());

            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString(), any()), times(1));
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleTrackPropositions_missingPropositionInteractions() throws Exception{
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            setConfigurationSharedState(new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<String, Object> optimizeTrackRequestData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_OPTIMIZE_TRACK_REQUEST_MISSING_PROPOSITION_INTERACTIONS.json"), HashMap.class);
            final Event testEvent = new Event.Builder("Optimize Track Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(optimizeTrackRequestData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(0)).dispatch(any());

            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString(), any()), times(1));
        }
    }

    @Test
    public void testHandleOptimizeRequestContent_HandleTrackPropositions_emptyPropositionInteractions() throws Exception{
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            setConfigurationSharedState(new HashMap<String, Object>() {
                {
                    put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
                }
            });

            final Map<String, Object> optimizeTrackRequestData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/EVENT_DATA_OPTIMIZE_TRACK_REQUEST_EMPTY_PROPOSITION_INTERACTIONS.json"), HashMap.class);
            final Event testEvent = new Event.Builder("Optimize Track Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestContent")
                    .setEventData(optimizeTrackRequestData)
                    .build();

            // test
            extension.handleOptimizeRequestContent(testEvent);

            // verify
            verify(mockExtensionApi, times(0)).dispatch(any());

            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString(), any()), times(1));
        }
    }

    @Test
    public void testHandleClearPropositions() throws Exception{
        // setup
        final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition testProposition = Proposition.fromEventData(testPropositionData);
        assertNotNull(testProposition);
        final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
        cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
        extension.setCachedPropositions(cachedPropositions);

        final Event testEvent = new Event.Builder("Optimize Clear Propositions Request", "com.adobe.eventType.optimize", "com.adobe.eventSource.requestReset")
                .build();

        // test
        extension.handleClearPropositions(testEvent);

        // verify
        final Map<DecisionScope, Proposition> actualCachedPropositions = extension.getCachedPropositions();
        assertTrue(actualCachedPropositions.isEmpty());
    }

    @Test
    public void testHandleClearPropositions_coreResetIdentities() throws Exception{
        // setup
        final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
        final Proposition testProposition = Proposition.fromEventData(testPropositionData);
        assertNotNull(testProposition);
        final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
        cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
        extension.setCachedPropositions(cachedPropositions);

        final Event testEvent = new Event.Builder("Reset Identities Request", "com.adobe.eventType.generic.identity", "com.adobe.eventSource.requestReset")
                .build();

        // test
        extension.handleClearPropositions(testEvent);

        // verify
        final Map<DecisionScope, Proposition> actualCachedPropositions = extension.getCachedPropositions();
        assertTrue(actualCachedPropositions.isEmpty());
    }

    @Test
    public void testHandleClearPropositions_nullEvent() throws Exception{
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            final Map<String, Object> testPropositionData = new ObjectMapper().readValue(getClass().getClassLoader().getResource("json/PROPOSITION_VALID.json"), HashMap.class);
            final Proposition testProposition = Proposition.fromEventData(testPropositionData);
            assertNotNull(testProposition);
            final Map<DecisionScope, Proposition> cachedPropositions = new HashMap<>();
            cachedPropositions.put(new DecisionScope(testProposition.getScope()), testProposition);
            extension.setCachedPropositions(cachedPropositions);

            // test
            extension.handleClearPropositions(null);

            // verify
            final Map<DecisionScope, Proposition> actualCachedPropositions = extension.getCachedPropositions();
            assertEquals(cachedPropositions, actualCachedPropositions);

            logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), anyString(), any()), times(1));
        }
    }


    // Helper methods
    private void setConfigurationSharedState(final Map<String, Object> data) {
        Map<String, Object> configurationSharedState = new HashMap<>();
        configurationSharedState.put(OptimizeConstants.Configuration.OPTIMIZE_OVERRIDE_DATASET_ID, "ffffffff-ffff-ffff-ffff-ffffffffffff");
        Mockito.when(mockExtensionApi.getSharedState(
                eq(OptimizeConstants.Configuration.EXTENSION_NAME),
                any(),
                eq(false),
                eq(SharedStateResolution.ANY)
        )).thenReturn(new SharedStateResult(SharedStateStatus.SET, data));
    }
}

