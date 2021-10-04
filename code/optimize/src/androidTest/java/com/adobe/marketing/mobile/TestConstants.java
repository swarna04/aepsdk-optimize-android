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

public class TestConstants {

    public final static class EventType {

        public static final String MONITOR = "com.adobe.functional.eventType.monitor";
        public static final String OPTIMIZE = "com.adobe.eventtype.optimize";
        public static final String EDGE = "com.adobe.eventType.edge";
    }

    public final static class EventSource {

        public static final String UNREGISTER = "com.adobe.eventSource.unregister";
        public static final String SHARED_STATE_REQUEST = "com.adobe.eventSource.sharedStateRequest";
        public static final String XDM_SHARED_STATE_REQUEST = "com.adobe.eventSource.xdmsharedStateRequest";
        public static final String XDM_SHARED_STATE_RESPONSE = "com.adobe.eventSource.xdmsharedStateResponse";
        public static final String SHARED_STATE_RESPONSE = "com.adobe.eventSource.xdmsharedStateResponse";
        public static final String REQUEST_CONTENT = "com.adobe.eventsource.requestcontent";
    }

    public final static class EventDataKey {

        public static final String STATE_OWNER = "";
    }
}
