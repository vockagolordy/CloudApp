package org.example.cloudapp.util.converter;

import java.util.Locale;
import org.example.cloudapp.entity.AccessLevel;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToAccessLevelConverter implements Converter<String, AccessLevel> {
    @Override
    public AccessLevel convert(String source) {
        if (source == null || source.isBlank()) {
            return AccessLevel.READ;
        }
        return AccessLevel.valueOf(source.trim().toUpperCase(Locale.ROOT));
    }
}
