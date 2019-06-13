package com.kakuiwong.server.annotation;

import com.kakuiwong.server.AmaniServer;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(AmaniServer.class)
public @interface EnableAmaniRpcServer {
    String value() default "8888";
}
