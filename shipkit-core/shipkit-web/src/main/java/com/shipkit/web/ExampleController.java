package com.shipkit.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/example")
public class ExampleController {

    @GetMapping
    public String example() {
        return "Hello world!";
    }
}
