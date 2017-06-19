package com.bandwidth.tts.api.configuration;

import javax.servlet.Filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.bandwidth.tts.CaseInsensitiveRequestFilter;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableAsync
@EnableCaching
@EnableSwagger2
@EnableCircuitBreaker
@EnableWebSecurity
@Configuration
@PropertySource(value = {"application.properties"})
public class Text2SpeechSampleConfig extends WebSecurityConfigurerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(Text2SpeechSampleConfig.class);

    @Bean
    public Docket swaggerDocket() {
        final ApiInfo apiInfo = new ApiInfoBuilder()
            .title("Sample TTS Service API")
            .description("Text to speech sample microservice")
            .license("MIT")
            .licenseUrl("http://opensource.org/licenses/MIT")
            .version("1.0.0")
            .build();

        return new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.bandwidth.tts.api"))
            .build()
            .directModelSubstitute(org.joda.time.LocalDate.class, java.sql.Date.class)
            .directModelSubstitute(org.joda.time.DateTime.class, java.util.Date.class)
            .apiInfo(apiInfo);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().anyRequest().permitAll();
    }

    @Bean
    public WebMvcConfigurerAdapter webMvcConfigurerAdapter() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
                configurer.defaultContentType(MediaType.APPLICATION_JSON);
            }
        };
    }

    @Bean
    public FilterRegistrationBean caseInsensitiveRequestFilterRegistrationBean() {
        return getFilterRegistrationBean(caseInsensitiveRequestFilter(), "caseInsensitiveRequestFilter", 3);
    }

    @Bean(name = "caseInsensitiveRequestFilter")
    public Filter caseInsensitiveRequestFilter() {
        return new CaseInsensitiveRequestFilter();
    }

    private FilterRegistrationBean getFilterRegistrationBean(final Filter filter, final String filterName, final int order) {
        final FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(filter);
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.setName(filterName);
        filterRegistrationBean.setOrder(order);

        return filterRegistrationBean;
    }
}
