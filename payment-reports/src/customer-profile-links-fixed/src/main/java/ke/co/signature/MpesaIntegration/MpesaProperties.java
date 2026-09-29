package ke.co.signature.MpesaIntegration;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MpesaProperties {
    @Value("${mpesa.consumer.key}")
    private String consumerKey;

    @Value("${mpesa.consumer.secret}")
    private String consumerSecret;

    @Value("${mpesa.shortcode}")
    private String shortCode;

    @Value("${mpesa.passkey}")
    private String passkey;

    @Value("${mpesa.callback.url}")
    private String callbackUrl;

    @Value("${mpesa.environment}")
    private String environment;

    @Value("${mpesa.sandbox.oauth}")
    private String sandboxOauth;

    @Value("${mpesa.sandbox.stk}")
    private String sandboxStk;

    @Value("${mpesa.production.oauth}")
    private String prodOauth;

    @Value("${mpesa.production.stk}")
    private String prodStk;

    // getters
    public String getConsumerKey() { return consumerKey; }
    public String getConsumerSecret() { return consumerSecret; }
    public String getShortCode() { return shortCode; }
    public String getPasskey() { return passkey; }
    public String getCallbackUrl() { return callbackUrl; }
    public String getEnvironment() { return environment; }
    public String getSandboxOauth() { return sandboxOauth; }
    public String getSandboxStk() { return sandboxStk; }
    public String getProdOauth() { return prodOauth; }
    public String getProdStk() { return prodStk; }

    public String getOauthUrl(){
        return "production".equalsIgnoreCase(environment) ? prodOauth : sandboxOauth;
    }
    public String getStkUrl(){
        return "production".equalsIgnoreCase(environment) ? prodStk : sandboxStk;
    }
}
