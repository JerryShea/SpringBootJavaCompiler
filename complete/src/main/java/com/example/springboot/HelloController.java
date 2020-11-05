package com.example.springboot;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.BinaryWire;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final SayHello mw;

    public HelloController() {
        mw = new BinaryWire(Bytes.elasticHeapByteBuffer()).methodWriter(SayHello.class);
        if (!mw.getClass().getCanonicalName().equals("com.example.springboot.SayHelloBinarylightMethodWriter"))
            throw new IllegalStateException(mw.getClass().getCanonicalName());
    }

    @RequestMapping("/")
    public String index() {
        mw.hello("you");
        return "Greetings from Spring Boot!";
    }

}
