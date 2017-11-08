package com.btcgarden.bitcoin.jsonrpcclient;


public class BitcoinException extends Exception {

    /**
     * Creates a new instance of
     * <code>BitcoinException</code> without detail message.
     */
    public BitcoinException() {
    }

    /**
     * Constructs an instance of
     * <code>BitcoinException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public BitcoinException(String msg) {
        super(msg);
    }

    public BitcoinException(Throwable cause) {
        super(cause);
    }

    public BitcoinException(String message, Throwable cause) {
        super(message, cause);
    }

}
