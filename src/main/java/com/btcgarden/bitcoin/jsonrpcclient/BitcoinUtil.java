

package com.btcgarden.bitcoin.jsonrpcclient;


public class BitcoinUtil {
    
    public static double normalizeAmount(double amount) {
        return (long)(0.5d + (amount / 0.00000001)) * 0.00000001;
    }

//    public static void main(String[] args) {
//        NumberFormat f = new DecimalFormat("#.#########");
//        System.out.println(f.format(normalizeAmount(1d)) + ":\n1");
//        System.out.println(f.format(normalizeAmount(0.00000001d)) + ":\n0.00000001");
//        System.out.println(f.format(normalizeAmount(0.000000001d)) + ":\n0");
//        System.out.println(f.format(normalizeAmount(0.000000006d)) + ":\n0.00000001");
//        System.out.println(f.format(normalizeAmount(22123123.12312312d)) + ":\n22123123.12312312");
//    }

}
