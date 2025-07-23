package com.mycompany.myapp.domain;

import com.mycompany.myapp.domain.enumeration.Mood;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A GratitudeEntry.
 */
@Entity
@Table(
    name = "gratitude_entry",
    uniqueConstraints = { @UniqueConstraint(name = "ux_gratitude_entry_user_date", columnNames = { "user_id", "date" }) }
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GratitudeEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Lob
    @Column(name = "entry", nullable = false)
    private String entry;

    @Enumerated(EnumType.STRING)
    @Column(name = "mood")
    private Mood mood;

    @NotNull
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public GratitudeEntry id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return this.date;
    }

    public GratitudeEntry date(LocalDate date) {
        this.setDate(date);
        return this;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getEntry() {
        return this.entry;
    }

    public GratitudeEntry entry(String entry) {
        this.setEntry(entry);
        return this;
    }

    public void setEntry(String entry) {
        this.entry = entry;
    }

    public Mood getMood() {
        return this.mood;
    }

    public GratitudeEntry mood(Mood mood) {
        this.setMood(mood);
        return this;
    }

    public void setMood(Mood mood) {
        this.mood = mood;
    }

    public Instant getTimestamp() {
        return this.timestamp;
    }

    public GratitudeEntry timestamp(Instant timestamp) {
        this.setTimestamp(timestamp);
        return this;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public GratitudeEntry user(User user) {
        this.setUser(user);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GratitudeEntry)) {
            return false;
        }
        return getId() != null && getId().equals(((GratitudeEntry) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GratitudeEntry{" +
            "id=" + getId() +
            ", date='" + getDate() + "'" +
            ", entry='" + getEntry() + "'" +
            ", mood='" + getMood() + "'" +
            ", timestamp='" + getTimestamp() + "'" +
            "}";
    }
}
