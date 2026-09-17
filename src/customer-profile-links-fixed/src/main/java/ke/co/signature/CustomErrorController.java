package ke.co.signature;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;
import java.util.Date;

@Controller
public class CustomErrorController implements ErrorController {

    @Autowired
    private ErrorAttributes errorAttributes;

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        WebRequest webRequest = new ServletWebRequest(request);

        // Get all error attributes
        Map<String, Object> errors = errorAttributes.getErrorAttributes(webRequest, ErrorAttributeOptions.defaults());

        Integer statusCode = (Integer) errors.get("status");
        String error = (String) errors.get("error");
        String message = (String) errors.get("message");
        String path = (String) errors.get("path");
        Date timestamp = new Date(); // optional: you can use errors.get("timestamp")

        model.addAttribute("status", statusCode);
        model.addAttribute("error", error);
        model.addAttribute("message", message);
        model.addAttribute("path", path);
        model.addAttribute("timestamp", timestamp);

        return "error/error";
    }
}
