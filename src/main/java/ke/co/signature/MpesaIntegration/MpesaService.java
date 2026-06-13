package ke.co.signature.MpesaIntegration;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class MpesaService {

    @Autowired
    private MpesaProperties mpesaProperties;

    @Autowired
    private MpesaTransactionRepository mpesaTransactionRepository;


    private RestTemplate rest = new RestTemplate();

    public String getAccessToken() {

        String url = "https://sandbox.safaricom.co.ke/oauth/v1/generate"
                + "?grant_type=client_credentials";

        String credentials = mpesaProperties.getConsumerKey()
                + ":" + mpesaProperties.getConsumerSecret();

        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", basicAuth);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<OAuthResponse> response =
                rest.exchange(url, HttpMethod.GET, entity, OAuthResponse.class);

        return response.getBody().getAccessToken();
    }


    public InitiateStkResponse initiateStkPush(String phoneNumber, BigDecimal amount,
                                               String accountReference,
                                               Long transactionReferenceId,
                                               String transactionDesc) {
        String token = getAccessToken();
        String url = mpesaProperties.getStkUrl();

        // timestamp format yyyyMMddHHmmss
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String passwd = mpesaProperties.getShortCode() + mpesaProperties.getPasskey() + timestamp;
        String password = java.util.Base64.getEncoder().encodeToString(passwd.getBytes());

        Map<String, Object> body = new HashMap<>();
        body.put("BusinessShortCode", mpesaProperties.getShortCode());
        body.put("Password", password);
        body.put("Timestamp", timestamp);
        body.put("TransactionType", "CustomerPayBillOnline");
        body.put("Amount", amount.intValue());
        body.put("PartyA", formatPhone(phoneNumber)); // customer phone
        body.put("PartyB", mpesaProperties.getShortCode()); // till/paybill
        body.put("PhoneNumber", formatPhone(phoneNumber));
        body.put("CallBackURL", mpesaProperties.getCallbackUrl());
        body.put("AccountReference", accountReference);
        body.put("TransactionDesc", transactionDesc);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + token);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        System.out.println("Request");
        System.out.println(body);
        ResponseEntity<InitiateStkResponse> resp = rest.postForEntity(url, entity, InitiateStkResponse.class);

        // Save initial transaction record with status PENDING
        InitiateStkResponse respBody = resp.getBody();
        System.out.println("Response");
        System.out.println(respBody);
        MpesaTransaction tx = new MpesaTransaction();
        if (respBody != null) {
            tx.setMerchantRequestId(respBody.getMerchantRequestId());
            tx.setCheckoutRequestId(respBody.getCheckoutRequestId());
        }
        tx.setPhoneNumber(formatPhone(phoneNumber));
        tx.setAmount(amount);
        tx.setStatus("PENDING");
        tx.setAccountReference(accountReference);
        tx.setTransactionReferenceId(transactionReferenceId);
        mpesaTransactionRepository.save(tx);

        return respBody;
    }

    private String formatPhone(String phone) {
        // Normalize to format 2547xxxxxxxx or 25477xxxxxxx
        if (phone.startsWith("0")) {
            return "254" + phone.substring(1);
        }
        if (phone.startsWith("+")) {
            return phone.substring(1);
        }
        return phone;
    }

    // Called by controller when callback is received
    public void handleCallback(Map<String, Object> callbackBody) {
        // callbackBody typically contains 'stkCallback' map
        if (!callbackBody.containsKey("stkCallback")) {
            return;
        }
        Map<String, Object> stkCallback = (Map<String, Object>) callbackBody.get("stkCallback");
        Integer resultCode = (Integer) stkCallback.get("ResultCode");
        String checkoutRequestId = (String) stkCallback.get("CheckoutRequestID");
        String callbackDescription = (String) stkCallback.get("ResultDesc");
        MpesaTransaction tx = mpesaTransactionRepository.findByCheckoutRequestId(checkoutRequestId);
        if (tx == null) {
            // maybe find by merchantRequestId
            String mrid = (String) stkCallback.get("MerchantRequestID");
            // try to find by merchant id - not implemented here; just log

            return;
        }

        tx.setTimeCallbackReceived(LocalDateTime.now());
        tx.setCallbackResultCode(resultCode);
        tx.setCallbackDescription(callbackDescription);

        if (resultCode != null && resultCode == 0) { // success
            tx.setStatus("SUCCESS");
            tx.setCompletedAt(LocalDateTime.now());

            // attempt to extract receipt from CallbackMetadata
            Map<String, Object> callbackMeta = (Map<String, Object>) stkCallback.get("CallbackMetadata");
            if (callbackMeta != null && callbackMeta.containsKey("Item")) {
                Object itemsObj = callbackMeta.get("Item");
                if (itemsObj instanceof java.util.List) {
                    java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) itemsObj;
                    for (Map<String, Object> item : items) {
                        String name = (String) item.get("Name");
                        if ("MpesaReceiptNumber".equalsIgnoreCase(name)) {
                            tx.setMpesaReceiptNumber((String) item.get("Value"));
                        }
                    }
                }
            }

            mpesaTransactionRepository.save(tx);
//            studySubscriptionService.stkPaymentSuccessful(tx.getTransactionReferenceId(), tx.getMpesaReceiptNumber());

        } else {
            tx.setStatus("FAILED");
            tx.setCompletedAt(LocalDateTime.now());
            mpesaTransactionRepository.save(tx);
//            studySubscriptionService.stkPaymentFailed(tx.getTransactionReferenceId());
        }
    }
}
