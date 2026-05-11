package com.andrew.smartielts.admin.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdminServiceImplTest {

    @Test
    void service_shouldBeConstructible() {
        assertNotNull(new AdminServiceImpl());
    }
}
