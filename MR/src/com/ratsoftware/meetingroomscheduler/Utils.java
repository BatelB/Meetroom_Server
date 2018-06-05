package com.ratsoftware.meetingroomscheduler;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

public class Utils {
	/**
	 * This function hashes the string using MD5 so we will not save the password as clear text
	 * @param param
	 * @return
	 */
    public static String md5(String param){
    	try {
    		MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(param.getBytes());
            byte byteData[] = md.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
             sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
	    	return sb.toString();
    	}catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
    	return "";
    }
    
}
