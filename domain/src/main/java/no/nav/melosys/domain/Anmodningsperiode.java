package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.Objects;
import javax.persistence.*;

import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

import static java.util.Objects.isNull;

@Entity
@Table(name = "anmodningsperiode")
public class Anmodningsperiode implements PeriodeOmLovvalg {

    public static final String FEIL_VED_HENT_BEHANDLINGSRESULTAT_ID = "Anmodningsperiode skal alltid ha " +
        "behandlingsresultat. Kunne ikke hente beh_resultat_id på anmodningsperiode id %s";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false, updatable = false)
    private Behandlingsresultat behandlingsresultat;

    @Column(name = "fom_dato", nullable = false, updatable = false)
    private LocalDate fom;

    @Column(name = "tom_dato", updatable = false)
    private LocalDate tom;

    @Enumerated(EnumType.STRING)
    @Column(name = "lovvalgsland", updatable = false)
    private Land_iso2 lovvalgsland;

    @Column(name = "lovvalg_bestemmelse", updatable = false)
    @Convert(converter = LovvalgBestemmelsekonverterer.class)
    private LovvalgBestemmelse bestemmelse;

    @Column(name = "tillegg_bestemmelse", updatable = false)
    @Convert(converter = LovvalgBestemmelsekonverterer.class)
    private LovvalgBestemmelse tilleggsbestemmelse;

    @Enumerated(EnumType.STRING)
    @Column(name = "unntak_fra_lovvalgsland", updatable = false)
    private Land_iso2 unntakFraLovvalgsland;

    @Column(name = "unntak_fra_bestemmelse", updatable = false)
    @Convert(converter = LovvalgBestemmelsekonverterer.class)
    private LovvalgBestemmelse unntakFraBestemmelse;

    @Enumerated(EnumType.STRING)
    @Column(name = "trygde_dekning")
    private Trygdedekninger dekning;

    @Column(name = "medlperiode_id")
    private Long medlPeriodeID;

    @Column(name = "sendt_utland")
    private boolean sendtUtland;

    @Column(name = "anmodet_av")
    private String anmodetAv;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "anmodningsperiode")
    private AnmodningsperiodeSvar anmodningsperiodeSvar;

    public Anmodningsperiode() {
    }

    public Anmodningsperiode(LocalDate fom, LocalDate tom, Land_iso2 lovvalgsland, LovvalgBestemmelse bestemmelse, LovvalgBestemmelse tilleggsbestemmelse,
                             Land_iso2 unntakFraLovvalgsland, LovvalgBestemmelse unntakFraBestemmelse, Trygdedekninger dekning) {
        this.fom = fom;
        this.tom = tom;
        this.lovvalgsland = lovvalgsland;
        this.bestemmelse = bestemmelse;
        this.tilleggsbestemmelse = tilleggsbestemmelse;
        this.unntakFraLovvalgsland = unntakFraLovvalgsland;
        this.unntakFraBestemmelse = unntakFraBestemmelse;
        this.dekning = dekning;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Behandlingsresultat getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public void setBehandlingsresultat(Behandlingsresultat behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    @Override
    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    @Override
    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public Land_iso2 getLovvalgsland() {
        return lovvalgsland;
    }

    public void setLovvalgsland(Land_iso2 lovvalgsland) {
        this.lovvalgsland = lovvalgsland;
    }

    public LovvalgBestemmelse getBestemmelse() {
        return bestemmelse;
    }

    public void setBestemmelse(LovvalgBestemmelse bestemmelse) {
        this.bestemmelse = bestemmelse;
    }

    public LovvalgBestemmelse getTilleggsbestemmelse() {
        return tilleggsbestemmelse;
    }

    public void setTilleggsbestemmelse(LovvalgBestemmelse tilleggsbestemmelse) {
        this.tilleggsbestemmelse = tilleggsbestemmelse;
    }

    public Land_iso2 getUnntakFraLovvalgsland() {
        return unntakFraLovvalgsland;
    }

    public void setUnntakFraLovvalgsland(Land_iso2 unntakFraLovvalgsland) {
        this.unntakFraLovvalgsland = unntakFraLovvalgsland;
    }

    public LovvalgBestemmelse getUnntakFraBestemmelse() {
        return unntakFraBestemmelse;
    }

    public void setUnntakFraBestemmelse(LovvalgBestemmelse unntakFraBestemmelse) {
        this.unntakFraBestemmelse = unntakFraBestemmelse;
    }

    public Trygdedekninger getDekning() {
        return dekning;
    }

    public void setDekning(Trygdedekninger dekning) {
        this.dekning = dekning;
    }

    public Long getMedlPeriodeID() {
        return medlPeriodeID;
    }

    public void setMedlPeriodeID(Long medlPeriodeID) {
        this.medlPeriodeID = medlPeriodeID;
    }

    public boolean erSendtUtland() {
        return sendtUtland;
    }

    public void setSendtUtland(boolean sendtUtland) {
        this.sendtUtland = sendtUtland;
    }

    public AnmodningsperiodeSvar getAnmodningsperiodeSvar() {
        return anmodningsperiodeSvar;
    }

    public void setAnmodningsperiodeSvar(AnmodningsperiodeSvar anmodningsperiodeSvar) {
        this.anmodningsperiodeSvar = anmodningsperiodeSvar;
    }

    public String getAnmodetAv() {
        return anmodetAv;
    }

    public void setAnmodetAv(String anmodetAv) {
        this.anmodetAv = anmodetAv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Anmodningsperiode that = (Anmodningsperiode) o;
        return Objects.equals(behandlingsresultat, that.behandlingsresultat)
            && Objects.equals(fom, that.fom) && Objects.equals(tom, that.tom)
            && lovvalgsland == that.lovvalgsland
            && Objects.equals(bestemmelse, that.bestemmelse)
            && Objects.equals(tilleggsbestemmelse, that.tilleggsbestemmelse)
            && unntakFraLovvalgsland == that.unntakFraLovvalgsland
            && Objects.equals(unntakFraBestemmelse, that.unntakFraBestemmelse)
            && dekning == that.dekning
            && Objects.equals(medlPeriodeID, that.medlPeriodeID)
            && sendtUtland == that.sendtUtland
            && Objects.equals(anmodetAv, that.anmodetAv);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingsresultat, fom, tom, lovvalgsland, bestemmelse, tilleggsbestemmelse,
            unntakFraLovvalgsland, unntakFraBestemmelse, dekning, medlPeriodeID, sendtUtland,
            anmodetAv);
    }

    @Override
    public String toString() {
        return "Anmodningsperiode{" + "id=" + id + ", fom=" + fom + ", tom=" + tom + ", lovvalgsland=" + lovvalgsland
            + ", bestemmelse=" + bestemmelse + ", tilleggsbestemmelse=" + tilleggsbestemmelse
            + ", unntakFraLovvalgsland=" + unntakFraLovvalgsland + ", unntakFraBestemmelse=" + unntakFraBestemmelse
            + ", dekning=" + dekning + ", medlPeriodeID=" + medlPeriodeID + ", sendtUtland=" + sendtUtland
            + ", anmodetAv='" + anmodetAv + '\'' + ", anmodningsperiodeSvar=" + anmodningsperiodeSvar + '}';
    }

    public boolean gjelderSammeLandOgUnntakSom(Anmodningsperiode periode2) {
        return lovvalgsland == periode2.getLovvalgsland() &&
            unntakFraBestemmelse == periode2.getUnntakFraBestemmelse() &&
            unntakFraLovvalgsland == periode2.getUnntakFraLovvalgsland();
    }

    public boolean harRegistrertSvar() {
        return anmodningsperiodeSvar != null && anmodningsperiodeSvar.getAnmodningsperiodeSvarType() != null;
    }

    public Long hentBehandlingsresultatId() {
        if (isNull(getBehandlingsresultat())) {
            throw new IllegalStateException(FEIL_VED_HENT_BEHANDLINGSRESULTAT_ID.formatted(id));
        }
        return getBehandlingsresultat().getId();
    }
}
