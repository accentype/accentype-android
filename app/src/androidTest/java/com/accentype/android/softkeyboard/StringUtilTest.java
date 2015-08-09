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

    public void testNormalizeByWords() throws Exception {
        String case1 = StringUtil.normalizeByWords("thế sao nhỉ", "thè sáo");
        Assert.assertEquals("thè sáo nhỉ", case1);

        String case2 = StringUtil.normalizeByWords("thế sao là sao", "thẹ sáo là sao hả");
        Assert.assertEquals("thẹ sáo là sao", case2);

        String case3 = StringUtil.normalizeByWords("thé là gì", "the là g");
        Assert.assertEquals("the là gì", case3);

        String case4 = StringUtil.normalizeByWords("tại sao lại thế", "thế làm gì");
        Assert.assertEquals("tại sao lại thế", case4);

        String case5 = StringUtil.normalizeByWords("cái gì thế này", "cải gg");
        Assert.assertEquals("cải gì thế này", case5);

        String case6 = StringUtil.normalizeByWords("cái gì thế này", "cái giếng");
        Assert.assertEquals("cái gì thế này", case6);

        String case7 = StringUtil.normalizeByWords("cái gì ", "cái gỉ");
        Assert.assertEquals("cái gỉ ", case7);
    }
}