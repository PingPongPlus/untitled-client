/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.Callable;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import net.labymod.voice.protocol.udp.session.MalformedFrameException;

public class Encryption {
    private int maxChunkSize = -1;
    private int maxEncryptedChunkSize = -1;
    private Key publicKey;
    private final Callable<Cipher> encryptCipherFactory;
    private final Callable<Cipher> decryptCipherFactory;
    private final Cipher defaultEncryptCipher;
    private Cipher defaultDecryptCipher;
    private Key shareKey;
    private String privateKeyHash;

    public Encryption(String publicKey) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
        this.publicKey = keyFactory.generatePublic(publicKeySpec);
        this.maxChunkSize = 200;
        this.maxEncryptedChunkSize = Encryption.getRsaKeySize(this.publicKey);
        this.encryptCipherFactory = () -> {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(1, this.publicKey);
            return cipher;
        };
        this.defaultEncryptCipher = this.createEncryptCipher();
        this.decryptCipherFactory = null;
    }

    public Encryption(File pubKeyFile) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        if (!pubKeyFile.exists()) {
            throw new IllegalArgumentException("Key generation is not supported here");
        }
        byte[] publicKeyBytes = Files.readAllBytes(pubKeyFile.toPath());
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBytes));
        this.publicKey = keyFactory.generatePublic(publicKeySpec);
        this.maxChunkSize = 200;
        this.maxEncryptedChunkSize = Encryption.getRsaKeySize(this.publicKey);
        this.encryptCipherFactory = () -> {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(1, this.publicKey);
            return cipher;
        };
        this.defaultEncryptCipher = this.createEncryptCipher();
        this.decryptCipherFactory = null;
    }

    public Encryption(File privateKeyFile, File publicKeyFile) throws Exception {
        KeyPair keyPair = null;
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        if (privateKeyFile.exists() && publicKeyFile.exists()) {
            try {
                byte[] publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath());
                byte[] privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath());
                X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBytes));
                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBytes));
                keyPair = new KeyPair(keyFactory.generatePublic(publicKeySpec), keyFactory.generatePrivate(privateKeySpec));
            }
            catch (IOException | InvalidKeySpecException e) {
                e.printStackTrace();
            }
        }
        if (keyPair == null) {
            System.out.println("Keypair could not be loaded, generating new Pair");
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
            try {
                System.out.println("Saving new keypair");
                try (FileOutputStream fos = new FileOutputStream("public.key");){
                    fos.write(Base64.getEncoder().encode(keyPair.getPublic().getEncoded()));
                }
                FileOutputStream fos = new FileOutputStream("private.key");
                try {
                    fos.write(Base64.getEncoder().encode(keyPair.getPrivate().getEncoded()));
                }
                finally {
                    fos.close();
                }
            }
            catch (IOException e) {
                System.err.println("Could not save generated Keys, restarting the Server will generate new keys");
                throw e;
            }
        }
        this.publicKey = keyPair.getPublic();
        this.maxChunkSize = 200;
        this.maxEncryptedChunkSize = Encryption.getRsaKeySize(this.publicKey);
        this.encryptCipherFactory = () -> {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(1, this.publicKey);
            return cipher;
        };
        this.defaultEncryptCipher = this.createEncryptCipher();
        PrivateKey privateKey = keyPair.getPrivate();
        this.decryptCipherFactory = () -> {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(2, privateKey);
            return cipher;
        };
        this.defaultDecryptCipher = this.createDecryptCipher();
        if (!Arrays.equals("42".getBytes(StandardCharsets.UTF_8), this.decrypt(this.encrypt("42".getBytes(StandardCharsets.UTF_8))))) {
            throw new IllegalArgumentException("Keyfiles dont match");
        }
        this.privateKeyHash = Encryption.sha256(privateKey.getEncoded());
    }

    public Encryption(byte[] key) throws Exception {
        this.shareKey = new SecretKeySpec(key, "AES");
        this.encryptCipherFactory = () -> {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(1, this.shareKey);
            return cipher;
        };
        this.defaultEncryptCipher = this.createEncryptCipher();
        this.decryptCipherFactory = () -> {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(2, this.shareKey);
            return cipher;
        };
        this.defaultDecryptCipher = this.createDecryptCipher();
        if (!Arrays.equals("42".getBytes(StandardCharsets.UTF_8), this.decrypt(this.encrypt("42".getBytes(StandardCharsets.UTF_8))))) {
            throw new IllegalArgumentException("Keyfiles dont match");
        }
    }

    public Encryption() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        this.shareKey = keyGenerator.generateKey();
        this.maxChunkSize = -1;
        this.encryptCipherFactory = () -> {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(1, this.shareKey);
            return cipher;
        };
        this.defaultEncryptCipher = this.createEncryptCipher();
        this.decryptCipherFactory = () -> {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(2, this.shareKey);
            return cipher;
        };
        this.defaultDecryptCipher = this.createDecryptCipher();
        if (!Arrays.equals("42".getBytes(StandardCharsets.UTF_8), this.decrypt(this.encrypt("42".getBytes(StandardCharsets.UTF_8))))) {
            throw new IllegalArgumentException("Keyfiles dont match");
        }
    }

    public byte[] encrypt(byte[] data) throws IllegalBlockSizeException, BadPaddingException {
        return this.encrypt(data, this.defaultEncryptCipher);
    }

    public byte[] decrypt(byte[] data) throws IllegalBlockSizeException, BadPaddingException {
        return this.decrypt(data, this.defaultDecryptCipher);
    }

    public byte[] encrypt(byte[] data, Cipher cipher) throws IllegalBlockSizeException, BadPaddingException {
        if (this.maxChunkSize <= 0) {
            return cipher.doFinal(data);
        }
        int chunks = Encryption.ceilDivide(data.length, this.maxChunkSize);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 0; i < chunks; ++i) {
            int chunkStart = this.maxChunkSize * i;
            int currentChunkSize = Math.min(this.maxChunkSize, data.length - chunkStart);
            byte[] encrypted = cipher.doFinal(data, chunkStart, currentChunkSize);
            buffer.write(encrypted.length + -128);
            buffer.write(encrypted, 0, encrypted.length);
        }
        return buffer.toByteArray();
    }

    public byte[] decrypt(byte[] data, Cipher cipher) throws IllegalBlockSizeException, BadPaddingException {
        if (this.maxChunkSize <= 0) {
            return cipher.doFinal(data);
        }
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while (input.available() > 0) {
            int maxEncryptedSize;
            int encryptedSize = input.read() - -128;
            int n = maxEncryptedSize = this.maxEncryptedChunkSize > 0 ? this.maxEncryptedChunkSize : 1024;
            if (encryptedSize <= 0 || encryptedSize > maxEncryptedSize || encryptedSize > input.available()) {
                throw new MalformedFrameException("Invalid encrypted size: " + encryptedSize + " bytes");
            }
            byte[] encrypted = new byte[encryptedSize];
            int read = input.read(encrypted, 0, encryptedSize);
            if (read != encryptedSize) {
                throw new MalformedFrameException("Truncated encrypted payload");
            }
            byte[] decrypted = cipher.doFinal(encrypted);
            output.write(decrypted, 0, decrypted.length);
        }
        return output.toByteArray();
    }

    public Cipher createEncryptCipher() throws Exception {
        if (this.encryptCipherFactory == null) {
            throw new IllegalStateException("No encrypt cipher available");
        }
        return this.encryptCipherFactory.call();
    }

    public Cipher createDecryptCipher() throws Exception {
        if (this.decryptCipherFactory == null) {
            throw new IllegalStateException("No decrypt cipher available");
        }
        return this.decryptCipherFactory.call();
    }

    public Key getPublicKey() {
        return this.publicKey;
    }

    private static int getRsaKeySize(Key key) {
        if (key instanceof RSAKey) {
            return (((RSAKey)((Object)key)).getModulus().bitLength() + 7) / 8;
        }
        return -1;
    }

    public Key getShareKey() {
        return this.shareKey;
    }

    private static int ceilDivide(int a, int b) {
        int result = a / b;
        if (a % b != 0) {
            ++result;
        }
        return result;
    }

    public String getPublicKeyHash() {
        return Encryption.sha256(this.publicKey.getEncoded());
    }

    public String getShareKeyHash() {
        return Encryption.sha256(this.shareKey.getEncoded());
    }

    public String getPrivateKeyHash() {
        return this.privateKeyHash;
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
