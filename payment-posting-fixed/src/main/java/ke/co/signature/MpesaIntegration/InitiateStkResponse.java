package ke.co.signature.MpesaIntegration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InitiateStkResponse {
    @JsonProperty("MerchantRequestID")
    private String merchantRequestId;
    @JsonProperty("CheckoutRequestID")
    private String checkoutRequestId;
    @JsonProperty("ResponseCode")
    private String responseCode;
    @JsonProperty("ResponseDescription")
    private String responseDescription;

    public String getMerchantRequestId() { return merchantRequestId; }
    public void setMerchantRequestId(String merchantRequestId) { this.merchantRequestId = merchantRequestId; }
    public String getCheckoutRequestId() { return checkoutRequestId; }
    public void setCheckoutRequestId(String checkoutRequestId) { this.checkoutRequestId = checkoutRequestId; }
    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }
    public String getResponseDescription() { return responseDescription; }
    public void setResponseDescription(String responseDescription) { this.responseDescription = responseDescription; }

    @Override
    public String toString() {
        return "InitiateStkResponse{" +
                "merchantRequestId='" + merchantRequestId + '\'' +
                ", checkoutRequestId='" + checkoutRequestId + '\'' +
                ", responseCode='" + responseCode + '\'' +
                ", responseDescription='" + responseDescription + '\'' +
                '}';
    }
}
