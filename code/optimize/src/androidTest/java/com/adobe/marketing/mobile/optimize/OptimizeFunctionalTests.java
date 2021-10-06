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

import static com.adobe.marketing.mobile.TestHelper.resetTestExpectations;

import android.util.JsonReader;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.adobe.marketing.mobile.ADBCountDownLatch;
import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.MockNetworkService;
import com.adobe.marketing.mobile.MockNetworkService.MockHttpConnecting;
import com.adobe.marketing.mobile.TestConstants;
import com.adobe.marketing.mobile.TestHelper;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.mockito.internal.matchers.Any;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class OptimizeFunctionalTests {

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(new TestHelper.SetupCoreRule()).
            around(new TestHelper.RegisterMonitorExtensionRule());

    @Before
    public void setup() throws Exception {
        Optimize.registerExtension();
        com.adobe.marketing.mobile.edge.identity.Identity.registerExtension();

        final CountDownLatch latch = new CountDownLatch(1);
        MobileCore.start(new AdobeCallback() {
            @Override
            public void call(Object o) {
                latch.countDown();
            }
        });

        latch.await();
        resetTestExpectations();
    }

    //1
    @Test
    public void testExtensionVersion() {
        Assert.assertEquals(OptimizeConstants.EXTENSION_VERSION, Optimize.extensionVersion());
    }

    //2
    @Test
    public void testUpdatePropositions_validDecisionScope() {
        //Setup
        final String decisionScopeName = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);
        ServiceProvider.getInstance().setNetworkService(new MockNetworkService(new MockHttpConnecting(200, "{}")));

        //Action
        Optimize.updatePropositions(Arrays.asList(new DecisionScope(decisionScopeName)), null, null);

        //Assert
        List<Event> eventsListOptimize = null;
        List<Event> eventsListEdge = null;
        try {
            eventsListOptimize = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.REQUEST_CONTENT, 1000);
            eventsListEdge = TestHelper.getDispatchedEventsWith(TestConstants.EventType.EDGE, TestConstants.EventSource.REQUEST_CONTENT, 1000);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Optimize Request Content Events.");
        }

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertNotNull(eventsListEdge);
        Assert.assertEquals(1, eventsListEdge.size());
        Event event = eventsListOptimize.get(0);
        Map<String, Object> eventData = event.getEventData();
        Assert.assertEquals(TestConstants.EventType.OPTIMIZE.toLowerCase(), event.getType());
        Assert.assertEquals(TestConstants.EventSource.REQUEST_CONTENT.toLowerCase(), event.getSource());
        Assert.assertTrue(eventData.size() > 0);
        Assert.assertEquals("updatepropositions", (String) eventData.get("requesttype"));
        List<Map<String, String>> decisionScopes = (List<Map<String, String>>) eventData.get("decisionscopes");
        Assert.assertEquals(1, decisionScopes.size());
        Assert.assertEquals(decisionScopeName, decisionScopes.get(0).get("name"));
    }

    //3
    @Test
    public void testUpdatePropositions_validDecisionScopeWithXdmAndDataAndDatasetId() {
        //Setup
        final String decisionScopeName = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";
        Map<String, Object> xdmMap = new HashMap<String, Object>() {
            {
                put("MyXDMKey", "MyXDMValue");
            }
        };

        Map<String, Object> dataMap = new HashMap<String, Object>() {
            {
                put("MyDataKey", "MyDataValue");
            }
        };

        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        configData.put("optimize.datasetId", "111111111111111111111111");
        MobileCore.updateConfiguration(configData);
        ServiceProvider.getInstance().setNetworkService(new MockNetworkService(new MockHttpConnecting(200, "{}")));

        Optimize.updatePropositions(Arrays.asList(new DecisionScope(decisionScopeName)), xdmMap, dataMap);

        //Assert
        List<Event> eventsListOptimize = null;
        List<Event> eventsListEdge = null;
        try {
            eventsListOptimize = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.REQUEST_CONTENT, 1000);
            eventsListEdge = TestHelper.getDispatchedEventsWith(TestConstants.EventType.EDGE, TestConstants.EventSource.REQUEST_CONTENT, 1000);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Optimize Request Content Events.");
        }

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertNotNull(eventsListEdge);
        Assert.assertEquals(1, eventsListEdge.size());
        Event event = eventsListOptimize.get(0);
        Map<String, Object> eventData = event.getEventData();
        Assert.assertEquals(TestConstants.EventType.OPTIMIZE, event.getType());
        Assert.assertEquals(TestConstants.EventSource.REQUEST_CONTENT, event.getSource());
        Assert.assertTrue(eventData.size() > 0);
        Assert.assertEquals("MyXDMValue", ((Map<String, String>) eventData.get("xdm")).get("MyXDMKey"));
        Assert.assertEquals("MyDataValue", ((Map<String, String>) eventData.get("data")).get("MyDataKey"));
        Assert.assertEquals("updatepropositions", (String) eventData.get("requesttype"));
        List<Map<String, String>> decisionScopes = (List<Map<String, String>>) eventData.get("decisionscopes");
        Assert.assertEquals(1, decisionScopes.size());
        Assert.assertEquals(decisionScopeName, decisionScopes.get(0).get("name"));
    }

    //4
    @Test
    public void testUpdatePropositions_multipleValidDecisionScope() {
        //Setup
        final String decisionScopeName1 = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";
        final String decisionScopeName2 = "MyMbox";
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);
        ServiceProvider.getInstance().setNetworkService(new MockNetworkService(new MockHttpConnecting(200, "{}")));

        //Action
        Optimize.updatePropositions(
                Arrays.asList(new DecisionScope(decisionScopeName1), new DecisionScope(decisionScopeName2))
                , null, null);

        //Assert
        List<Event> eventsListOptimize = null;
        List<Event> eventsListEdge = null;
        try {
            eventsListOptimize = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.REQUEST_CONTENT, 1000);
            eventsListEdge = TestHelper.getDispatchedEventsWith(TestConstants.EventType.EDGE, TestConstants.EventSource.REQUEST_CONTENT, 1000);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Optimize Request Content Events.");
        }

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertNotNull(eventsListEdge);
        Assert.assertEquals(1, eventsListEdge.size());
        Event event = eventsListOptimize.get(0);
        Map<String, Object> eventData = event.getEventData();
        Assert.assertEquals(TestConstants.EventType.OPTIMIZE.toLowerCase(), event.getType());
        Assert.assertEquals(TestConstants.EventSource.REQUEST_CONTENT.toLowerCase(), event.getSource());
        Assert.assertTrue(eventData.size() > 0);
        Assert.assertEquals("updatepropositions", (String) eventData.get("requesttype"));
        List<Map<String, String>> decisionScopes = (List<Map<String, String>>) eventData.get("decisionscopes");
        Assert.assertEquals(2, decisionScopes.size());
        Assert.assertEquals(decisionScopeName1, decisionScopes.get(0).get("name"));
        Assert.assertEquals(decisionScopeName2, decisionScopes.get(1).get("name"));
    }

    //5
    @Test
    public void testUpdatePropositions_missingEventRequestTypeInData() {
        //setup
        List<DecisionScope> decisionScopesList = Arrays.asList(new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);
        final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                Assert.fail("Error in dispatching Optimize Request content event");
            }
        };

        final List<Map<String, Object>> flattenedDecisionScopes = new ArrayList<Map<String, Object>>();
        for (final DecisionScope scope : decisionScopesList) {
            flattenedDecisionScopes.add(scope.toEventData());
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put("decisionscopes", flattenedDecisionScopes);

        final Event event = new Event.Builder(OptimizeConstants.EventNames.UPDATE_PROPOSITIONS_REQUEST,
                OptimizeConstants.EventType.OPTIMIZE,
                OptimizeConstants.EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(event, errorCallback);

        //Assert
        List<Event> eventsListOptimize = null;
        List<Event> eventsListEdge = null;
        try {
            eventsListOptimize = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.REQUEST_CONTENT, 1000);
            eventsListEdge = TestHelper.getDispatchedEventsWith(TestConstants.EventType.EDGE, TestConstants.EventSource.REQUEST_CONTENT, 1000);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Optimize Request Content Events.");
        }

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertTrue(eventsListEdge.isEmpty());
    }

    //6
    @Test
    public void testUpdatePropositions_ConfigNotAvailable() {
        //Setup
        final String decisionScopeName = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";
        ServiceProvider.getInstance().setNetworkService(new MockNetworkService(new MockHttpConnecting(200, "{}")));
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", null);
        configData.put("optimize.datasetId", null);
        MobileCore.updateConfiguration(null);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Action
        Optimize.updatePropositions(Arrays.asList(new DecisionScope(decisionScopeName)), null, null);

        //Assert
        List<Event> eventsListOptimize = null;
        List<Event> eventsListEdge = null;
        try {
            eventsListOptimize = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.REQUEST_CONTENT, 1000);
            eventsListEdge = TestHelper.getDispatchedEventsWith(TestConstants.EventType.EDGE, TestConstants.EventSource.REQUEST_CONTENT, 1000);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Optimize Request Content Events.");
        }

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertNotNull(eventsListEdge);
        Assert.assertTrue(eventsListEdge.isEmpty());
    }

    //7
    @Test
    public void testUpdatePropositions_noDecisionScopes() {
        //setup
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);
        final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                Assert.fail("Error in dispatching Optimize Request content event");
            }
        };

        final List<Map<String, Object>> flattenedDecisionScopes = Collections.emptyList();

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put("requesttype", "updatepropositions");
        eventData.put("decisionscopes", flattenedDecisionScopes);

        final Event event = new Event.Builder(OptimizeConstants.EventNames.UPDATE_PROPOSITIONS_REQUEST,
                OptimizeConstants.EventType.OPTIMIZE,
                OptimizeConstants.EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(event, errorCallback);

        //Assert
        List<Event> eventsListOptimize = null;
        List<Event> eventsListEdge = null;
        try {
            eventsListOptimize = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.REQUEST_CONTENT, 1000);
            eventsListEdge = TestHelper.getDispatchedEventsWith(TestConstants.EventType.EDGE, TestConstants.EventSource.REQUEST_CONTENT, 1000);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Optimize Request Content Events.");
        }

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertTrue(eventsListEdge.isEmpty());
    }

    //8
    @Test
    public void testUpdatePropositions_validAndInvalidDecisionScopes() {
        //Setup
        final String decisionScopeName1 = "eyJhY3Rpdml0eUlkIjoiIiwicGxhY2VtZW50SWQiOiJ4Y29yZTpvZmZlci1wbGFjZW1lbnQ6MTExMTExMTExMTExMTExMSJ9";
        final String decisionScopeName2 = "MyMbox";
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);
        ServiceProvider.getInstance().setNetworkService(new MockNetworkService(new MockHttpConnecting(200, "{}")));

        //Action
        Optimize.updatePropositions(
                Arrays.asList(new DecisionScope(decisionScopeName1), new DecisionScope(decisionScopeName2))
                , null, null);

        //Assert
        List<Event> eventsListOptimize = null;
        List<Event> eventsListEdge = null;
        try {
            eventsListOptimize = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.REQUEST_CONTENT, 1000);
            eventsListEdge = TestHelper.getDispatchedEventsWith(TestConstants.EventType.EDGE, TestConstants.EventSource.REQUEST_CONTENT, 1000);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Optimize Request Content Events.");
        }

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertNotNull(eventsListEdge);
        Assert.assertEquals(1, eventsListEdge.size());
        Event event = eventsListOptimize.get(0);
        Map<String, Object> eventData = event.getEventData();
        Assert.assertEquals(TestConstants.EventType.OPTIMIZE.toLowerCase(), event.getType());
        Assert.assertEquals(TestConstants.EventSource.REQUEST_CONTENT.toLowerCase(), event.getSource());
        Assert.assertTrue(eventData.size() > 0);
        Assert.assertEquals("updatepropositions", (String) eventData.get("requesttype"));
        List<Map<String, String>> decisionScopes = (List<Map<String, String>>) eventData.get("decisionscopes");
        Assert.assertEquals(1, decisionScopes.size());
        Assert.assertEquals(decisionScopeName2, decisionScopes.get(0).get("name"));
    }

    //9
    @Test
    public void testEdgeResponse_ValidProposition() {
        // setup
        final String data = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                "                                        \"scope\": \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==\",\n" +
                "                                        \"activity\": {\n" +
                "                                            \"etag\": \"8\",\n" +
                "                                            \"id\": \"xcore:offer-activity:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"placement\": {\n" +
                "                                            \"etag\": \"1\",\n" +
                "                                            \"id\": \"xcore:offer-placement:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                \"etag\": \"10\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/experience/offer-management/content-component-html\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                    \"format\": \"text/html\",\n" +
                "                                                    \"content\": \"<h1>This is HTML content</h1>\",\n" +
                "                                                    \"characteristics\": {\n" +
                "                                                        \"testing\": \"true\"\n" +
                "                                                    }\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = null;
        try {
            eventData = objectMapper.readValue(data, Map.class);
        } catch (IOException e) {
            Assert.fail("Error in converting JSON payload to Map.");
        }

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                TestConstants.EventType.EDGE,
                TestConstants.EventSource.PERSONALIZATION).setEventData(eventData).build();

        //Action
        MobileCore.dispatchEvent(event, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Assert.fail("Error in dispatching Edge Personalization event.");
            }
        });

        //Assert
        List<Event> eventsListOptimizeNotification = null;
        try {
            eventsListOptimizeNotification = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.NOTIFICATION, 1000);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Notification Events.");
        }

        Assert.assertNotNull(eventsListOptimizeNotification);

        Map<String, Object> propositionData = ((List<Map<String, Object>>) eventsListOptimizeNotification.get(0).getEventData().get("propositions")).get(0);
        Proposition proposition = Proposition.fromEventData(propositionData);
        Assert.assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", proposition.getId());
        Assert.assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", proposition.getScope());
        Assert.assertEquals(1, proposition.getOffers().size());
        Assert.assertEquals("xcore:personalized-offer:1111111111111111", proposition.getOffers().get(0).getId());
        Assert.assertEquals("https://ns.adobe.com/experience/offer-management/content-component-html", proposition.getOffers().get(0).getSchema());
        Assert.assertEquals(OfferType.HTML, proposition.getOffers().get(0).getType());
        Assert.assertEquals("<h1>This is HTML content</h1>", proposition.getOffers().get(0).getContent());
        Assert.assertEquals(1, proposition.getOffers().get(0).getCharacteristics().size());
        Assert.assertEquals("true", proposition.getOffers().get(0).getCharacteristics().get("testing"));
    }

    //10
    @Test
    public void testEdgeResponse_validPropositionFromTargetWithClickTracking() {

        // setup
        final String data = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"AT:eyJhY3Rpdml0eUlkIjoiMTExMTExIiwiZXhwZXJpZW5jZUlkIjoiMCJ9\",\n" +
                "                                        \"scope\": \"myMbox1\",\n" +
                "                                        \"scopeDetails\": {\n" +
                "                                           \"decisionProvider\": \"TGT\",\n" +
                "                                           \"activity\": {\n" +
                "                                               \"id\": \"111111\"\n" +
                "                                             },\n" +
                "                                           \"experience\": {\n" +
                "                                               \"id\": \"0\"\n" +
                "                                             },\n" +
                "                                           \"strategies\": [\n" +
                "                                               {\n" +
                "                                                   \"algorithmID\": \"0\",\n" +
                "                                                   \"trafficType\": \"0\"\n" +
                "                                               }\n" +
                "                                           ]\n" +
                "                                         },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"0\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/personalization/json-content-item\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"id\": \"0\",\n" +
                "                                                    \"format\": \"application/json\",\n" +
                "                                                    \"content\": {\n" +
                "                                                        \"device\": \"mobile\"\n" +
                "                                                    }\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    },\n" +
                "                                    {\n" +
                "                                        \"id\": \"AT:eyJhY3Rpdml0eUlkIjoiMTExMTExIiwiZXhwZXJpZW5jZUlkIjoiMCJ9\",\n" +
                "                                        \"scope\": \"myMbox1\",\n" +
                "                                        \"scopeDetails\": {\n" +
                "                                            \"activity\": {\n" +
                "                                                \"id\": \"111111\"\n" +
                "                                            },\n" +
                "                                            \"decisionProvider\": \"TGT\"\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"111111\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/personalization/measurement\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"type\": \"click\",\n" +
                "                                                    \"format\": \"application/vnd.adobe.target.metric\"\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = null;
        try {
            eventData = objectMapper.readValue(data, Map.class);
        } catch (IOException e) {
            Assert.fail("Error in converting JSON payload to Map.");
        }

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                TestConstants.EventType.EDGE,
                TestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).build();

        //Action
        MobileCore.dispatchEvent(event, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Assert.fail("Error in dispatching Edge Personalization event.");
            }
        });

        //Assert
        List<Event> eventsListOptimizeNotification = null;
        try {
            eventsListOptimizeNotification = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.NOTIFICATION, 1000);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Optimize Notification Events.");
        }

        Assert.assertNotNull(eventsListOptimizeNotification);

        Map<String, Object> propositionData = ((List<Map<String, Object>>) eventsListOptimizeNotification.get(0).getEventData().get("propositions")).get(0);
        Proposition proposition = Proposition.fromEventData(propositionData);
        Assert.assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTExMTExIiwiZXhwZXJpZW5jZUlkIjoiMCJ9", proposition.getId());
        Assert.assertEquals("myMbox1", proposition.getScope());
        Assert.assertEquals(1, proposition.getOffers().size());
        Assert.assertEquals("0", proposition.getOffers().get(0).getId());
        Assert.assertEquals("https://ns.adobe.com/personalization/json-content-item", proposition.getOffers().get(0).getSchema());
        Assert.assertEquals(OfferType.JSON, proposition.getOffers().get(0).getType());
        Assert.assertEquals("{\"device\":\"mobile\"}", proposition.getOffers().get(0).getContent());
        Assert.assertNull(proposition.getOffers().get(0).getCharacteristics());
        Assert.assertNull(proposition.getOffers().get(0).getLanguage());
    }

    //11
    @Test
    public void testEdgeResponse_emptyProposition(){
        final String data = "{\n" +
                "                                \"payload\": [],\n" +
                "                                \"requestEventId\": \"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = null;
        try {
            eventData = objectMapper.readValue(data, Map.class);
        } catch (IOException e) {
            Assert.fail("Error in converting JSON payload to Map.");
        }

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                TestConstants.EventType.EDGE,
                TestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).build();

        //Action
        MobileCore.dispatchEvent(event, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Assert.fail("Error in dispatching Edge Personalization event.");
            }
        });

        //Assertion
        List<Event> optimizeNotificationEventsList = null;
        try {
            optimizeNotificationEventsList = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.NOTIFICATION);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Optimize Notification events.");
        }
        Assert.assertTrue(optimizeNotificationEventsList.isEmpty());
    }

    //12
    @Test
    public void testEdgeResponse_unsupportedItemInProposition() {
        final String data = "{\n" +
                "                                \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"AT:eyJhY3Rpdml0eUlkIjoiMTExMTExIiwiZXhwZXJpZW5jZUlkIjoiMCJ9\",\n" +
                "                                        \"scope\": \"myMbox1\",\n" +
                "                                        \"scopeDetails\": {\n" +
                "                                            \"activity\": {\n" +
                "                                                \"id\": \"111111\"\n" +
                "                                            },\n" +
                "                                            \"decisionProvider\": \"TGT\"\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"111111\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/personalization/measurement\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"type\": \"click\",\n" +
                "                                                    \"format\": \"application/vnd.adobe.target.metric\"\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                ],\n" +
                "                                \"requestEventId\": \"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = null;
        try {
            eventData = objectMapper.readValue(data, Map.class);
        } catch (IOException e) {
            Assert.fail("Error in converting JSON payload to Map.");
        }

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                TestConstants.EventType.EDGE,
                TestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).build();

        //Action
        MobileCore.dispatchEvent(event, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Assert.fail("Error in dispatching Edge Personalization event.");
            }
        });

        //Assertion
        List<Event> optimizeNotificationEventsList = null;
        try {
            optimizeNotificationEventsList = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.NOTIFICATION);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Optimize Notification events.");
        }
        Assert.assertTrue(optimizeNotificationEventsList.isEmpty());
    }

    //13
    @Test
    public void testEdgeResponse_missingEventHandleInData() {
        final String data = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                "                                        \"scope\": \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==\",\n" +
                "                                        \"activity\": {\n" +
                "                                            \"etag\": \"8\",\n" +
                "                                            \"id\": \"xcore:offer-activity:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"placement\": {\n" +
                "                                            \"etag\": \"1\",\n" +
                "                                            \"id\": \"xcore:offer-placement:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                \"etag\": \"10\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/experience/offer-management/content-component-html\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                    \"format\": \"text/html\",\n" +
                "                                                    \"content\": \"<h1>This is HTML content</h1>\",\n" +
                "                                                    \"characteristics\": {\n" +
                "                                                        \"testing\": \"true\"\n" +
                "                                                    }\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\"\n" +
                "                              }";

        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = null;
        try {
            eventData = objectMapper.readValue(data, Map.class);
        } catch (IOException e) {
            Assert.fail("Error in converting JSON payload to Map.");
        }

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                TestConstants.EventType.EDGE,
                TestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).build();

        //Action
        MobileCore.dispatchEvent(event, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Assert.fail("Error in dispatching Edge Personalization event.");
            }
        });

        //Assertion
        List<Event> optimizeNotificationEventsList = null;
        try {
            optimizeNotificationEventsList = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.NOTIFICATION);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Optimize Notification events.");
        }
        Assert.assertTrue(optimizeNotificationEventsList.isEmpty());
    }

    //14
    @Test
    public void testEdgeErrorResponse() {
        final String data = "{\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBBA\",\n" +
                "                                \"detail\": \"The following scope was not found: xcore:offer-activity:1111111111111111/xcore:offer-placement:1111111111111111\",\n" +
                "                                \"status\": 404,\n" +
                "                                \"requestEventId\": \"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\",\n" +
                "                                \"type\": \"https://ns.adobe.com/aep/errors/ODE-0001-404\",\n" +
                "                                \"title\": \"Not Found\"\n" +
                "                              }";

        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = null;
        try {
            eventData = objectMapper.readValue(data, Map.class);
        } catch (IOException e) {
            Assert.fail("Error in converting JSON payload to Map.");
        }

        Event event = new Event.Builder(
                "AEP Error Response",
                TestConstants.EventType.EDGE,
                TestConstants.EventSource.EDGE_ERROR_RESPONSE).
                setEventData(eventData).
                build();

        //Action
        MobileCore.dispatchEvent(event, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Assert.fail("Error in dispatching Edge Personalization event.");
            }
        });

        //Assertion
        List<Event> optimizeNotificationEventsList = null;
        try {
            optimizeNotificationEventsList = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.NOTIFICATION);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Optimize Notification events.");
        }
        Assert.assertTrue(optimizeNotificationEventsList.isEmpty());
    }

    //15
    @Test
    public void testGetPropositions_decisionScopeInCache() {
        //setup
        //Send Edge Response event so that propositions will get cached by the Optimize SDK
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);
        final String decisionScopeString = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";

        final String edgeResponseData = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                "                                        \"scope\": \""+decisionScopeString+"\",\n" +
                "                                        \"activity\": {\n" +
                "                                            \"etag\": \"8\",\n" +
                "                                            \"id\": \"xcore:offer-activity:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"placement\": {\n" +
                "                                            \"etag\": \"1\",\n" +
                "                                            \"id\": \"xcore:offer-placement:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                \"etag\": \"10\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/experience/offer-management/content-component-html\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                    \"format\": \"text/html\",\n" +
                "                                                    \"content\": \"<h1>This is HTML content</h1>\",\n" +
                "                                                    \"characteristics\": {\n" +
                "                                                        \"testing\": \"true\"\n" +
                "                                                    }\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = null;
        try {
            eventData = objectMapper.readValue(edgeResponseData, Map.class);
        } catch (IOException e) {
            Assert.fail("Error in converting JSON payload to Map.");
        }

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                TestConstants.EventType.EDGE,
                TestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).build();

        //Action
        MobileCore.dispatchEvent(event, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Assert.fail("Error in dispatching Edge Personalization event.");
            }
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        TestHelper.resetTestExpectations();
        DecisionScope decisionScope = new DecisionScope(decisionScopeString);
        final Map<DecisionScope, Proposition> propositionMap = new HashMap<>();
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        Optimize.getPropositions(Arrays.asList(decisionScope), new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting cached propositions");
            }

            @Override
            public void call(Map<DecisionScope, Proposition> decisionScopePropositionMap) {
                propositionMap.putAll(decisionScopePropositionMap);
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail("Timeout while getting cached propositions.");
        }
        //Assertions
        List<Event> optimizeResponseEventsList = null;
        try {
            optimizeResponseEventsList = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.RESPONSE_CONTENT);
        } catch (InterruptedException e) {
            Assert.fail("Error in getting Optimize events content list.");
        }

        Assert.assertNotNull(optimizeResponseEventsList);
        Assert.assertEquals(1, optimizeResponseEventsList.size());
        Assert.assertNull(optimizeResponseEventsList.get(0).getEventData().get("responseerror"));
        Assert.assertEquals(1, propositionMap.size());
        Proposition proposition = propositionMap.get(decisionScope);
        Assert.assertNotNull(proposition);
        Assert.assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", proposition.getId());
        Assert.assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", proposition.getScope());
        Assert.assertEquals(1, proposition.getOffers().size());

        Offer offer = proposition.getOffers().get(0);
        Assert.assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
        Assert.assertEquals("https://ns.adobe.com/experience/offer-management/content-component-html", offer.getSchema());
        Assert.assertEquals(OfferType.HTML, offer.getType());
        Assert.assertEquals("<h1>This is HTML content</h1>", offer.getContent());
        Assert.assertEquals(1, offer.getCharacteristics().size());
        Assert.assertEquals("true", offer.getCharacteristics().get("testing"));
    }

    //16
    @Test
    public void testGetPropositions_notAllDecisionScopesInCache() {
        //setup
        //Send Edge Response event so that propositions will get cached by the Optimize SDK
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);
        final String decisionScopeString = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";

        final String edgeResponseData = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                "                                        \"scope\": \""+decisionScopeString+"\",\n" +
                "                                        \"activity\": {\n" +
                "                                            \"etag\": \"8\",\n" +
                "                                            \"id\": \"xcore:offer-activity:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"placement\": {\n" +
                "                                            \"etag\": \"1\",\n" +
                "                                            \"id\": \"xcore:offer-placement:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                \"etag\": \"10\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/experience/offer-management/content-component-html\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                    \"format\": \"text/html\",\n" +
                "                                                    \"content\": \"<h1>This is HTML content</h1>\",\n" +
                "                                                    \"characteristics\": {\n" +
                "                                                        \"testing\": \"true\"\n" +
                "                                                    }\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = null;
        try {
            eventData = objectMapper.readValue(edgeResponseData, Map.class);
        } catch (IOException e) {
            Assert.fail("Error in converting JSON payload to Map.");
        }

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                TestConstants.EventType.EDGE,
                TestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).build();

        //Action
        MobileCore.dispatchEvent(event, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Assert.fail("Error in dispatching Edge Personalization event.");
            }
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        TestHelper.resetTestExpectations();
        DecisionScope decisionScope = new DecisionScope(decisionScopeString);
        DecisionScope decisionScope1 = new DecisionScope("myMbox");
        final Map<DecisionScope, Proposition> propositionMap = new HashMap<>();
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        Optimize.getPropositions(Arrays.asList(decisionScope, decisionScope1), new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting cached propositions");
            }

            @Override
            public void call(Map<DecisionScope, Proposition> decisionScopePropositionMap) {
                propositionMap.putAll(decisionScopePropositionMap);
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail("Timeout while getting cached propositions.");
        }
        //Assertions
        List<Event> optimizeResponseEventsList = null;
        try {
            optimizeResponseEventsList = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.RESPONSE_CONTENT);
        } catch (InterruptedException e) {
            Assert.fail("Error in getting Optimize events content list.");
        }

        Assert.assertNotNull(optimizeResponseEventsList);
        Assert.assertEquals(1, optimizeResponseEventsList.size());
        Assert.assertNull(optimizeResponseEventsList.get(0).getEventData().get("responseerror"));
        Assert.assertEquals(1, propositionMap.size());

        Assert.assertTrue(propositionMap.containsKey(decisionScope));
        Assert.assertFalse(propositionMap.containsKey(decisionScope1)); //Decision scope myMbox is not present in cache

        Proposition proposition = propositionMap.get(decisionScope);
        Assert.assertNotNull(proposition);
        Assert.assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", proposition.getId());
        Assert.assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", proposition.getScope());
        Assert.assertEquals(1, proposition.getOffers().size());

        Offer offer = proposition.getOffers().get(0);
        Assert.assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
        Assert.assertEquals("https://ns.adobe.com/experience/offer-management/content-component-html", offer.getSchema());
        Assert.assertEquals(OfferType.HTML, offer.getType());
        Assert.assertEquals("<h1>This is HTML content</h1>", offer.getContent());
        Assert.assertEquals(1, offer.getCharacteristics().size());
        Assert.assertEquals("true", offer.getCharacteristics().get("testing"));
    }

    //17
    @Test
    public void testGetPropositions_noDecisionScopeInCache() {
        //setup
        //Send Edge Response event so that propositions will get cached by the Optimize SDK
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);
        final String decisionScopeString = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";

        final String edgeResponseData = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                "                                        \"scope\": \""+decisionScopeString+"\",\n" +
                "                                        \"activity\": {\n" +
                "                                            \"etag\": \"8\",\n" +
                "                                            \"id\": \"xcore:offer-activity:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"placement\": {\n" +
                "                                            \"etag\": \"1\",\n" +
                "                                            \"id\": \"xcore:offer-placement:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                \"etag\": \"10\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/experience/offer-management/content-component-html\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                    \"format\": \"text/html\",\n" +
                "                                                    \"content\": \"<h1>This is HTML content</h1>\",\n" +
                "                                                    \"characteristics\": {\n" +
                "                                                        \"testing\": \"true\"\n" +
                "                                                    }\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = null;
        try {
            eventData = objectMapper.readValue(edgeResponseData, Map.class);
        } catch (IOException e) {
            Assert.fail("Error in converting JSON payload to Map.");
        }

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                TestConstants.EventType.EDGE,
                TestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).build();

        //Action
        MobileCore.dispatchEvent(event, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Assert.fail("Error in dispatching Edge Personalization event.");
            }
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        TestHelper.resetTestExpectations();
        DecisionScope decisionScope = new DecisionScope("myMbox1");
        DecisionScope decisionScope1 = new DecisionScope("myMbox2");
        final Map<DecisionScope, Proposition> propositionMap = new HashMap<>();
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        Optimize.getPropositions(Arrays.asList(decisionScope, decisionScope1), new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting cached propositions");
            }

            @Override
            public void call(Map<DecisionScope, Proposition> decisionScopePropositionMap) {
                propositionMap.putAll(decisionScopePropositionMap);
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail("Timeout while getting cached propositions.");
        }
        //Assertions
        List<Event> optimizeResponseEventsList = null;
        try {
            optimizeResponseEventsList = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.RESPONSE_CONTENT);
        } catch (InterruptedException e) {
            Assert.fail("Error in getting Optimize events content list.");
        }

        Assert.assertNotNull(optimizeResponseEventsList);
        Assert.assertEquals(1, optimizeResponseEventsList.size());
        Assert.assertNull(optimizeResponseEventsList.get(0).getEventData().get("responseerror"));
        Assert.assertEquals(0, propositionMap.size());

        Assert.assertFalse(propositionMap.containsKey(decisionScope)); //Decision scope myMbox1 is not present in cache
        Assert.assertFalse(propositionMap.containsKey(decisionScope1)); //Decision scope myMbox2 is not present in cache
    }

    //18
    @Test
    public void testGetPropositions_invalidDecisionScopesArray() {
        //setup
        //Send Edge Response event so that propositions will get cached by the Optimize SDK
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);
        final String decisionScopeString = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";

        final String edgeResponseData = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                "                                        \"scope\": \""+decisionScopeString+"\",\n" +
                "                                        \"activity\": {\n" +
                "                                            \"etag\": \"8\",\n" +
                "                                            \"id\": \"xcore:offer-activity:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"placement\": {\n" +
                "                                            \"etag\": \"1\",\n" +
                "                                            \"id\": \"xcore:offer-placement:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                \"etag\": \"10\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/experience/offer-management/content-component-html\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                    \"format\": \"text/html\",\n" +
                "                                                    \"content\": \"<h1>This is HTML content</h1>\",\n" +
                "                                                    \"characteristics\": {\n" +
                "                                                        \"testing\": \"true\"\n" +
                "                                                    }\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = null;
        try {
            eventData = objectMapper.readValue(edgeResponseData, Map.class);
        } catch (IOException e) {
            Assert.fail("Error in converting JSON payload to Map.");
        }

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                TestConstants.EventType.EDGE,
                TestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).build();

        //Action
        MobileCore.dispatchEvent(event, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Assert.fail("Error in dispatching Edge Personalization event.");
            }
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        TestHelper.resetTestExpectations();

        final String getPropositionData = "{\n" +
                "                                \"requesttype\": \"getpropositions\",\n" +
                "                                \"decisionscopes\": [\n" +
                "                                    {\n" +
                "                                        \"name1\": \""+decisionScopeString+"\"\n" +
                "                                    },\n" +
                "                                    {\n" +
                "                                        \"name2\": \"myMbox\"\n" +
                "                                    }\n" +
                "                                ]\n" +
                "                              }";

        Map<String, Object> getPropositionEventData = null;
        try {
            getPropositionEventData = objectMapper.readValue(getPropositionData, Map.class);
        } catch (IOException e) {
            Assert.fail("Error in converting getProposition event data from json to map.");
        }

        Event getPropositionEvent = new Event.Builder(
                "Optimize Get Propositions Request",
                TestConstants.EventType.OPTIMIZE
                , TestConstants.EventSource.REQUEST_CONTENT).
                setEventData(getPropositionEventData).build();

        TestHelper.resetTestExpectations();
        MobileCore.dispatchEvent(getPropositionEvent, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Assert.fail("Error in dispatching Optimize Request event");
            }
        });

        //Assertions
        List<Event> optimizeResponseEventsList = null;
        try {
            optimizeResponseEventsList = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.RESPONSE_CONTENT, 1000);
        } catch (InterruptedException e) {
            Assert.fail("Error in getting Optimize events content list.");
        }

        Assert.assertNotNull(optimizeResponseEventsList);
        Assert.assertEquals(1, optimizeResponseEventsList.size());
        Assert.assertNotNull(optimizeResponseEventsList.get(0).getEventData().get("responseerror"));
        Assert.assertEquals(AdobeError.UNEXPECTED_ERROR, optimizeResponseEventsList.get(0).getEventData().get("responseerror"));
    }

    //19
    @Test
    public void testGetPropositions_emptyCache() {
        //setup
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);
        final DecisionScope decisionScope1 = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
        final DecisionScope decisionScope2 = new DecisionScope("myMbox");

        //Action
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        final Map<DecisionScope, Proposition> propositionMap = new HashMap<>();
        TestHelper.resetTestExpectations();
        Optimize.getPropositions(Arrays.asList(decisionScope1, decisionScope2), new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting Propositions.");
            }

            @Override
            public void call(Map<DecisionScope, Proposition> decisionScopePropositionMap) {
                propositionMap.putAll(decisionScopePropositionMap);
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail("Timeout while getting cached propositions.");
        }
        //Assertions
        List<Event> optimizeResponseEventsList = null;
        try {
            optimizeResponseEventsList = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.RESPONSE_CONTENT);
        } catch (InterruptedException e) {
            Assert.fail("Error in getting Optimize events content list.");
        }

        Assert.assertNotNull(optimizeResponseEventsList);
        Assert.assertEquals(1, optimizeResponseEventsList.size());
        Assert.assertNull(optimizeResponseEventsList.get(0).getEventData().get("responseerror"));
        Assert.assertEquals(0, propositionMap.size());

        Assert.assertFalse(propositionMap.containsKey(decisionScope1));
        Assert.assertFalse(propositionMap.containsKey(decisionScope2));
    }

    //20
    @Test
    public void testGetPropositions_missingEventRequestTypeInData() {
        //setup
        //Send Edge Response event so that propositions will get cached by the Optimize SDK
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);
        final String decisionScopeString = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";

        final String edgeResponseData = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                "                                        \"scope\": \""+decisionScopeString+"\",\n" +
                "                                        \"activity\": {\n" +
                "                                            \"etag\": \"8\",\n" +
                "                                            \"id\": \"xcore:offer-activity:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"placement\": {\n" +
                "                                            \"etag\": \"1\",\n" +
                "                                            \"id\": \"xcore:offer-placement:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                \"etag\": \"10\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/experience/offer-management/content-component-html\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                    \"format\": \"text/html\",\n" +
                "                                                    \"content\": \"<h1>This is HTML content</h1>\",\n" +
                "                                                    \"characteristics\": {\n" +
                "                                                        \"testing\": \"true\"\n" +
                "                                                    }\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = null;
        try {
            eventData = objectMapper.readValue(edgeResponseData, Map.class);
        } catch (IOException e) {
            Assert.fail("Error in converting JSON payload to Map.");
        }

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                TestConstants.EventType.EDGE,
                TestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).
                build();

        //Action
        MobileCore.dispatchEvent(event, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Assert.fail("Error in dispatching Edge Personalization event.");
            }
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        final String getPropositionData = "{\n" +
                "                                \"decisionscopes\": [\n" +
                "                                    {\n" +
                "                                        \"name\": \""+decisionScopeString+"\"\n" +
                "                                    }" +
                "                                ]\n" +
                "                              }";

        Map<String, Object> getPropositionEventData = null;
        try {
            getPropositionEventData = objectMapper.readValue(getPropositionData, Map.class);
        } catch (IOException e) {
            Assert.fail("Error in converting getProposition event data from json to map.");
        }

        Event getPropositionEvent = new Event.Builder(
                "Optimize Get Propositions Request",
                TestConstants.EventType.OPTIMIZE,
                TestConstants.EventSource.REQUEST_CONTENT).
                setEventData(getPropositionEventData)
                .build();

        TestHelper.resetTestExpectations();
        MobileCore.dispatchEvent(getPropositionEvent, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Assert.fail("Error in dispatching Optimize Request event");
            }
        });

        //Assertions
        List<Event> optimizeResponseEventsList = null;
        try {
            optimizeResponseEventsList = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.RESPONSE_CONTENT, 1000);
        } catch (InterruptedException e) {
            Assert.fail("Error in getting Optimize response events list.");
        }

        Assert.assertTrue(optimizeResponseEventsList.isEmpty());
    }

    //21
    @Test
    public void testTrackPropositions_validPropositionInteractionsForDisplay() {

    }
}