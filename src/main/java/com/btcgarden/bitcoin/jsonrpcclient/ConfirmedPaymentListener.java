package com.btcgarden.bitcoin.jsonrpcclient;

import com.btcgarden.bitcoin.jsonrpcclient.Bitcoin.Transaction;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public abstract class ConfirmedPaymentListener extends SimpleBitcoinPaymentListener {

    public int minConf;

    public ConfirmedPaymentListener(int minConf) {
        this.minConf = minConf;
    }

    public ConfirmedPaymentListener() {
        this(6);
    }

    protected Set<String> processed = Collections.synchronizedSet(new HashSet<String>());

    protected boolean markProcess(String txId) {
        return processed.add(txId);
    }

    public void transaction(Transaction transaction) {
        if (transaction.confirmations() < minConf)
            return;
        if (!markProcess(transaction.txId()))
            return;
        confirmed(transaction);
    }

    public abstract void confirmed(Transaction transaction);

}
