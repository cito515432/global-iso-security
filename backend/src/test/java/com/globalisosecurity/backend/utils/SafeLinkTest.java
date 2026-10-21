package com.globalisosecurity.backend.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

class SafeLinkTest {

    @Test
    void acceptsHttpsAndSameApplicationPaths() {
        assertEquals("https://example.com/course", SafeLink.validate(" https://example.com/course ", "link"));
        assertEquals("/files/guide.pdf", SafeLink.validate("/files/guide.pdf", "link"));
        assertNull(SafeLink.validate(" ", "link"));
    }

    @Test
    void rejectsExecutableAndExternalSchemes() {
        assertThrows(BadRequestException.class, () -> SafeLink.validate("javascript:alert(1)", "link"));
        assertThrows(BadRequestException.class, () -> SafeLink.validate("data:text/html,hello", "link"));
        assertThrows(BadRequestException.class, () -> SafeLink.validate("//example.com/path", "link"));
        assertThrows(BadRequestException.class, () -> SafeLink.validate("https://user:pass@example.com", "link"));
        assertThrows(BadRequestException.class, () -> SafeLink.validate("http://example.com", "link"));
    }
}
