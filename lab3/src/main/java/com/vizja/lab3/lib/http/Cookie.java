package com.vizja.lab3.lib.http;

import lombok.Getter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;


@Getter
public final class Cookie {
    private final String name;
    private final String value;

    private ZonedDateTime expires;
    private int maxAge = -1; // -1 means not set
    private String domain;
    private String path;
    private boolean secure;
    private boolean httpOnly;
    private SameSite sameSite;

    public enum SameSite {Strict, Lax, None}

    public Cookie(String name, String value) {
        Objects.requireNonNull(name, "Cookie name cannot be null");
        Objects.requireNonNull(value, "Cookie value cannot be null");
        this.name = name;
        this.value = value;
    }

    public Cookie setExpires(ZonedDateTime expires) {
        this.expires = expires;
        return this;
    }

    public Cookie setMaxAge(int maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    public Cookie setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public Cookie setPath(String path) {
        this.path = path;
        return this;
    }

    public Cookie setSecure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public Cookie setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    public Cookie setSameSite(SameSite sameSite) {
        this.sameSite = sameSite;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value);

        if (expires != null) {
            sb.append("; Expires=").append(DateTimeFormatter.RFC_1123_DATE_TIME.format(expires));
        }
        if (maxAge >= 0) {
            sb.append("; Max-Age=").append(maxAge);
        }
        if (domain != null && !domain.isEmpty()) {
            sb.append("; Domain=").append(domain);
        }
        if (path != null && !path.isEmpty()) {
            sb.append("; Path=").append(path);
        }
        if (sameSite != null) {
            sb.append("; SameSite=").append(sameSite.name());
        }
        if (secure) {
            sb.append("; Secure");
        }
        if (httpOnly) {
            sb.append("; HttpOnly");
        }

        return sb.toString();
    }
}
