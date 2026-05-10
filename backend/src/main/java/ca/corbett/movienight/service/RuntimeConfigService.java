package ca.corbett.movienight.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class RuntimeConfigService {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeConfigService.class);

    private final AtomicBoolean fullyLocal;

    public RuntimeConfigService(@Value("${movienight.fully_local:false}") boolean fullyLocalDefault) {
        this.fullyLocal = new AtomicBoolean(fullyLocalDefault);
        logger.info("Runtime config initialized: movienight.fully_local={}", fullyLocalDefault);
    }

    public boolean isFullyLocal() {
        return fullyLocal.get();
    }

    public boolean setFullyLocal(boolean newValue) {
        boolean previousValue = fullyLocal.getAndSet(newValue);
        if (previousValue != newValue) {
            logger.info("Runtime config updated: movienight.fully_local {} -> {}", previousValue, newValue);
        } else {
            logger.debug("Runtime config unchanged: movienight.fully_local remains {}", newValue);
        }
        return newValue;
    }
}

