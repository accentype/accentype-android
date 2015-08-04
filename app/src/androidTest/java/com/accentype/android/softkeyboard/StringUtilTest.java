package com.accentype.android.softkeyboard;

import junit.framework.Assert;
import junit.framework.TestCase;

public class StringUtilTest extends TestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
    }

    public void testReplaceDottedPreserveCase() throws Exception {
        String case1 = StringUtil.replaceDottedPreserveCase("Hello World", new StringBuilder(".....werl"));
        Assert.assertEquals("Hello Werld", case1);

        String case2 = StringUtil.replaceDottedPreserveCase("Làm   sao lai thế", new StringBuilder("......lạithể"));
        Assert.assertEquals("Làm   sao lại thể", case2);

        String case3 = StringUtil.replaceDottedPreserveCase("   sao lai thế   ", new StringBuilder("...lạithể"));
        Assert.assertEquals("   sao lại thể   ", case3);
    }

    public void testNormalizeWordCasePreserve() throws Exception {
        String case1 = StringUtil.normalizeWordCasePreserve("WxYz", "abcd");
        Assert.assertEquals("AbCd", case1);

        String case2 = StringUtil.normalizeWordCasePreserve("AAAA", "abc");
        Assert.assertEquals("ABC", case2);

        String case3 = StringUtil.normalizeWordCasePreserve("1234", "abcdef");
        Assert.assertEquals("abcdef", case3);
    }
}