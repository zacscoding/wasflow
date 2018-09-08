package org.wasflow.testweb.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
@RequestMapping("/sample/**")
@RestController
public class SampleController {

    @GetMapping(value = "/index")
    public String index() {
        return "SUCCESS";
    }
}
