package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.GratitudeEntryAsserts.*;
import static com.mycompany.myapp.domain.GratitudeEntryTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GratitudeEntryMapperTest {

    private GratitudeEntryMapper gratitudeEntryMapper;

    @BeforeEach
    void setUp() {
        gratitudeEntryMapper = new GratitudeEntryMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getGratitudeEntrySample1();
        var actual = gratitudeEntryMapper.toEntity(gratitudeEntryMapper.toDto(expected));
        assertGratitudeEntryAllPropertiesEquals(expected, actual);
    }
}
