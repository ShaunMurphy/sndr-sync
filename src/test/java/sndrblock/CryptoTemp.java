package sndrblock;


import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Provider;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import com.sndr.crypto.ECCProvider;
import com.sndr.crypto.EllipticCurveCrypto;
import com.sndr.logger.SndrLogger;

public enum CryptoTemp {
    INSTANCE;
    private final Logger logger = SndrLogger.getLogger();
    private CryptoTemp() {
    }
    
    private final ECCProvider provider = new ECCProvider() {
        private static final String CURVE = "prime256v1";
        private final ECNamedCurveParameterSpec ECC_SPEC = ECNamedCurveTable.getParameterSpec(CURVE);
        
        @Override
        public final Provider getRealProvider() {
            return new BouncyCastleProvider();
        }
        @Override
        public final AlgorithmParameterSpec getCurveAlgorithm() {
            return this.ECC_SPEC;
        }

        @Override
        public final KeySpec createPublic(BigInteger x, BigInteger y) {
            if (x == null || y == null) {
                logger.log(Level.SEVERE, "The BigInteger x and y cannot be null.");
                return null;
            }

            ECPoint q = ECC_SPEC.getCurve().createPoint(x, y);
            return new ECPublicKeySpec(q, ECC_SPEC);
        }

        @Override
        public final KeySpec createPrivate(BigInteger d) {
            if (d == null) {
                logger.log(Level.SEVERE, "The BigInteger d cannot be null.");
                return null;
            }
            return new ECPrivateKeySpec(d, ECC_SPEC);
        }

        @Override
        public final BigInteger[] getXYPoint(ECPublicKey publicKey) {
            if (publicKey == null) {
                logger.log(Level.SEVERE, "The public key cannot be null.");
                return null;
            }

            ECPublicKeyParameters publicParameters;
            try {
                publicParameters = (ECPublicKeyParameters) ECUtil.generatePublicKeyParameter(publicKey);
            } catch (InvalidKeyException e) {
                logger.log(Level.SEVERE, "Filed to generate the public key parameter.", e);
                return null;
            }

            //Get the X,Y coordinate.
            ECPoint q = publicParameters.getQ();

            BigInteger[] point = new BigInteger[2];
            point[0] = q.getRawXCoord().toBigInteger();
            point[1] = q.getRawYCoord().toBigInteger();

            return point;
        }

        @Override
        public final BigInteger getDValue(ECPrivateKey privateKey) {
            if (privateKey == null) {
                logger.log(Level.SEVERE, "getDValue - invalid params");
                return null;
            }

            ECPrivateKeyParameters privateParameters;
            try {
                privateParameters = (ECPrivateKeyParameters) ECUtil.generatePrivateKeyParameter(privateKey);
            } catch (InvalidKeyException e) {
                logger.log(Level.SEVERE, "getDValue - couldn't generate public key parameter", e);
                return null;
            }

            //Get the D value.
            return privateParameters.getD();
        }
    };
    
    /**
     * Creates a new instance of {@link EllipticCurveCrypto}.
     * @return
     */
    public EllipticCurveCrypto createNewInstance() {
        return new EllipticCurveCrypto(this.provider);
    }    
}
