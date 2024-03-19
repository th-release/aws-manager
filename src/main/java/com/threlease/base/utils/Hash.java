package com.threlease.base.utils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

public class Hash {
    public String generateSHA512(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");

            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Handle NoSuchAlgorithmException (unavailable algorithm)
            e.printStackTrace();
            return null;
        }
    }

    public String generateSHA256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Handle NoSuchAlgorithmException (unavailable algorithm)
            e.printStackTrace();
            return null;
        }
    }

    public String base64_encode(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes());
    }

    public String base64_decode(String data) {
        StringBuilder sb = new StringBuilder();
        byte[] decode_array = Base64.getDecoder().decode(data);

        for (byte decode : decode_array) {
            sb.append((char) decode);
        }

        return sb.toString();
    }
}
