package com.btcgarden.bitcoin.jsonrpcclient;


public interface BitcoinPaymentListener {

    public void block(String blockHash);
    public void transaction(Bitcoin.Transaction transaction);

}
