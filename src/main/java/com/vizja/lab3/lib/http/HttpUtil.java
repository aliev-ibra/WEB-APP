package com.vizja.lab3.lib.http;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpUtil {
    // SUPPORTED HTTP VERSIONS
    public static final String SUPPORTED_HTTP_VERSIONS = "HTTP/1.1";

    // CRLF
    static final String CRLF = "\r\n";

    // SUPPORTED HEADERS
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String HEADER_SET_COOKIE = "Set-Cookie";

    // REQUEST HEADERS
    public static final String HEADER_COOKIE = "Cookie";


    // SUPPORTED METHODS
    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_DELETE = "DELETE";


}
