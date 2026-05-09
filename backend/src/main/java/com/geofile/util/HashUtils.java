package com.geofile.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class HashUtils {
    public static String getMd5(InputStream is) throws IOException {
        return DigestUtils.md5Hex(is);
    }
    public static String getSha256(InputStream is) throws IOException {
        return DigestUtils.sha256Hex(is);
    }
}
