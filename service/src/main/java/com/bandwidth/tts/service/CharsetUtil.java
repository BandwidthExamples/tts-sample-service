package com.bandwidth.tts.service;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.stream.Collectors;

interface CharsetUtil {
    Charset SUPPORTED_CHARSET = Charset.forName("ISO_8859-15");

    static String filterUnsupportedCharacters(String text) {
        final CharsetEncoder charsetEncoder = SUPPORTED_CHARSET.newEncoder();
        return text.chars()
                   .mapToObj(currentChar -> Character.toString((char) currentChar))
                   .filter(charsetEncoder::canEncode)
                   .collect(Collectors.joining());
    }
}
