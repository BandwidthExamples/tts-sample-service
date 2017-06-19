package com.bandwidth.tts;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class Text2SpeechSampleApi {
    public static void main(final String[] args) {
        final Properties properties = new Properties();
        properties.setProperty("modificationTime", new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date()));

        new SpringApplicationBuilder()
            .sources(Text2SpeechSampleApi.class)
            .properties(properties)
            .bannerMode(Banner.Mode.OFF)
            .web(true)
            .run(args);
    }
}
