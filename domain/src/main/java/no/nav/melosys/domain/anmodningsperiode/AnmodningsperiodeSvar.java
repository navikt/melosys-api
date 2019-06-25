package no.nav.melosys.domain.anmodningsperiode;

import java.time.LocalDate;
import javax.persistence.*;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.kodeverk.AnmodningsperiodeSvarType;

@Entity
@Table(name = "anmodningsperiodeSvar")
public class AnmodningsperiodeSvar {

    @Id
    private Long id;

    @MapsId
    @OneToOne(optional = false, mappedBy = "anmodningsperiode")
    @JoinColumn(name = "anmodningsperiode_id", nullable = false, updatable = false)
    private Anmodningsperiode behandlingsresultat;

    @Column(name = "svar_type")
    private AnmodningsperiodeSvarType anmodningsperiodeSvarType;

    @Column(name = "registrert_dato")
    private LocalDate registrertDato;

    @Column(name = "begrunnelseFritekst")
    private String begrunnelseFritekst;

    @Column(name = "fom_dato")
    private LocalDate fom;

    @Column(name = "tom_dato")
    private LocalDate tom;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Anmodningsperiode getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public void setBehandlingsresultat(Anmodningsperiode behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    public AnmodningsperiodeSvarType getAnmodningsperiodeSvarType() {
        return anmodningsperiodeSvarType;
    }

    public void setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType anmodningsperiodeSvarType) {
        this.anmodningsperiodeSvarType = anmodningsperiodeSvarType;
    }

    public LocalDate getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(LocalDate registrertDato) {
        this.registrertDato = registrertDato;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public void setBegrunnelseFritekst(String begrunnelseFritekst) {
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }
}
