package com.eodigaljido.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.stereotype.Component;
import org.springframework.validation.SmartValidator;

@Component
@RequiredArgsConstructor
public class StompValidatorConfigurer implements SmartInitializingSingleton {

    private final SimpAnnotationMethodMessageHandler simpAnnotationMethodMessageHandler;
    private final SmartValidator validator;

    @Override
    public void afterSingletonsInstantiated() {
        simpAnnotationMethodMessageHandler.setValidator(validator);
    }
}
