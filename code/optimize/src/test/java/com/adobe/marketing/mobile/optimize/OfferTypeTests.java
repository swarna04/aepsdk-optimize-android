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

import org.junit.Assert;
import org.junit.Test;

public class OfferTypeTests {
    @Test
    public void testOfferType_fromJson() {
        // test
        final OfferType offerType = OfferType.from("application/json");
        Assert.assertEquals(OfferType.JSON, offerType);
    }

    @Test
    public void testOfferType_fromText() {
        // test
        final OfferType offerType = OfferType.from("text/plain");
        Assert.assertEquals(OfferType.TEXT, offerType);
    }

    @Test
    public void testOfferType_fromHtml() {
        // test
        final OfferType offerType = OfferType.from("text/html");
        Assert.assertEquals(OfferType.HTML, offerType);
    }

    @Test
    public void testOfferType_fromImage() {
        // test
        final OfferType offerType = OfferType.from("image/png");
        Assert.assertEquals(OfferType.IMAGE, offerType);
    }

    @Test
    public void testOfferType_fromUnknownString() {
        // test
        final OfferType offerType = OfferType.from("*/*");
        Assert.assertEquals(OfferType.UNKNOWN, offerType);
    }

    @Test
    public void testOfferType_fromEmptyString() {
        // test
        final OfferType offerType = OfferType.from("");
        Assert.assertEquals(OfferType.UNKNOWN, offerType);
    }

    @Test
    public void testOfferType_fromNullString() {
        // test
        final OfferType offerType = OfferType.from(null);
        Assert.assertEquals(OfferType.UNKNOWN, offerType);
    }

    @Test
    public void testOfferType_unknownFromValues() {
        // test
        Assert.assertEquals(OfferType.UNKNOWN, OfferType.values()[0]);
    }

    @Test
    public void testOfferType_jsonFromValues() {
        // test
        Assert.assertEquals(OfferType.JSON, OfferType.values()[1]);
    }

    @Test
    public void testOfferType_textFromValues() {
        // test
        Assert.assertEquals(OfferType.TEXT, OfferType.values()[2]);
    }

    @Test
    public void testOfferType_htmlFromValues() {
        // test
        Assert.assertEquals(OfferType.HTML, OfferType.values()[3]);
    }

    @Test
    public void testOfferType_imageFromValues() {
        // test
        Assert.assertEquals(OfferType.IMAGE, OfferType.values()[4]);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testOfferType_invalidFromValues() {
        // test
        Assert.assertEquals(OfferType.UNKNOWN, OfferType.values()[100]);
    }

    @Test
    public void testToString_jsonOfferType() {
        // test
        Assert.assertEquals("application/json", OfferType.JSON.toString());
    }

    @Test
    public void testToString_textOfferType() {
        // test
        Assert.assertEquals("text/plain", OfferType.TEXT.toString());
    }

    @Test
    public void testToString_htmlOfferType() {
        // test
        Assert.assertEquals("text/html", OfferType.HTML.toString());
    }

    @Test
    public void testToString_imageOfferType() {
        // test
        Assert.assertEquals("image/*", OfferType.IMAGE.toString());
    }

    @Test
    public void testToString_unknownOfferType() {
        // test
        Assert.assertEquals("", OfferType.UNKNOWN.toString());
    }
}
