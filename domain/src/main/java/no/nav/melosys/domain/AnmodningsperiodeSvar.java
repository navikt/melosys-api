package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.Objects;
import javax.persistence.*;

import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;

@Entity
@Table(name = "anmodningsperiode_svar")
public class AnmodningsperiodeSvar {

    //mappes av hibernate til anmodningsperiode.id
    @Id
    private Long id;

    @MapsId
    @OneToOne(optional = false)
    private Anmodningsperiode anmodningsperiode;

    @Enumerated(EnumType.STRING)
    @Column(name = "svar_type")
    private Anmodningsperiodesvartyper anmodningsperiodeSvarType;

    @Column(name = "registrert_dato")
    private LocalDate registrertDato;

    @Column(name = "begrunnelseFritekst")
    private String begrunnelseFritekst;

    @Column(name = "innvilget_fom_dato")
    private LocalDate innvilgetFom;

    @Column(name = "innvilget_tom_dato")
    private LocalDate innvilgetTom;

    @SuppressWarnings("unused")
    public AnmodningsperiodeSvar() {
    }

    public AnmodningsperiodeSvar(Anmodningsperiode anmodningsperiode, Anmodningsperiodesvartyper anmodningsperiodeSvarType, LocalDate registrertDato, String begrunnelseFritekst, LocalDate innvilgetFom, LocalDate innvilgetTom) {
        this.anmodningsperiode = anmodningsperiode;
        this.anmodningsperiodeSvarType = anmodningsperiodeSvarType;
        this.registrertDato = registrertDato;
        this.begrunnelseFritekst = begrunnelseFritekst;
        this.innvilgetFom = innvilgetFom;
        this.innvilgetTom = innvilgetTom;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Anmodningsperiode getAnmodningsperiode() {
        return anmodningsperiode;
    }

    public void setAnmodningsperiode(Anmodningsperiode anmodningsperiode) {
        this.anmodningsperiode = anmodningsperiode;
    }

    public Anmodningsperiodesvartyper getAnmodningsperiodeSvarType() {
        return anmodningsperiodeSvarType;
    }

    public void setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper anmodningsperiodeSvarType) {
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

    public LocalDate getInnvilgetFom() {
        return innvilgetFom;
    }

    public void setInnvilgetFom(LocalDate innvilgetFom) {
        this.innvilgetFom = innvilgetFom;
    }

    public LocalDate getInnvilgetTom() {
        return innvilgetTom;
    }

    public void setInnvilgetTom(LocalDate innvilgetTom) {
        this.innvilgetTom = innvilgetTom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnmodningsperiodeSvar that = (AnmodningsperiodeSvar) o;
        return Objects.equals(id, that.id) &&
            anmodningsperiodeSvarType == that.anmodningsperiodeSvarType &&
            Objects.equals(registrertDato, that.registrertDato) &&
            Objects.equals(begrunnelseFritekst, that.begrunnelseFritekst) &&
            Objects.equals(innvilgetFom, that.innvilgetFom) &&
            Objects.equals(innvilgetTom, that.innvilgetTom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, registrertDato, begrunnelseFritekst, innvilgetFom, innvilgetTom);
    }

    @Override
    public String toString() {
        return "AnmodningsperiodeSvar{" +
            "id=" + id +
            ", anmodningsperiode=" + anmodningsperiode +
            ", anmodningsperiodeSvarType=" + anmodningsperiodeSvarType +
            ", registrertDato=" + registrertDato +
            ", begrunnelseFritekst='" + begrunnelseFritekst + '\'' +
            ", innvilgetFom=" + innvilgetFom +
            ", innvilgetTom=" + innvilgetTom +
            '}';
    }

    public boolean erGyldigDelvisInnvilgelse() {
        return Anmodningsperiodesvartyper.DELVIS_INNVILGELSE == getAnmodningsperiodeSvarType()
            && getInnvilgetFom() != null && getInnvilgetTom() != null;
    }

    public boolean erAvslag() {
        return anmodningsperiodeSvarType == Anmodningsperiodesvartyper.AVSLAG;
    }

    public boolean erInnvilgelse() {
        return anmodningsperiodeSvarType == Anmodningsperiodesvartyper.INNVILGELSE;
    }
}
