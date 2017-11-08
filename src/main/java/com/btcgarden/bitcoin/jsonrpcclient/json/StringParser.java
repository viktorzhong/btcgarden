package com.btcgarden.bitcoin.jsonrpcclient.json;


public class StringParser {

    public String string;

    public StringParser(String string) {
        this.string = string;
    }

    public void forward(int chars) {
        string = string.substring(chars);
    }

    public char poll() {
        char c = string.charAt(0);
        forward(1);
        return c;
    }

    public String poll(int length) {
        String str = string.substring(0, length);
        forward(length);
        return str;
    }

    public String pollBeforeSkipDelim(String s) {
        int i = string.indexOf(s);
        if (i == -1)
            throw new RuntimeException("\"" + s + "\" not found in \"" + string + "\"");
        String rv = string.substring(0, i);
        forward(i + s.length());
        return rv;
    }

    public char peek() {
        return string.charAt(0);
    }

    public String peek(int length) {
        return string.substring(0, length);
    }

    public String trim() {
        return string = string.trim();
    }

    public boolean isEmpty() {
        return string.isEmpty();
    }

    @Override
    public String toString() {
        return string;
    }

}
