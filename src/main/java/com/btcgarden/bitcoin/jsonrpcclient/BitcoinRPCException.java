
package com.btcgarden.bitcoin.jsonrpcclient;


public class BitcoinRPCException extends BitcoinException {

    /**
     * Creates a new instance of
     * <code>BitcoinRPCException</code> without detail message.
     */
    public BitcoinRPCException() {
    }

    /**
     * Constructs an instance of
     * <code>BitcoinRPCException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public BitcoinRPCException(String msg) {
        super(msg);
    }

    public BitcoinRPCException(Throwable cause) {
        super(cause);
    }

    public BitcoinRPCException(String message, Throwable cause) {
        super(message, cause);
    }

}
