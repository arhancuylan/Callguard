package com.callguard;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.io.ByteArrayInputStream;
import java.util.Date;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/** TLS RSA identity encrypted at rest with a device-only Android Keystore AES key. */
final class AdbIdentity {
    final PrivateKey key;final java.security.cert.Certificate certificate;
    private AdbIdentity(PrivateKey key,java.security.cert.Certificate certificate){this.key=key;this.certificate=certificate;}
    static AdbIdentity load(Context context)throws Exception{
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);String alias="callguard-adb-wrap-v1";
        if(!store.containsAlias(alias)){
            KeyGenerator generator=KeyGenerator.getInstance("AES","AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());generator.generateKey();
        }
        SecretKey wrapping=(SecretKey)store.getKey(alias,null);
        SharedPreferences prefs=context.getSharedPreferences("local-adb-identity",0);
        String encrypted=prefs.getString("identity",null);
        if(encrypted!=null){
            String[] parts=encrypted.split(":");if(parts.length!=3)throw new GeneralSecurityException();
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,wrapping,new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
            byte[] raw=cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP));
            try{return new AdbIdentity(KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(raw)),CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(Base64.decode(parts[2],Base64.NO_WRAP))));}
            finally{java.util.Arrays.fill(raw,(byte)0);}
        }
        KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KeyPair pair=generator.generateKeyPair();
        X500Name subject=new X500Name("CN=CallGuard local ADB");
        java.security.cert.Certificate certificate=new JcaX509CertificateConverter().getCertificate(new JcaX509v3CertificateBuilder(subject,new BigInteger(128,new SecureRandom()),new Date(0),new Date(4102444800000L),subject,pair.getPublic()).build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate())));
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,wrapping);byte[] raw=pair.getPrivate().getEncoded();String value;
        try{value=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(raw),Base64.NO_WRAP)+":"+Base64.encodeToString(certificate.getEncoded(),Base64.NO_WRAP);}finally{java.util.Arrays.fill(raw,(byte)0);}
        if(!prefs.edit().putString("identity",value).commit())throw new java.io.IOException("Identity could not be saved");
        return new AdbIdentity(pair.getPrivate(),certificate);
    }
    private AdbIdentity(){key=null;certificate=null;}
}
