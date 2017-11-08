package com.btcgarden.bitcoin.jsonrpcclient;

import java.util.Date;
import java.util.List;

import com.btcgarden.bitcoin.jsonrpcclient.Bitcoin.Transaction;


public interface Bitcoin {
    
    //addmultisigaddress
    
    //addnode
    
    //backupwallet
    
    //createrawtransaction
    
    //decoderawtransaction
    
    public String dumpPrivKey(String address) throws BitcoinException;
    
    //encryptwallet
    
    public String getAccount(String address) throws BitcoinException;
    
    //getaccountaddress
    
    //getaddednodeinfo
    
    public List<String> getAddressesByAccount(String account) throws BitcoinException;
    
    /**
     * 
     * @return returns the server's total available balance
     * @throws BitcoinException 
     */
    public double getBalance() throws BitcoinException;
    
    /**
     * 
     * @param account
     * @return returns the balance in the account
     * @throws BitcoinException 
     */
    public double getBalance(String account) throws BitcoinException;
    
    /**
     * 
     * @param account
     * @param minConf
     * @return returns the balance in the account
     * @throws BitcoinException 
     */
    public double getBalance(String account, int minConf) throws BitcoinException;

    public static interface Block {
        public String hash();
        public int confirmations();
        public int size();
        public int height();
        public int version();
        public String merkleRoot();
        public List<String> tx();
        public Date time();
        public long nonce();
        public String bits();
        public double difficulty();
        public String previousHash();
        public String nextHash();
        public Block previous() throws BitcoinException;
        public Block next() throws BitcoinException;
    }
    public Block getBlock(String blockHash) throws BitcoinException;
    
    public int getBlockCount() throws BitcoinException;
    
    //getblockhash
    
    //getblocknumber - deprecated
    
    //getconnectioncount
    
    //getdifficulty
    
    //getgenerate
    
    //gethashespersec
    
    //getinfo
    
    //getmemorypool
    
    //getmininginfo
    
    //getnewaddress
    public String getNewAddress() throws BitcoinException;
    public String getNewAddress(String account) throws BitcoinException;
    
    //getpeerinfo
    
    //getrawmempool
    
    //getrawtransaction
    
    //getreceivedbyaccount
    
    public double getReceivedByAddress(String address) throws BitcoinException;
    /**
     * Returns the total amount received by &lt;bitcoinaddress&gt; in transactions with at least [minconf] confirmations. While some might
     * consider this obvious, value reported by this only considers *receiving* transactions. It does not check payments that have been made
     * *from* this address. In other words, this is not "getaddressbalance". Works only for addresses in the local wallet, external
     * addresses will always show 0.
     *
     * @param address
     * @param minConf
     * @return the total amount received by &lt;bitcoinaddress&gt;
     */
    public double getReceivedByAddress(String address, int minConf) throws BitcoinException;
    
    //gettransaction
    
    //getwork
    
    //help
    
    public void importPrivKey(String bitcoinPrivKey) throws BitcoinException;
    public void importPrivKey(String bitcoinPrivKey, String label) throws BitcoinException;
    public void importPrivKey(String bitcoinPrivKey, String label, boolean rescan) throws BitcoinException;
    
    //keypoolrefill
    
    //listaccounts
    
    //listaddressgroupings
    
    //listreceivedbyaccount
    
    public static interface ReceviedAddress {
        public String address();
        public String account();
        public double amount();
        public int confirmations();
    }

    public List<ReceviedAddress> listReceivedByAddress() throws BitcoinException;
    public List<ReceviedAddress> listReceivedByAddress(int minConf) throws BitcoinException;
    public List<ReceviedAddress> listReceivedByAddress(int minConf, boolean includeEmpty) throws BitcoinException;
    
    /**
     * returned by listsinceblock and  listtransactions
     */
    public static interface Transaction {
        public String account();
        public String address();
        public String category();
        public double amount();
        public double fee();
        public int confirmations();
        public String blockHash();
        public int blockIndex();
        public Date blockTime();
        public String txId();
        public Date time();
        public Date timeReceived();
        public String comment();
        public String commentTo();
    }

    //listsinceblock
    public static interface TransactionsSinceBlock {
        public List<Transaction> transactions();
        public String lastBlock();
    }
    
    public TransactionsSinceBlock listSinceBlock() throws BitcoinException;
    public TransactionsSinceBlock listSinceBlock(String blockHash) throws BitcoinException;
    public TransactionsSinceBlock listSinceBlock(String blockHash, int targetConfirmations) throws BitcoinException;
    
    //listtransactions
    public List<Transaction> listTransactions() throws BitcoinException;
    public List<Transaction> listTransactions(String account) throws BitcoinException;
    public List<Transaction> listTransactions(String account, int count) throws BitcoinException;
    public List<Transaction> listTransactions(String account, int count, int from) throws BitcoinException;
    
    //listunspent
    
    //listlockunspent
    
    //lockunspent
    
    //move
    
    public String sendFrom(String fromAccount, String toBitcoinAddress, double amount) throws BitcoinException;
    public String sendFrom(String fromAccount, String toBitcoinAddress, double amount, int minConf) throws BitcoinException;
    public String sendFrom(String fromAccount, String toBitcoinAddress, double amount, int minConf, String comment) throws BitcoinException;
    /**
     * Will send the given amount to the given address, ensuring the account has a valid balance using minConf confirmations.
     * @param fromAccount
     * @param toBitcoinAddress
     * @param amount is a real and is rounded to 8 decimal places
     * @param minConf
     * @return the transaction ID if successful
     * @throws BitcoinException 
     */
    public String sendFrom(String fromAccount, String toBitcoinAddress, double amount, int minConf, String comment, String commentTo) throws BitcoinException;
    
    //sendmany
    
    //sendrawtransaction
    
    public String sendToAddress(String toAddress, double amount) throws BitcoinException;
    public String sendToAddress(String toAddress, double amount, String comment) throws BitcoinException;
    /**
     * 
     * @param toAddress
     * @param amount is a real and is rounded to 8 decimal places
     * @param comment
     * @param commentTo
     * @return the transaction ID &lt;txid&gt; if successful
     * @throws BitcoinException 
     */
    public String sendToAddress(String toAddress, double amount, String comment, String commentTo) throws BitcoinException;

    //setaccount
    
    //setgenerate
    
    //signmessage
    
    //signrawtransaction
    
    //settxfee
    
    //stop
    
    public static interface AddressValidationResult {
        public boolean isValid();
        public String address();
        public boolean isMine();
        public boolean isScript();
        public String pubKey();
        public boolean isCompressed();
        public String account();
    }

    public AddressValidationResult validateAddress(String address) throws BitcoinException;

	int getTransactionConfirms(String txId) throws BitcoinException;
    
    //verifymessage
    
    //walletlock
    
    //walletpassphrase
    
    //walletpassphrasechange
    

}
