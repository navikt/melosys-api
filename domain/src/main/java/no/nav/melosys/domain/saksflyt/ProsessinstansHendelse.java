package no.nav.melosys.domain.saksflyt;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.*;


@Entity
@Table(name = "prosessinstans_hendelser")
public class ProsessinstansHendelse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "prosessinstans_id", nullable = false, updatable = false)
    private Prosessinstans prosessinstans;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private LocalDateTime dato;

    @Enumerated(EnumType.STRING)
    @Column(name = "steg", nullable = false, updatable = false)
    private ProsessSteg steg;

    @Column(name = "type", updatable = false)
    private String type;

    @Column(name = "melding", nullable = false, updatable = false)
    private String melding;

    public ProsessinstansHendelse() {
    }

    public ProsessinstansHendelse(Prosessinstans prosessinstans, LocalDateTime dato, ProsessSteg steg, String type, String melding) {
        this.prosessinstans = prosessinstans;
        this.dato = dato;
        this.steg = steg;
        this.type = type;
        this.melding = melding;
    }

    public Long getId() {
        return id;
    }

    public Prosessinstans getProsessinstans() {
        return prosessinstans;
    }

    public LocalDateTime getDato() {
        return dato;
    }

    public ProsessSteg getSteg() {
        return steg;
    }

    public String getType() {
        return type;
    }

    public String getMelding() {
        return melding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProsessinstansHendelse hendelse = (ProsessinstansHendelse) o;
        return Objects.equals(prosessinstans, hendelse.prosessinstans) &&
            dato.equals(hendelse.dato) &&
            steg == hendelse.steg &&
            Objects.equals(type, hendelse.type) &&
            Objects.equals(melding, hendelse.melding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prosessinstans, dato, steg, type, melding);
    }

    @Override
    public String toString() {
        return "ProsessinstansHendelse{" +
            "id=" + id +
            ", dato=" + dato +
            ", steg=" + steg +
            ", type='" + type + '\'' +
            ", melding='" + melding + '\'' +
            '}';
    }
}
