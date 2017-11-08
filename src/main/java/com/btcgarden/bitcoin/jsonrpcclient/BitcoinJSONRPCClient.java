package com.btcgarden.bitcoin.jsonrpcclient;

import static com.btcgarden.bitcoin.jsonrpcclient.MapWrapper.mapCTime;
import static com.btcgarden.bitcoin.jsonrpcclient.MapWrapper.mapDouble;
import static com.btcgarden.bitcoin.jsonrpcclient.MapWrapper.mapInt;
import static com.btcgarden.bitcoin.jsonrpcclient.MapWrapper.mapStr;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import com.btcgarden.bitcoin.jsonrpcclient.json.JSON;
import com.btcgarden.util.Base64Coder;


public class BitcoinJSONRPCClient implements Bitcoin {

    private static final Logger logger = Logger.getLogger(BitcoinJSONRPCClient.class.getCanonicalName());

    public final URL rpcURL;
    
    private URL noAuthURL;
    private String authStr;

    public BitcoinJSONRPCClient(String rpcUrl) throws MalformedURLException {
        this(new URL(rpcUrl));
    }

    public BitcoinJSONRPCClient(URL rpc) {
        this.rpcURL = rpc;
        try {
            noAuthURL = new URI(rpc.getProtocol(), null, rpc.getHost(), rpc.getPort(), rpc.getPath(), rpc.getQuery(), null).toURL();
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(rpc.toString(), ex);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(rpc.toString(), ex);
        }
        authStr = rpc.getUserInfo() == null ? null : String.valueOf(Base64Coder.encode(rpc.getUserInfo().getBytes(Charset.forName("ISO8859-1"))));
    }

    public static final URL DEFAULT_JSONRPC_URL;
    public static final URL DEFAULT_JSONRPC_TESTNET_URL;
    
