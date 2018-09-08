package org.wasflow.testweb.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wasflow.testweb.util.GsonUtil;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
@RequestMapping("/exclude/**")
@RestController
public class CollectController {

    private static final Logger logger = LoggerFactory.getLogger(CollectController.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping(value = "/logs/was")
    public void collect(@RequestBody String body) throws Exception {
        logger.info("## >> Receive access log. \n" + body);
    }
}
