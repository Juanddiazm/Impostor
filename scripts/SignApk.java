import com.android.apksig.ApkSigner;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Firma un APK (v1 + v2) usando la librería apksig. */
public class SignApk {
    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            System.err.println("uso: SignApk <in.apk> <out.apk> <keystore> <storepass> <alias> <keypass>");
            System.exit(2);
        }
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream in = new FileInputStream(args[2])) {
            ks.load(in, args[3].toCharArray());
        }
        PrivateKey key = (PrivateKey) ks.getKey(args[4], args[5].toCharArray());
        List<X509Certificate> certs = new ArrayList<>();
        for (Certificate c : ks.getCertificateChain(args[4])) certs.add((X509Certificate) c);

        ApkSigner.SignerConfig signer = new ApkSigner.SignerConfig.Builder(args[4], key, certs).build();
        new ApkSigner.Builder(Collections.singletonList(signer))
                .setInputApk(new File(args[0]))
                .setOutputApk(new File(args[1]))
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .build()
                .sign();
        System.out.println("APK firmado: " + args[1]);
    }
}
