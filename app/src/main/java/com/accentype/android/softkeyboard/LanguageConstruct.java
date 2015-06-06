package com.accentype.android.softkeyboard;

import java.util.HashMap;

/**
 * Created by lhoang on 3/31/2015.
 */
public class LanguageConstruct {
    private static LanguageConstruct instance = null;
    public HashMap<Integer, Character> AsciiToAccentMap = null;

    protected LanguageConstruct() {
        AsciiToAccentMap = new HashMap<>();
        AsciiToAccentMap.put(33 , 'a');
        AsciiToAccentMap.put(34 , 'd');
        AsciiToAccentMap.put(35 , 'e');
        AsciiToAccentMap.put(36 , 'i');
        AsciiToAccentMap.put(37 , 'o');
        AsciiToAccentMap.put(38 , 'u');
        AsciiToAccentMap.put(39 , 'y');
        AsciiToAccentMap.put(40 , 'à');
        AsciiToAccentMap.put(41 , 'á');
        AsciiToAccentMap.put(42 , 'â');
        AsciiToAccentMap.put(43 , 'ã');
        AsciiToAccentMap.put(44 , 'è');
        AsciiToAccentMap.put(45 , 'é');
        AsciiToAccentMap.put(46 , 'ê');
        AsciiToAccentMap.put(47 , 'ì');
        AsciiToAccentMap.put(48 , 'í');
        AsciiToAccentMap.put(49 , 'ò');
        AsciiToAccentMap.put(50 , 'ó');
        AsciiToAccentMap.put(51 , 'ô');
        AsciiToAccentMap.put(52 , 'õ');
        AsciiToAccentMap.put(53 , 'ù');
        AsciiToAccentMap.put(54 , 'ú');
        AsciiToAccentMap.put(55 , 'ý');
        AsciiToAccentMap.put(56 , 'ă');
        AsciiToAccentMap.put(57 , 'đ');
        AsciiToAccentMap.put(58 , 'ĩ');
        AsciiToAccentMap.put(59 , 'ũ');
        AsciiToAccentMap.put(60 , 'ơ');
        AsciiToAccentMap.put(61 , 'ư');
        AsciiToAccentMap.put(62 , 'ạ');
        AsciiToAccentMap.put(63 , 'ả');
        AsciiToAccentMap.put(64 , 'ấ');
        AsciiToAccentMap.put(65 , 'ầ');
        AsciiToAccentMap.put(66 , 'ẩ');
        AsciiToAccentMap.put(67 , 'ẫ');
        AsciiToAccentMap.put(68 , 'ậ');
        AsciiToAccentMap.put(69 , 'ắ');
        AsciiToAccentMap.put(70 , 'ằ');
        AsciiToAccentMap.put(71 , 'ẳ');
        AsciiToAccentMap.put(72 , 'ẵ');
        AsciiToAccentMap.put(73 , 'ặ');
        AsciiToAccentMap.put(74 , 'ẹ');
        AsciiToAccentMap.put(75 , 'ẻ');
        AsciiToAccentMap.put(76 , 'ẽ');
        AsciiToAccentMap.put(77 , 'ế');
        AsciiToAccentMap.put(78 , 'ề');
        AsciiToAccentMap.put(79 , 'ể');
        AsciiToAccentMap.put(80 , 'ễ');
        AsciiToAccentMap.put(81 , 'ệ');
        AsciiToAccentMap.put(82 , 'ỉ');
        AsciiToAccentMap.put(83 , 'ị');
        AsciiToAccentMap.put(84 , 'ọ');
        AsciiToAccentMap.put(85 , 'ỏ');
        AsciiToAccentMap.put(86 , 'ố');
        AsciiToAccentMap.put(87 , 'ồ');
        AsciiToAccentMap.put(88 , 'ổ');
        AsciiToAccentMap.put(89 , 'ỗ');
        AsciiToAccentMap.put(90 , 'ộ');
        AsciiToAccentMap.put(91 , 'ớ');
        AsciiToAccentMap.put(92 , 'ờ');
        AsciiToAccentMap.put(93 , 'ở');
        AsciiToAccentMap.put(94 , 'ỡ');
        AsciiToAccentMap.put(95 , 'ợ');
        AsciiToAccentMap.put(96 , 'ụ');
        AsciiToAccentMap.put(97 , 'ủ');
        AsciiToAccentMap.put(98 , 'ứ');
        AsciiToAccentMap.put(99 , 'ừ');
        AsciiToAccentMap.put(100, 'ử');
        AsciiToAccentMap.put(101, 'ữ');
        AsciiToAccentMap.put(102, 'ự');
        AsciiToAccentMap.put(103, 'ỳ');
        AsciiToAccentMap.put(104, 'ỵ');
        AsciiToAccentMap.put(105, 'ỷ');
        AsciiToAccentMap.put(106, 'ỹ');
    }
    public static LanguageConstruct getInstance() {
        if(instance == null) {
            instance = new LanguageConstruct();
        }
        return instance;
    }
}
