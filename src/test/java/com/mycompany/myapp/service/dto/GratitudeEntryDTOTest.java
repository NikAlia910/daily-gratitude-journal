package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GratitudeEntryDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(GratitudeEntryDTO.class);
        GratitudeEntryDTO gratitudeEntryDTO1 = new GratitudeEntryDTO();
        gratitudeEntryDTO1.setId(1L);
        GratitudeEntryDTO gratitudeEntryDTO2 = new GratitudeEntryDTO();
        assertThat(gratitudeEntryDTO1).isNotEqualTo(gratitudeEntryDTO2);
        gratitudeEntryDTO2.setId(gratitudeEntryDTO1.getId());
        assertThat(gratitudeEntryDTO1).isEqualTo(gratitudeEntryDTO2);
        gratitudeEntryDTO2.setId(2L);
        assertThat(gratitudeEntryDTO1).isNotEqualTo(gratitudeEntryDTO2);
        gratitudeEntryDTO1.setId(null);
        assertThat(gratitudeEntryDTO1).isNotEqualTo(gratitudeEntryDTO2);
    }
}
