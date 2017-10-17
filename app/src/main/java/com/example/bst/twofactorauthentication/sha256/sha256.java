package com.example.bst.twofactorauthentication.sha256;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * sha256 is used to convert password to encrypted passwprd
 */
public class sha256 {
    private String salt = "SecretSaltSentenceSha256";
    public String shaConverter(String pass){
        MessageDigest messageDigest=null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update((pass+salt).getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String encryptedPassword = (new BigInteger(messageDigest.digest())).toString(16);
        return encryptedPassword;
    }
}