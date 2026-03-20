package dev.tr7zw.mango2j.controller;

import jakarta.servlet.http.*;
import org.springframework.boot.webmvc.error.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

@Controller
public class GlobalErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        // Custom error handling, for now just redirect to the home page
        /*
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            // Redirect 404 to home page
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "redirect:/";
            }
        }
        */
        return "redirect:/";
    }
}
