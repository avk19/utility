<!--
  <dependency>
    <groupId>com.nimbusds</groupId>
    <artifactId>nimbus-jose-jwt</artifactId>
    <version>9.32</version> <!-- Check for the latest version -->
</dependency>
-->
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class JwtTokenService {

    private static final String SECRET_KEY = "your_secret_key"; // Replace with your actual secret key
    private static final long EXPIRATION_TIME_MS = 1000 * 60 * 60; // 1 hour

    // Cache for storing tokens
    private static final ConcurrentHashMap<String, String> tokenCache = new ConcurrentHashMap<>();

    // Function to generate a new token
    private static String generateToken(String username) {
        try {
            // Create JWT claims set
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(username)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + EXPIRATION_TIME_MS))
                    .build();

            // Create signed JWT
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet
            );

            // Sign the JWT
            signedJWT.sign(new MACSigner(SECRET_KEY));

            String token = signedJWT.serialize();

            // Store the token in cache
            tokenCache.put(username, token);

            return token;
        } catch (JOSEException e) {
            throw new RuntimeException("Error generating token", e);
        }
    }

    // Function to parse a token and get the claims
    private static JWTClaimsSet parseToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);

        // Verify the JWT
        JWSVerifier verifier = new MACVerifier(SECRET_KEY);
        if (!signedJWT.verify(verifier)) {
            throw new JOSEException("Invalid signature");
        }

        return signedJWT.getJWTClaimsSet();
    }

    // Function to check if a token is valid
    private static boolean isTokenValid(String token) {
        try {
            JWTClaimsSet claimsSet = parseToken(token);
            return claimsSet.getExpirationTime().after(new Date());
        } catch (ParseException | JOSEException e) {
            return false;
        }
    }

    // Function to issue or refresh a token
    public static String getToken(String username) {
        return tokenCache.compute(username, (key, cachedToken) -> {
            if (cachedToken == null || !isTokenValid(cachedToken)) {
                return generateToken(username);
            }
            return cachedToken;
        });
    }

    public static void main(String[] args) {
        String username = "user1";

        // Getting the token using caching
        String token = getToken(username);
        System.out.println("Token: " + token);

        // Simulate fetching token again to test cache
        String tokenAgain = getToken(username);
        System.out.println("Token Again: " + tokenAgain);
    }
}
