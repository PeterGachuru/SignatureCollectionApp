package ke.co.signature.MpesaIntegration;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CallbackPayload {
    private Map<String, Object> Body;

    public Map<String, Object> getBody() { return Body; }
    public void setBody(Map<String, Object> body) { Body = body; }
}
