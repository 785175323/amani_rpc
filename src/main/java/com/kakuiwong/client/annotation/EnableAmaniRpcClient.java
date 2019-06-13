package com.kakuiwong.client.annotation;

import com.kakuiwong.client.AmaniClient;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(AmaniClient.class)
public @interface EnableAmaniRpcClient {
    String basePackage() default "";
}
