package com.adobe.marketing.mobile;

import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.NetworkCallback;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MockNetworkService implements Networking {

    List<NetworkRequest> networkRequests = new ArrayList<>();
    private HttpConnecting httpConnecting;

    public MockNetworkService(HttpConnecting httpConnecting) {
        this.httpConnecting = httpConnecting;
    }

    @Override
    public void connectAsync(NetworkRequest networkRequest, NetworkCallback networkCallback) {
        networkRequests.add(networkRequest);
        networkCallback.call(httpConnecting);
    }

    public void clearRequestsCache() {
        networkRequests.clear();
    }

    public static class MockHttpConnecting implements HttpConnecting{
        int responseCode;
        String response;

        public MockHttpConnecting(int responseCode, String response) {
            this.responseCode = responseCode;
            this.response = response;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public InputStream getErrorStream() {
            return null;
        }

        @Override
        public int getResponseCode() {
            return responseCode;
        }

        @Override
        public String getResponseMessage() {
            return null;
        }

        @Override
        public String getResponsePropertyValue(String s) {
            return null;
        }

        @Override
        public void close() {

        }
    }
}
