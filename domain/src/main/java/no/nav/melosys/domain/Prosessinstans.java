package no.nav.melosys.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Arbeidstabell for saksflyt.
 */
@Entity
@Table(name = "prosessinstans")
public class Prosessinstans {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "prosess_type", nullable = false, updatable = false)
    @Convert(converter = ProsessType.DbKonverterer.class)
    private ProsessType type;

    @ManyToOne(optional = true)
    @JoinColumn(name = "behandling_id", nullable = true, updatable = true)
    private Behandling behandling;

    @Column(name = "data", nullable = true, updatable = true)
    private String data;

    @Column(name = "steg", nullable = false, updatable = true)
    @Convert(converter = ProsessSteg.DbKonverterer.class)
    private ProsessSteg steg;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private LocalDateTime registrertDato;

    @Column(name = "endret_dato", nullable = false, updatable = true)
    private LocalDateTime endretDato;

    public long getId() {
        return id;
    }

    public ProsessType getType() {
        return type;
    }

    public void setType(ProsessType type) {
        this.type = type;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public ProsessSteg getSteg() {
        return steg;
    }

    public void setSteg(ProsessSteg steg) {
        this.steg = steg;
    }

    public LocalDateTime getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(LocalDateTime registrertDato) {
        this.registrertDato = registrertDato;
    }

    public LocalDateTime getSistEndret() {
        return endretDato;
    }

    public void setSistEndret(LocalDateTime endretDato) {
        this.endretDato = endretDato;
    }

}
