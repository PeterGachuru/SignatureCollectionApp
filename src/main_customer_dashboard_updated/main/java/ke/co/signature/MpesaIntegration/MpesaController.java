package ke.co.signature.MpesaIntegration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/mpesa")
public class MpesaController {

    @Autowired
    private MpesaService mpesaService;

    @Autowired
    private MpesaTransactionRepository mpesaTransactionRepository;

    // Start STK Push
    @PostMapping("/pay")
    public ResponseEntity<?> pay(@RequestBody StkPushRequestDto request) {
        try {
            InitiateStkResponse resp = mpesaService.initiateStkPush(
                    request
            );
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body("Error initiating STK Push: " + ex.getMessage());
        }
    }

    // Callback endpoint that Safaricom will call
    @PostMapping("/callback")
    public ResponseEntity<?> callback(@RequestBody Map<String, Object> body) {
        // Immediately return 200 OK to Daraja, then process
        try {
            System.out.println("MPESA CALLBACK RECEIVED: " + body);
            // The body contains {"Body":{ ... }}. We will extract Body and then pass it
            Map<String, Object> outer = (Map<String, Object>) body.get("Body");
            if (outer != null) {
                mpesaService.handleCallback(outer);
            } else {
                mpesaService.handleCallback(body);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // Safaricom expects 200 with a JSON response usually; respond a success object
        return ResponseEntity.ok("{\"ResultCode\":0,\"ResultDesc\":\"Accepted\"}");
    }

    // Get transactions (quick debug)
    @GetMapping("/transactions")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(mpesaTransactionRepository.findAll());
    }

    @GetMapping("/transaction/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        MpesaTransaction tx = mpesaTransactionRepository.findById(id).orElse(null);
        if (tx == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(tx);
    }
}
