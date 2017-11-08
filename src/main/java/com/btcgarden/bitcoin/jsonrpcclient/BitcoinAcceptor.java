package com.btcgarden.bitcoin.jsonrpcclient;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class BitcoinAcceptor implements Runnable {
    
    private static final Logger logger = Logger.getLogger(BitcoinAcceptor.class.getCanonicalName());

    public final Bitcoin bitcoin;
    private String lastBlock, monitorBlock = null;
    int monitorDepth;
    private final LinkedHashSet<BitcoinPaymentListener> listeners = new LinkedHashSet<BitcoinPaymentListener>();

    public BitcoinAcceptor(Bitcoin bitcoin, String lastBlock, int monitorDepth) {
        this.bitcoin = bitcoin;
        this.lastBlock = lastBlock;
        this.monitorDepth = monitorDepth;
    }
    
    public BitcoinAcceptor(Bitcoin bitcoin) {
        this(bitcoin, null, 6);
    }

    public BitcoinAcceptor(Bitcoin bitcoin, String lastBlock, int monitorDepth, BitcoinPaymentListener listener) {
        this(bitcoin, lastBlock, monitorDepth);
        listeners.add(listener);
    }

    public BitcoinAcceptor(Bitcoin bitcoin, BitcoinPaymentListener listener) {
        this(bitcoin, null, 12);
        listeners.add(listener);
    }

    public String getAccountAddress(String account) throws BitcoinException {
        List<String> a = bitcoin.getAddressesByAccount(account);
        if (a.isEmpty())
            return bitcoin.getNewAddress(account);
        return a.get(0);
    }

    public synchronized String getLastBlock() {
        return lastBlock;
    }

    public synchronized void setLastBlock(String lastBlock) throws BitcoinException {
        if (this.lastBlock != null)
            throw new IllegalStateException("lastBlock already set");
        this.lastBlock = lastBlock;
        updateMonitorBlock();
    }

    public synchronized BitcoinPaymentListener[] getListeners() {
        return listeners.toArray(new BitcoinPaymentListener[0]);
    }

    public synchronized void addListener(BitcoinPaymentListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(BitcoinPaymentListener listener) {
        listeners.remove(listener);
    }

    private HashSet<String> seen = new HashSet<String>();

    private void updateMonitorBlock() throws BitcoinException {
        monitorBlock = lastBlock;
        for(int i = 0; i < monitorDepth && monitorBlock != null; i++) {
            Bitcoin.Block b = bitcoin.getBlock(monitorBlock);
            monitorBlock = b == null ? null : b.previousHash();
        }
    }

    public synchronized void checkPayments() throws BitcoinException {
        Bitcoin.TransactionsSinceBlock t = monitorBlock == null ? bitcoin.listSinceBlock() : bitcoin.listSinceBlock(monitorBlock);
        for (Bitcoin.Transaction transaction : t.transactions()) {
            if ("receive".equals(transaction.category())) {
                if (!seen.add(transaction.txId()))
                    continue;
                for (BitcoinPaymentListener listener : listeners) {
                    try {
                        listener.transaction(transaction);
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        if (!t.lastBlock().equals(lastBlock)) {
            seen.clear();
            lastBlock = t.lastBlock();
            updateMonitorBlock();
            for (BitcoinPaymentListener listener : listeners) {
                try {
                    listener.block(lastBlock);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private boolean stop = false;
    
    public void stopAccepting() {
        stop = true;
    }
    
    public void run() {
        stop = false;
        long nextCheck = 0;
        while(!(Thread.interrupted() || stop)) {
            if (nextCheck <= System.currentTimeMillis())
                try {
                    nextCheck = System.currentTimeMillis() + 5000;
                    checkPayments();
                } catch (BitcoinException ex) {
                    Logger.getLogger(BitcoinAcceptor.class.getName()).log(Level.SEVERE, null, ex);
                }
            else
                try {
                    Thread.sleep(Math.max(nextCheck - System.currentTimeMillis(), 100));
                } catch (InterruptedException ex) {
                    Logger.getLogger(BitcoinAcceptor.class.getName()).log(Level.WARNING, null, ex);
                }
        }
    }

//    public static void main(String[] args) {
//        //System.out.println(System.getProperties().toString().replace(", ", ",\n"));
//        final Bitcoin bitcoin = new BitcoinJSONRPCClient(true);
//        new BitcoinAcceptor(bitcoin, null, 6, new BitcoinPaymentListener() {
//
//            public void block(String blockHash) {
//                try {
//                    System.out.println("new block: " + blockHash + "; date: " + bitcoin.getBlock(blockHash).time());
//                } catch (BitcoinException ex) {
//                    logger.log(Level.SEVERE, null, ex);
//                }
//            }
//
//            public void transaction(Transaction transaction) {
//                System.out.println("tx: " + transaction.confirmations() + "\t" + transaction.amount() + "\t=> " + transaction.account());
//            }
//        }).run();
//    }

}
