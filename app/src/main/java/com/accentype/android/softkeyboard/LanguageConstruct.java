package com.accentype.android.softkeyboard;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lhoang on 3/31/2015.
 */
public class LanguageConstruct {
//    public static final Map<Integer, Character> AsciiToAccentMap;
    public static final Map<Character, Character> AccentToRawMap;
    static
    {
        // Mapping from accented to unaccented letters
        AccentToRawMap = new HashMap<>();
        AccentToRawMap.put('a', 'a');
        AccentToRawMap.put('á', 'a');
        AccentToRawMap.put('à', 'a');
        AccentToRawMap.put('ạ', 'a');
        AccentToRawMap.put('ả', 'a');
        AccentToRawMap.put('ã', 'a');

        AccentToRawMap.put('ă', 'a');
        AccentToRawMap.put('ắ', 'a');
        AccentToRawMap.put('ằ', 'a');
        AccentToRawMap.put('ặ', 'a');
        AccentToRawMap.put('ẳ', 'a');
        AccentToRawMap.put('ẵ', 'a');

        AccentToRawMap.put('â', 'a');
        AccentToRawMap.put('ấ', 'a');
        AccentToRawMap.put('ầ', 'a');
        AccentToRawMap.put('ậ', 'a');
        AccentToRawMap.put('ẩ', 'a');
        AccentToRawMap.put('ẫ', 'a');

        AccentToRawMap.put('e', 'e');
        AccentToRawMap.put('é', 'e');
        AccentToRawMap.put('è', 'e');
        AccentToRawMap.put('ẹ', 'e');
        AccentToRawMap.put('ẻ', 'e');
        AccentToRawMap.put('ẽ', 'e');

        AccentToRawMap.put('ê', 'e');
        AccentToRawMap.put('ế', 'e');
        AccentToRawMap.put('ề', 'e');
        AccentToRawMap.put('ệ', 'e');
        AccentToRawMap.put('ể', 'e');
        AccentToRawMap.put('ễ', 'e');

        AccentToRawMap.put('i', 'i');
        AccentToRawMap.put('í', 'i');
        AccentToRawMap.put('ì', 'i');
        AccentToRawMap.put('ị', 'i');
        AccentToRawMap.put('ỉ', 'i');
        AccentToRawMap.put('ĩ', 'i');

        AccentToRawMap.put('o', 'o');
        AccentToRawMap.put('ó', 'o');
        AccentToRawMap.put('ò', 'o');
        AccentToRawMap.put('ọ', 'o');
        AccentToRawMap.put('ỏ', 'o');
        AccentToRawMap.put('õ', 'o');

        AccentToRawMap.put('ô', 'o');
        AccentToRawMap.put('ố', 'o');
        AccentToRawMap.put('ồ', 'o');
        AccentToRawMap.put('ộ', 'o');
        AccentToRawMap.put('ổ', 'o');
        AccentToRawMap.put('ỗ', 'o');

        AccentToRawMap.put('ơ', 'o');
        AccentToRawMap.put('ớ', 'o');
        AccentToRawMap.put('ờ', 'o');
        AccentToRawMap.put('ợ', 'o');
        AccentToRawMap.put('ở', 'o');
        AccentToRawMap.put('ỡ', 'o');

        AccentToRawMap.put('u', 'u');
        AccentToRawMap.put('ú', 'u');
        AccentToRawMap.put('ù', 'u');
        AccentToRawMap.put('ụ', 'u');
        AccentToRawMap.put('ủ', 'u');
        AccentToRawMap.put('ũ', 'u');

        AccentToRawMap.put('ư', 'u');
        AccentToRawMap.put('ứ', 'u');
        AccentToRawMap.put('ừ', 'u');
        AccentToRawMap.put('ự', 'u');
        AccentToRawMap.put('ử', 'u');
        AccentToRawMap.put('ữ', 'u');

        AccentToRawMap.put('y', 'y');
        AccentToRawMap.put('ý', 'y');
        AccentToRawMap.put('ỳ', 'y');
        AccentToRawMap.put('ỵ', 'y');
        AccentToRawMap.put('ỷ', 'y');
        AccentToRawMap.put('ỹ', 'y');

        AccentToRawMap.put('d', 'd');
        AccentToRawMap.put('đ', 'd');

        // Mapping from ASCII codes to accented characters.
//        AsciiToAccentMap = new HashMap<>();
//        AsciiToAccentMap.put(33 , 'a');
//        AsciiToAccentMap.put(34 , 'd');
//        AsciiToAccentMap.put(35 , 'e');
//        AsciiToAccentMap.put(36 , 'i');
//        AsciiToAccentMap.put(37 , 'o');
//        AsciiToAccentMap.put(38 , 'u');
//        AsciiToAccentMap.put(39 , 'y');
//        AsciiToAccentMap.put(40 , 'à');
//        AsciiToAccentMap.put(41 , 'á');
//        AsciiToAccentMap.put(42 , 'â');
//        AsciiToAccentMap.put(43 , 'ã');
//        AsciiToAccentMap.put(44 , 'è');
//        AsciiToAccentMap.put(45 , 'é');
//        AsciiToAccentMap.put(46 , 'ê');
//        AsciiToAccentMap.put(47 , 'ì');
//        AsciiToAccentMap.put(48 , 'í');
//        AsciiToAccentMap.put(49 , 'ò');
//        AsciiToAccentMap.put(50 , 'ó');
//        AsciiToAccentMap.put(51 , 'ô');
//        AsciiToAccentMap.put(52 , 'õ');
//        AsciiToAccentMap.put(53 , 'ù');
//        AsciiToAccentMap.put(54 , 'ú');
//        AsciiToAccentMap.put(55 , 'ý');
//        AsciiToAccentMap.put(56 , 'ă');
//        AsciiToAccentMap.put(57 , 'đ');
//        AsciiToAccentMap.put(58 , 'ĩ');
//        AsciiToAccentMap.put(59 , 'ũ');
//        AsciiToAccentMap.put(60 , 'ơ');
//        AsciiToAccentMap.put(61 , 'ư');
//        AsciiToAccentMap.put(62 , 'ạ');
//        AsciiToAccentMap.put(63 , 'ả');
//        AsciiToAccentMap.put(64 , 'ấ');
//        AsciiToAccentMap.put(65 , 'ầ');
//        AsciiToAccentMap.put(66 , 'ẩ');
//        AsciiToAccentMap.put(67 , 'ẫ');
//        AsciiToAccentMap.put(68 , 'ậ');
//        AsciiToAccentMap.put(69 , 'ắ');
//        AsciiToAccentMap.put(70 , 'ằ');
//        AsciiToAccentMap.put(71 , 'ẳ');
//        AsciiToAccentMap.put(72 , 'ẵ');
//        AsciiToAccentMap.put(73 , 'ặ');
//        AsciiToAccentMap.put(74 , 'ẹ');
//        AsciiToAccentMap.put(75 , 'ẻ');
//        AsciiToAccentMap.put(76 , 'ẽ');
//        AsciiToAccentMap.put(77 , 'ế');
//        AsciiToAccentMap.put(78 , 'ề');
//        AsciiToAccentMap.put(79 , 'ể');
//        AsciiToAccentMap.put(80 , 'ễ');
//        AsciiToAccentMap.put(81 , 'ệ');
//        AsciiToAccentMap.put(82 , 'ỉ');
//        AsciiToAccentMap.put(83 , 'ị');
//        AsciiToAccentMap.put(84 , 'ọ');
//        AsciiToAccentMap.put(85 , 'ỏ');
//        AsciiToAccentMap.put(86 , 'ố');
//        AsciiToAccentMap.put(87 , 'ồ');
//        AsciiToAccentMap.put(88 , 'ổ');
//        AsciiToAccentMap.put(89 , 'ỗ');
//        AsciiToAccentMap.put(90 , 'ộ');
//        AsciiToAccentMap.put(91 , 'ớ');
//        AsciiToAccentMap.put(92 , 'ờ');
//        AsciiToAccentMap.put(93 , 'ở');
//        AsciiToAccentMap.put(94 , 'ỡ');
//        AsciiToAccentMap.put(95 , 'ợ');
//        AsciiToAccentMap.put(96 , 'ụ');
//        AsciiToAccentMap.put(97 , 'ủ');
//        AsciiToAccentMap.put(98 , 'ứ');
//        AsciiToAccentMap.put(99 , 'ừ');
//        AsciiToAccentMap.put(100, 'ử');
//        AsciiToAccentMap.put(101, 'ữ');
//        AsciiToAccentMap.put(102, 'ự');
//        AsciiToAccentMap.put(103, 'ỳ');
//        AsciiToAccentMap.put(104, 'ỵ');
//        AsciiToAccentMap.put(105, 'ỷ');
//        AsciiToAccentMap.put(106, 'ỹ');
    }
}