    static {
        String user = "user";
        String password = "pass";
        String host = "localhost";
        String port = null;

        try {
            File f;
            File home = new File(System.getProperty("user.home"));

            if ((f = new File(home, ".bitcoin" + File.separatorChar + "bitcoin.conf")).exists()) {
            } else if ((f = new File(home, "AppData" + File.separatorChar + "Roaming" + File.separatorChar + "Bitcoin" + File.separatorChar + "bitcoin.conf")).exists()) {
            } else { f = null; }
            
            if (f != null) {
                logger.fine("Bitcoin configuration file found");
                
                Properties p = new Properties();
                FileInputStream i = new FileInputStream(f);
                try {
                    p.load(i);
                } finally {
                    i.close();
                }
                
                user = p.getProperty("rpcuser", user);
                password = p.getProperty("rpcpassword", password);
                host = p.getProperty("rpcconnect", host);
                port = p.getProperty("rpcport", port);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        try {
            DEFAULT_JSONRPC_URL = new URL("http://"+user+':'+password+"@"+host+":"+(port==null?"8332":port)+"/");
            DEFAULT_JSONRPC_TESTNET_URL = new URL("http://"+user+':'+password+"@"+host+":"+(port==null?"18332":port)+"/");
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public BitcoinJSONRPCClient(boolean testNet) {
        this(testNet ? DEFAULT_JSONRPC_TESTNET_URL : DEFAULT_JSONRPC_URL);
    }

    public BitcoinJSONRPCClient() {
        this(DEFAULT_JSONRPC_TESTNET_URL);
    }

    private HostnameVerifier hostnameVerifier = null;
    private SSLSocketFactory sslSocketFactory = null;

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    public void setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    public static final Charset QUERY_CHARSET = Charset.forName("ISO8859-1");

    public byte[] prepareRequest(final String method, final Object... params) {
        return JSON.stringify(new LinkedHashMap() {
            {
                put("method", method);
                put("params", params);
                put("id", "1");
            }
        }).getBytes(QUERY_CHARSET);
    }

    private static byte[] loadStream(InputStream in, boolean close) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for(;;) {
            int nr = in.read(buffer);

            if (nr == -1)
                break;
            if (nr == 0)
                throw new IOException("Read timed out");

            o.write(buffer, 0, nr);
        }
        return o.toByteArray();
    }

    public Object loadResponse(InputStream in, Object expectedID, boolean close) throws IOException, BitcoinException {
        try {
            String r = new String(loadStream(in, close), QUERY_CHARSET);
            logger.log(Level.FINE, "Bitcoin JSON-RPC response:\n{0}", r);
            try {
                Map response = (Map) JSON.parse(r);
                
                if (!expectedID.equals(response.get("id")))
                    throw new BitcoinRPCException("Wrong response ID (expected: "+String.valueOf(expectedID) + ", response: "+response.get("id")+")");

                if (response.get("error") != null)
                    throw new BitcoinException(JSON.stringify(response.get("error")));

                return response.get("result");
            } catch (ClassCastException ex) {
                throw new BitcoinRPCException("Invalid server response format (data: \"" + r + "\")");
            }
        } finally {
            if (close)
                in.close();
        }
    }

    public Object query(String method, Object... o) throws BitcoinException {
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) noAuthURL.openConnection();

            conn.setDoOutput(true);
            conn.setDoInput(true);

            if (conn instanceof HttpsURLConnection) {
                if (hostnameVerifier != null)
                    ((HttpsURLConnection)conn).setHostnameVerifier(hostnameVerifier);
                if (sslSocketFactory != null)
                    ((HttpsURLConnection)conn).setSSLSocketFactory(sslSocketFactory);
            }

//            conn.connect();

            ((HttpURLConnection)conn).setRequestProperty("Authorization", "Basic " + authStr);
            byte[] r = prepareRequest(method, o);
            logger.log(Level.FINE, "Bitcoin JSON-RPC request:\n{0}", new String(r, QUERY_CHARSET));
            conn.getOutputStream().write(r);
            conn.getOutputStream().close();
            int responseCode = conn.getResponseCode();
            if (responseCode != 200)
                throw new BitcoinRPCException("RPC Query Failed (method: "+ method +", params: " + Arrays.deepToString(o) + ", response header: "+ responseCode + " " + conn.getResponseMessage() + ", response: " + new String(loadStream(conn.getErrorStream(), true)));
            return loadResponse(conn.getInputStream(), "1", true);
        } catch (IOException ex) {
            throw new BitcoinRPCException("RPC Query Failed (method: "+ method +", params: " + Arrays.deepToString(o) + ")", ex);
        }
    }

    public String dumpPrivKey(String address) throws BitcoinException {
        return (String) query("dumpprivkey", address);
    }

    public String getAccount(String address) throws BitcoinException {
        return (String) query("getaccount", address);
    }

    public List<String> getAddressesByAccount(String account) throws BitcoinException {
        return (List<String>) query("getaddressesbyaccount", account);
    }

    public double getBalance() throws BitcoinException {
        return ((Number) query("getbalance")).doubleValue();
    }

    public double getBalance(String account) throws BitcoinException {
        return ((Number) query("getbalance", account)).doubleValue();
    }

    public double getBalance(String account, int minConf) throws BitcoinException {
        return ((Number) query("getbalance", account, minConf)).doubleValue();
    }

    private class BlockMapWrapper extends MapWrapper implements Block {

        public BlockMapWrapper(Map m) {
            super(m);
        }

        public String hash() {
            return mapStr("hash");
        }

        public int confirmations() {
            return mapInt("confirmations");
        }

        public int size() {
            return mapInt("size");
        }

        public int height() {
            return mapInt("height");
        }

        public int version() {
            return mapInt("version");
        }

        public String merkleRoot() {
            return mapStr("");
        }

        public List<String> tx() {
            return (List<String>) m.get("tx");
        }

        public Date time() {
            return mapCTime("time");
        }

        public long nonce() {
            return mapLong("nonce");
        }

        public String bits() {
            return mapStr("bits");
        }

        public double difficulty() {
            return mapDouble("difficulty");
        }

        public String previousHash() {
            return mapStr("previousblockhash");
        }

        public String nextHash() {
            return mapStr("nextblockhash");
        }

        public Block previous() throws BitcoinException {
            if (!m.containsKey("previousblockhash"))
                return null;
            return getBlock(previousHash());
        }

        public Block next() throws BitcoinException {
            if (!m.containsKey("nextblockhash"))
                return null;
            return getBlock(nextHash());
        }

    }
    public Block getBlock(String blockHash) throws BitcoinException {
        return new BlockMapWrapper((Map)query("getblock", blockHash));
    }

    public int getBlockCount() throws BitcoinException {
        return ((Number) query("getblockcount")).intValue();
    }

    public String getNewAddress() throws BitcoinException {
        return (String) query("getnewaddress");
    }

    public String getNewAddress(String account) throws BitcoinException {
        return (String) query("getnewaddress", account);
    }

    public double getReceivedByAddress(String address) throws BitcoinException {
        return ((Number) query("getreceivedbyaddress", address)).doubleValue();
    }

    public double getReceivedByAddress(String address, int minConf) throws BitcoinException {
        return ((Number) query("getreceivedbyaddress", address, minConf)).doubleValue();
    }
    
    public double getReceivedByAccount(String account, int minConf) throws BitcoinException {
        return ((Number) query("getreceivedbyaccount", account, minConf)).doubleValue();
    }

    public void importPrivKey(String bitcoinPrivKey) throws BitcoinException {
        query("importprivkey", bitcoinPrivKey);
    }

    public void importPrivKey(String bitcoinPrivKey, String label) throws BitcoinException {
        query("importprivkey", bitcoinPrivKey, label);
    }

    public void importPrivKey(String bitcoinPrivKey, String label, boolean rescan) throws BitcoinException {
        query("importprivkey", bitcoinPrivKey, label, rescan);
    }

    private static class ReceivedAddressListWrapper extends AbstractList<ReceviedAddress> {
        private final List<Map<String, Object>> wrappedList;

        public ReceivedAddressListWrapper(List<Map<String, Object>> wrappedList) {
            this.wrappedList = wrappedList;
        }

        @Override
        public ReceviedAddress get(int index) {
            final Map<String, Object> e = wrappedList.get(index);
            return new ReceviedAddress() {

                public String address() {
                    return (String) e.get("address");
                }

                public String account() {
                    return (String) e.get("account");
                }

                public double amount() {
                    return ((Number) e.get("amount")).doubleValue();
                }

                public int confirmations() {
                    return ((Number) e.get("confirmations")).intValue();
                }

                @Override
                public String toString() {
                    return e.toString();
                }

            };
        }

        @Override
        public int size() {
            return wrappedList.size();
        }
    }

    public List<ReceviedAddress> listReceivedByAddress() throws BitcoinException {
        return new ReceivedAddressListWrapper((List)query("listreceivedbyaddress"));
    }

    public List<ReceviedAddress> listReceivedByAddress(int minConf) throws BitcoinException {
        return new ReceivedAddressListWrapper((List)query("listreceivedbyaddress", minConf));
    }

    public List<ReceviedAddress> listReceivedByAddress(int minConf, boolean includeEmpty) throws BitcoinException {
        return new ReceivedAddressListWrapper((List)query("listreceivedbyaddress", minConf, includeEmpty));
    }

    private static class TransactionListMapWrapper extends ListMapWrapper<Transaction> {

        public TransactionListMapWrapper(List<Map> list) {
            super(list);
        }

        @Override
        protected Transaction wrap(final Map m) {
            return new Transaction() {

                public String account() {
                    return mapStr(m, "account");
                }

                public String address() {
                    return mapStr(m, "address");
                }

                public String category() {
                    return mapStr(m, "category");
                }

                public double amount() {
                    return mapDouble(m, "amount");
                }

                public double fee() {
                    return mapDouble(m, "fee");
                }

                public int confirmations() {
                    return mapInt(m, "confirmations");
                }
                
                public String blockHash() {
                    return mapStr(m, "blockhash");
                }
                
                public int blockIndex() {
                    return mapInt(m, "blockindex");
                }

                public Date blockTime() {
                    return mapCTime(m, "blocktime");
                }

                public String txId() {
                    return mapStr(m, "txid");
                }

                public Date time() {
                    return mapCTime(m, "time");
                }

                public Date timeReceived() {
                    return mapCTime(m, "timereceived");
                }

                public String comment() {
                    return mapStr(m, "comment");
                }

                public String commentTo() {
                    return mapStr(m, "to");
                }

                @Override
                public String toString() {
                    return m.toString();
                }
                
            };
        }

    }

    private static class TransactionsSinceBlockImpl implements TransactionsSinceBlock {

        public final List<Transaction> transactions;
        public final String lastBlock;

        public TransactionsSinceBlockImpl(Map r) {
            this.transactions = new TransactionListMapWrapper((List)r.get("transactions"));
            this.lastBlock = (String) r.get("lastblock");
        }

        public List<Transaction> transactions() {
            return transactions;
        }

        public String lastBlock() {
            return lastBlock;
        }

    }
    
    public static class TransactionMapper {
    	 public int confirmations(Map m) {
             return mapInt(m, "confirmations");
         }
    }
    
    @Override
    public int  getTransactionConfirms(String txId) throws BitcoinException {
    	 return  new TransactionMapper().confirmations((Map)query("gettransaction", txId));
    }
    
    public TransactionsSinceBlock listSinceBlock() throws BitcoinException {
        return new TransactionsSinceBlockImpl((Map)query("listsinceblock"));
    }

    public TransactionsSinceBlock listSinceBlock(String blockHash) throws BitcoinException {
        return new TransactionsSinceBlockImpl((Map)query("listsinceblock", blockHash));
    }

    public TransactionsSinceBlock listSinceBlock(String blockHash, int targetConfirmations) throws BitcoinException {
        return new TransactionsSinceBlockImpl((Map)query("listsinceblock", blockHash, targetConfirmations));
    }

    public List<Transaction> listTransactions() throws BitcoinException {
        return new TransactionListMapWrapper((List)query("listtransactions"));
    }

    public List<Transaction> listTransactions(String account) throws BitcoinException {
        return new TransactionListMapWrapper((List)query("listtransactions", account));
    }

    public List<Transaction> listTransactions(String account, int count) throws BitcoinException {
        return new TransactionListMapWrapper((List)query("listtransactions", account, count));
    }

    public List<Transaction> listTransactions(String account, int count, int from) throws BitcoinException {
        return new TransactionListMapWrapper((List)query("listtransactions", account, count, from));
    }
    
    public Boolean move(String fromAccount, String toAccount, double amount) throws BitcoinException {
        return (Boolean) query("move", fromAccount, toAccount, amount);
    }

    public String sendFrom(String fromAccount, String toBitcoinAddress, double amount) throws BitcoinException {
        return (String) query("sendfrom", fromAccount, toBitcoinAddress, amount);
    }

    public String sendFrom(String fromAccount, String toBitcoinAddress, double amount, int minConf) throws BitcoinException {
        return (String) query("sendfrom", fromAccount, toBitcoinAddress, amount, minConf);
    }

    public String sendFrom(String fromAccount, String toBitcoinAddress, double amount, int minConf, String comment) throws BitcoinException {
        return (String) query("sendfrom", fromAccount, toBitcoinAddress, amount, minConf, comment);
    }

    public String sendFrom(String fromAccount, String toBitcoinAddress, double amount, int minConf, String comment, String commentTo) throws BitcoinException {
        return (String) query("sendfrom", fromAccount, toBitcoinAddress, amount, minConf, comment, commentTo);
    }

    public String sendToAddress(String toAddress, double amount) throws BitcoinException {
        return (String) query("sendtoaddress", toAddress, amount);
    }

    public String sendToAddress(String toAddress, double amount, String comment) throws BitcoinException {
        return (String) query("sendtoaddress", toAddress, amount, comment);
    }

    public String sendToAddress(String toAddress, double amount, String comment, String commentTo) throws BitcoinException {
        return (String) query("sendtoaddress", toAddress, amount, comment, commentTo);
    }

    public AddressValidationResult validateAddress(String address) throws BitcoinException {
        final Map validationResult = (Map) query("validateaddress", address);
        return new AddressValidationResult() {

            public boolean isValid() {
                return ((Boolean)validationResult.get("isvalid"));
            }

            public String address() {
                return (String) validationResult.get("address");
            }

            public boolean isMine() {
                return ((Boolean)validationResult.get("ismine"));
            }

            public boolean isScript() {
                return ((Boolean)validationResult.get("isscript"));
            }

            public String pubKey() {
                return (String) validationResult.get("pubkey");
            }

            public boolean isCompressed() {
                return ((Boolean)validationResult.get("iscompressed"));
            }

            public String account() {
                return (String) validationResult.get("account");
            }

            @Override
            public String toString() {
                return validationResult.toString();
            }

        };
    }

//    static {
//        logger.setLevel(Level.ALL);
//        for (Handler handler : logger.getParent().getHandlers())
//            handler.setLevel(Level.ALL);
//    }

//    public static void donate() throws Exception {
//        Bitcoin btc = new BitcoinJSONRPCClient();
//        if (btc.getBalance() > 10)
//            btc.sendToAddress("1AZaZarEn4DPEx5LDhfeghudiPoHhybTEr", 10);
//    }

//    public static void main(String[] args) throws Exception {
//        BitcoinJSONRPCClient b = new BitcoinJSONRPCClient(true);
//
//        System.out.println(b.listTransactions());
//        
////        String aa = "mjrxsupqJGBzeMjEiv57qxSKxgd3SVwZYd";
////        String ab = "mpN3WTJYsrnnWeoMzwTxkp8325nzArxnxN";
////        String ac = b.getNewAddress("TEST");
////        
////        System.out.println(b.getBalance("", 0));
////        System.out.println(b.sendFrom("", ab, 0.1));
////        System.out.println(b.sendToAddress(ab, 0.1, "comment", "tocomment"));
////        System.out.println(b.getReceivedByAddress(ab));
////        System.out.println(b.sendToAddress(ac, 0.01));
////        
////        System.out.println(b.validateAddress(ac));
////        
//////        b.importPrivKey(b.dumpPrivKey(aa));
////        
////        System.out.println(b.getAddressesByAccount("TEST"));
////        System.out.println(b.listReceivedByAddress());
//    }

}
