package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.GratitudeEntryTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GratitudeEntryTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(GratitudeEntry.class);
        GratitudeEntry gratitudeEntry1 = getGratitudeEntrySample1();
        GratitudeEntry gratitudeEntry2 = new GratitudeEntry();
        assertThat(gratitudeEntry1).isNotEqualTo(gratitudeEntry2);

        gratitudeEntry2.setId(gratitudeEntry1.getId());
        assertThat(gratitudeEntry1).isEqualTo(gratitudeEntry2);

        gratitudeEntry2 = getGratitudeEntrySample2();
        assertThat(gratitudeEntry1).isNotEqualTo(gratitudeEntry2);
    }
}
