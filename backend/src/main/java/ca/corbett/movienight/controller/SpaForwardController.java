package ca.corbett.movienight.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({"/", "/admin", "/admin/{*path}"})
    public String forwardToSpa() {
        return "forward:/frontend/index.html";
    }
}
