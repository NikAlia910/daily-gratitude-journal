package com.mycompany.myapp.service.dto;

import com.mycompany.myapp.domain.enumeration.Mood;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.GratitudeEntry} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GratitudeEntryDTO implements Serializable {

    private Long id;

    @NotNull
    private LocalDate date;

    @Lob
    private String entry;

    private Mood mood;

    @NotNull
    private Instant timestamp;

    private UserDTO user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getEntry() {
        return entry;
    }

    public void setEntry(String entry) {
        this.entry = entry;
    }

    public Mood getMood() {
        return mood;
    }

    public void setMood(Mood mood) {
        this.mood = mood;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GratitudeEntryDTO)) {
            return false;
        }

        GratitudeEntryDTO gratitudeEntryDTO = (GratitudeEntryDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, gratitudeEntryDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GratitudeEntryDTO{" +
            "id=" + getId() +
            ", date='" + getDate() + "'" +
            ", entry='" + getEntry() + "'" +
            ", mood='" + getMood() + "'" +
            ", timestamp='" + getTimestamp() + "'" +
            ", user=" + getUser() +
            "}";
    }
}
