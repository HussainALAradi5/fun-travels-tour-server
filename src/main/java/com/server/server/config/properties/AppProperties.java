package com.server.server.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

/** Application settings read from the {@code app.*} properties. */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue("http://localhost:3000") @NotBlank String clientUrl,
        @DefaultValue({ "http://localhost:[*]", "http://127.0.0.1:[*]", "http://10.0.2.2:[*]" })
        @NotEmpty List<String> corsAllowedOrigins,
        @DefaultValue("./uploads") @NotBlank String uploadDir,
        @DefaultValue("2097152") @Positive long maxImageBytes,
        /** Private folder for generated files (reports); never served statically. */
        @DefaultValue("./media") @NotBlank String mediaDir,
        @DefaultValue @Valid Seed seed) {

    /** Development seed: one user per role. Never runs under the prod profile. */
    public record Seed(@DefaultValue("false") boolean enabled, String password) {
    }
}
