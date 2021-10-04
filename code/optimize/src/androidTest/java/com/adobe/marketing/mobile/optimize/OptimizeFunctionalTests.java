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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.MockNetworkService;
import com.adobe.marketing.mobile.MockNetworkService.MockHttpConnecting;
import com.adobe.marketing.mobile.TestConstants;
import com.adobe.marketing.mobile.TestHelper;
import com.adobe.marketing.mobile.services.ServiceProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

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
        configData.put("edge.configId","ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);
        ServiceProvider.getInstance().setNetworkService(new MockNetworkService(new MockHttpConnecting(200, "{}")));

        //Action
        Optimize.updatePropositions(Arrays.asList(new DecisionScope(decisionScopeName)), null, null);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Assert
        List<Event> eventsListOptimize = null;
        List<Event> eventsListEdge = null;
        try {
            eventsListOptimize = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.REQUEST_CONTENT);
            eventsListEdge = TestHelper.getDispatchedEventsWith(TestConstants.EventType.EDGE, TestConstants.EventSource.REQUEST_CONTENT);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Optimize Request Content Events.");
        }

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertNotNull(eventsListEdge);
        Assert.assertEquals(1, eventsListEdge.size());
        Event event =  eventsListOptimize.get(0);
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
        configData.put("edge.configId","ffffffff-ffff-ffff-ffff-ffffffffffff");
        configData.put("optimize.datasetId","111111111111111111111111");
        MobileCore.updateConfiguration(configData);
        ServiceProvider.getInstance().setNetworkService(new MockNetworkService(new MockHttpConnecting(200, "{}")));

        Optimize.updatePropositions(Arrays.asList(new DecisionScope(decisionScopeName)), xdmMap, dataMap);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Assert
        List<Event> eventsListOptimize = null;
        List<Event> eventsListEdge = null;
        try {
            eventsListOptimize = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.REQUEST_CONTENT);
            eventsListEdge = TestHelper.getDispatchedEventsWith(TestConstants.EventType.EDGE, TestConstants.EventSource.REQUEST_CONTENT);
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
        Assert.assertEquals("MyXDMValue", ((Map<String, String>)eventData.get("xdm")).get("MyXDMKey"));
        Assert.assertEquals("MyDataValue", ((Map<String, String>)eventData.get("data")).get("MyDataKey"));
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
        configData.put("edge.configId","ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);
        ServiceProvider.getInstance().setNetworkService(new MockNetworkService(new MockHttpConnecting(200, "{}")));

        //Action
        Optimize.updatePropositions(
                Arrays.asList(new DecisionScope(decisionScopeName1), new DecisionScope(decisionScopeName2))
                , null, null);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Assert
        List<Event> eventsListOptimize = null;
        List<Event> eventsListEdge = null;
        try {
            eventsListOptimize = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.REQUEST_CONTENT);
            eventsListEdge = TestHelper.getDispatchedEventsWith(TestConstants.EventType.EDGE, TestConstants.EventSource.REQUEST_CONTENT);
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
    public void testUpdatePropositions_missingEventRequestTypeInData(){
        //setup
        List<DecisionScope> decisionScopesList = Arrays.asList(new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ=="));
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId","ffffffff-ffff-ffff-ffff-ffffffffffff");
        MobileCore.updateConfiguration(configData);
        final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                Assert.fail("Error in dispatching Optimize Request content event");
            }
        };

        final List<Map<String, Object>> flattenedDecisionScopes = new ArrayList<Map<String, Object>>();
        for (final DecisionScope scope: decisionScopesList) {
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
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        //Assert
        List<Event> eventsListOptimize = null;
        List<Event> eventsListEdge = null;
        try {
            eventsListOptimize = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.REQUEST_CONTENT);
            eventsListEdge = TestHelper.getDispatchedEventsWith(TestConstants.EventType.EDGE, TestConstants.EventSource.REQUEST_CONTENT);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Optimize Request Content Events.");
        }

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertTrue(eventsListEdge.isEmpty());
    }

    //6
    @Test
    public void testUpdatePropositions_ConfigNotAvailable(){
        //Setup
        final String decisionScopeName = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";
        ServiceProvider.getInstance().setNetworkService(new MockNetworkService(new MockHttpConnecting(200, "{}")));
        MobileCore.updateConfiguration(Collections.<String, Object>emptyMap());

        //Action
        Optimize.updatePropositions(Arrays.asList(new DecisionScope(decisionScopeName)), null, null);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Assert
        List<Event> eventsListOptimize = null;
        List<Event> eventsListEdge = null;
        try {
            eventsListOptimize = TestHelper.getDispatchedEventsWith(TestConstants.EventType.OPTIMIZE, TestConstants.EventSource.REQUEST_CONTENT);
            eventsListEdge = TestHelper.getDispatchedEventsWith(TestConstants.EventType.EDGE, TestConstants.EventSource.REQUEST_CONTENT);
        } catch (InterruptedException e) {
            Assert.fail("Exception in getting Optimize Request Content Events.");
        }

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertNotNull(eventsListEdge);
        Assert.assertTrue(eventsListEdge.isEmpty());
    }
}