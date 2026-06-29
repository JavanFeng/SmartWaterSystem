package com.javan.smart.water.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author FengJ
 * @description
 */
@Data
@ConfigurationProperties(prefix = "water.qa.vector.collection")
public class VectorCreateProps {
    private boolean autoCreate;
}
