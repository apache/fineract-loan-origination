package org.apache.fineract.los.config;

import org.apache.fineract.los.bridge.FineractClientProperties;
import org.apache.fineract.los.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Enables binding of configuration properties. */
@Configuration
@EnableConfigurationProperties({FineractClientProperties.class, JwtProperties.class})
public class FineractClientConfig {}
