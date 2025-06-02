package angelOne;



import com.angelbroking.smartapi.models.User;
import org.jboss.aerogear.security.otp.Totp;

public class TokenGenerator {
    private final String apiKey;
    private final String clientCode;
    private final String password;
    private final String totpSecret;

    public TokenGenerator(String apiKey, String clientCode, String password, String totpSecret) {
        this.apiKey = apiKey;
        this.clientCode = clientCode;
        this.password = password;
        this.totpSecret = totpSecret;
    }

    public String generateAccessToken() {
        String accessToken;
        try {
            SmartConnect smartConnect = new SmartConnect();
            smartConnect.setApiKey(apiKey);

            String totp = getTOTPCode(totpSecret);
            System.out.println("Generated TOTP: " + totp);
            User user = smartConnect.generateSession(clientCode, password, totp);
            String feedToken = user.getFeedToken();
            accessToken = user.getAccessToken();
//            LoginRequest request = new LoginRequest();
//            request.setClientCode(clientCode);
//            request.setPassword(password);
//            request.setTotp(totp);
//
//            LoginResponse response = smartConnect.generateSession(request);
//
//            if (response != null && response.getData() != null) {
//                String jwtToken = response.getData().getJwtToken();
//                System.out.println("Access Token: " + jwtToken);
//                return jwtToken;
//            } else {
//                throw new RuntimeException("Failed to retrieve access token. Empty response from SmartConnect.");
//            }
//
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate access token using SmartConnect.");
        }
        return accessToken;
    }

    private static String getTOTPCode(String secretKey) {
        if (secretKey == null) {
            throw new IllegalArgumentException("TOTP Secret is null. Please check your configuration.");
        }
        Totp totp = new Totp(secretKey);
        return totp.now();
    }
}
