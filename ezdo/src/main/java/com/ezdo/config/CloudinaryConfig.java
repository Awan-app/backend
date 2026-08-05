package com.ezdo.config;

import com.cloudinary.Cloudinary;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties props) {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", props.cloudName());
        config.put("api_key", props.apiKey());
        config.put("api_secret", props.apiSecret());
        config.put("secure", true);
        return new Cloudinary(config);
    }
}
