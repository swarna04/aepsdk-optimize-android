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
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

class OptimizeUtils {

    private static final String SELF_TAG = "OptimizeUtils";

    /**
     * Checks if the given {@code collection} is null or empty.
     *
     * @param collection input {@code Collection<?>} to be tested.
     * @return {@code boolean} result indicating whether the provided {@code collection} is null or empty.
     */
    static boolean isNullOrEmpty(final Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Checks if the given {@code map} is null or empty.
     *
     * @param map input {@code Map<?, ?>} to be tested.
     * @return {@code boolean} result indicating whether the provided {@code map} is null or empty.
     */
    static boolean isNullOrEmpty(final Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Checks if the given {@code String} is null or empty.
     *
     * @param str input {@link String} to be tested.
     * @return {@code boolean} result indicating whether the provided {@code str} is null or empty.
     */
    static boolean isNullOrEmpty(final String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Base64 encode the given {@code String}.
     * <p>
     * This method returns the provided {@code str} value if it is null or empty.
     *
     * @param str input {@link String} to be encoded.
     * @return {@code String} containing the Base64 encoded value.
     */
    static String base64Encode(final String str) {
        if (isNullOrEmpty(str)) {
            return str;
        }
        return Base64.encodeToString(str.getBytes(), Base64.NO_WRAP);
    }

    /**
     * Base64 decode the given {@code String}.
     * <p>
     * This method returns the provided {@code str} value if it is null or empty. It returns null if the base64 decode fails.
     *
     * @param str input {@link String} to be decoded.
     * @return {@code String} containing the Base64 decoded value.
     */
    static String base64Decode(final String str) {
        if (isNullOrEmpty(str)) {
            return str;
        }

        String output = null;
        try {
            output = new String(Base64.decode(str, Base64.DEFAULT));
        } catch(final IllegalArgumentException ex) {
            Log.debug(OptimizeConstants.LOG_TAG, SELF_TAG, "Base64 decode failed for the given string (%s) with exception: %s", str, ex.getLocalizedMessage());
        }
        return output;
    }

    /**
     * Determines the {@code AdobeError} provided the error code.
     *
     * @return {@link AdobeError} corresponding to the given error code, or {@link AdobeError#UNEXPECTED_ERROR} otherwise.
     */
    @SuppressWarnings("magicnumber")
    static AdobeError convertToAdobeError(final int errorCode) {
        final AdobeError error;
        switch (errorCode) {
            case 0:
                error = AdobeError.UNEXPECTED_ERROR;
                break;
            case 1:
                error = AdobeError.CALLBACK_TIMEOUT;
                break;
            case 2:
                error = AdobeError.CALLBACK_NULL;
                break;
            case 11:
                error = AdobeError.EXTENSION_NOT_INITIALIZED;
                break;
            default:
                error = AdobeError.UNEXPECTED_ERROR;
        }
        return error;
    }

    /**
     * Returns the mobile app surface
     *
     * @return {@link String} containing the mobile app surface
     */
    static String getMobileAppSurface() {
        final String appSurface = ServiceProvider.getInstance().getDeviceInfoService().getApplicationPackageName();
        if (OptimizeUtils.isNullOrEmpty(appSurface)) {
            return null;
        }
        return OptimizeConstants.SURFACE_BASE + appSurface;
    }

    /**
     * Return surface prefixed with mobile app surface
     *
     * @param surface {@link String} the surface name
     * @return {@link String} surface prefixed with mobile app surface if valid, or null otherwise
     */
    static String getPrefixedSurface(final String surface) {
        if (OptimizeUtils.isNullOrEmpty(surface)) {
            return null;
        }

        final String mobileAppSurface = getMobileAppSurface();
        if (OptimizeUtils.isNullOrEmpty(mobileAppSurface)) {
            return null;
        }

        return mobileAppSurface + "/" + surface;
    }

    /**
     * Check if given event is a Personalization Decision Response Event
     * @param event instance of {@link Event}
     * @return true if event is a Personalization Decision Response event, false otherwise
     */
    static boolean isPersonalizationDecisionResponse(final Event event) {
        return OptimizeConstants.EventType.EDGE.equalsIgnoreCase(event.getType()) &&
                OptimizeConstants.EventSource.EDGE_PERSONALIZATION_DECISIONS.equalsIgnoreCase(event.getSource());
    }

    /**
     * Checks if provided string is a valid URI
     * @param uriString {@link String} input string
     * @return true if inputted string is a valid URI, false otherwise
     */
    static boolean isValidUri(final String uriString) {
        if (StringUtils.isNullOrEmpty(uriString)) {
            return false;
        }
        try {
            new URI(uriString);
            return true;
        } catch (final URISyntaxException ex) {
            Log.debug(OptimizeConstants.LOG_TAG, SELF_TAG, "Given string %s is not a valid URI, exception: %s", uriString, ex.getLocalizedMessage());
            return false;
        }
    }

    /**
     * Retrieves surface path from the provided scope string.
     *
     * @param scope {@link String} containing the surface URI to extract surface path from.
     * @return surface path extracted from input string
     */
    static String retrieveSurfacePathFromScope(final String scope) {
        if (OptimizeUtils.isNullOrEmpty(scope)) {
            return scope;
        }
        final String mobileAppSurface = OptimizeUtils.getMobileAppSurface();
        if (!OptimizeUtils.isNullOrEmpty(mobileAppSurface) && scope.startsWith(mobileAppSurface)) {
            return scope.substring(mobileAppSurface.length()+1);
        }
        return scope;
    }
}
