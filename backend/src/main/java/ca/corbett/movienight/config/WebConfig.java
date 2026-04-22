package ca.corbett.movienight.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        boolean hasResourceRegion = converters.stream()
                .anyMatch(c -> c instanceof ResourceRegionHttpMessageConverter);

        if (!hasResourceRegion) {
            converters.add(new ResourceRegionHttpMessageConverter());
        }
    }
}
