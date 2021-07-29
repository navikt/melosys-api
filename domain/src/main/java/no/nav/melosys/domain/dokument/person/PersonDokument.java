package no.nav.melosys.domain.dokument.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.brev.Postadresse;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.adresse.UstrukturertAdresse;
import no.nav.melosys.domain.person.KjoennType;
import no.nav.melosys.domain.person.Master;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;
import no.nav.melosys.domain.util.LandkoderUtils;


/**
 * Representerer svar fra personregisteret (TPS)
 *
 */
public class PersonDokument implements Persondata, SaksopplysningDokument {
    private String fnr;
    private Sivilstand sivilstand;
    private LocalDate sivilstandGyldighetsperiodeFom;
    private Land statsborgerskap;
    @JsonProperty("kjoenn")
    private KjoennsType kjønn;
    private String fornavn;
    private String mellomnavn;
    private String etternavn;
    private String sammensattNavn;
    private List<Familiemedlem> familiemedlemmer = new ArrayList<>();
    @JsonProperty("foedselsdato")
    private LocalDate fødselsdato;
    private LocalDate dødsdato;
    private Diskresjonskode diskresjonskode;
    private Personstatus personstatus;
    private LocalDate statsborgerskapDato;
    private Bostedsadresse bostedsadresse = new Bostedsadresse();
    private UstrukturertAdresse postadresse = new UstrukturertAdresse();
    private MidlertidigPostadresse midlertidigPostadresse = new MidlertidigPostadresse();
    private UstrukturertAdresse gjeldendePostadresse = new UstrukturertAdresse();
    @JsonProperty(defaultValue = "false" )
    private boolean erEgenAnsatt;

    @Override
    public boolean erPersonDød() {
        return dødsdato != null;
    }

    @Override
    public boolean harIkkeRegistrertAdresse() {
        return bostedsadresse.erTom() &&
            postadresse.erTom() &&
            midlertidigPostadresse.land == null;
    }

    @Override
    public boolean manglerBostedsadresse() {
        return bostedsadresse.erTom();
    }

    @Override
    public Optional<Familiemedlem> hentAnnenForelder(String fnrGjeldendeForelder) {
        return familiemedlemmer.stream()
            .filter(Familiemedlem::erForelder)
            .filter(forelder -> !fnrGjeldendeForelder.equals(forelder.fnr))
            .findAny();
    }

    @Override
    public Optional<no.nav.melosys.domain.person.adresse.Bostedsadresse> finnBostedsadresse() {
        if (bostedsadresse == null || bostedsadresse.erTom()) {
            return Optional.empty();
        }
        return Optional.of(
            new no.nav.melosys.domain.person.adresse.Bostedsadresse(bostedsadresse.tilStrukturertAdresse(), null, null,
                null, Master.TPS.name(), Master.TPS.name(), false));
    }

    @Override
    public Optional<Kontaktadresse> finnKontaktadresse() {
        return Optional.empty();
    }

    @Override
    public Optional<Oppholdsadresse> finnOppholdsadresse() {
        return Optional.empty();
    }

    @Override
    public boolean harStrengtAdressebeskyttelse() {
        return diskresjonskode != null && diskresjonskode.erKode6();
    }

    @Override
    public Set<Land> hentAlleStatsborgerskap() {
        return Set.of(statsborgerskap);
    }

    @Override
    public KjoennType hentKjønnType() {
        return KjoennType.avKode(kjønn.getKode());
    }

    @Override
    public String hentFolkeregisterIdent() {
        return fnr;
    }

    public String getFnr() {
        return fnr;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public Sivilstand getSivilstand() {
        return sivilstand;
    }

    public void setSivilstand(Sivilstand sivilstand) {
        this.sivilstand = sivilstand;
    }

    public LocalDate getSivilstandGyldighetsperiodeFom() {
        return sivilstandGyldighetsperiodeFom;
    }

    public void setSivilstandGyldighetsperiodeFom(LocalDate sivilstandGyldighetsperiodeFom) {
        this.sivilstandGyldighetsperiodeFom = sivilstandGyldighetsperiodeFom;
    }

    /** Kodeverk: Landkoder */
    public Land getStatsborgerskap() {
        return statsborgerskap;
    }

    public void setStatsborgerskap(Land statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }

    /** Kodeverk: Kjønnstyper */
    public KjoennsType getKjønn() {
        return kjønn;
    }

    public void setKjønn(KjoennsType kjønn) {
        this.kjønn = kjønn;
    }

    @Override
    public String getFornavn() {
        return fornavn;
    }

    public void setFornavn(String fornavn) {
        this.fornavn = fornavn;
    }

    @Override
    public String getMellomnavn() {
        return mellomnavn;
    }

    public void setMellomnavn(String mellomnavn) {
        this.mellomnavn = mellomnavn;
    }

    @Override
    public String getEtternavn() {
        return etternavn;
    }

    public void setEtternavn(String etternavn) {
        this.etternavn = etternavn;
    }

    @Override
    public String getSammensattNavn() {
        return sammensattNavn;
    }

    public void setSammensattNavn(String sammensattNavn) {
        this.sammensattNavn = sammensattNavn;
    }

    @Override
    public List<Familiemedlem> getFamiliemedlemmer() {
        return familiemedlemmer;
    }

    public void setFamiliemedlemmer(List<Familiemedlem> familiemedlemmer) {
        this.familiemedlemmer = familiemedlemmer;
    }

    @Override
    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public void setFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public void setDødsdato(LocalDate dødsdato) {
        this.dødsdato = dødsdato;
    }

    public Diskresjonskode getDiskresjonskode() {
        return diskresjonskode;
    }

    public void setDiskresjonskode(Diskresjonskode diskresjonskode) {
        this.diskresjonskode = diskresjonskode;
    }

    public Personstatus getPersonstatus() {
        return personstatus;
    }

    public void setPersonstatus(Personstatus personstatus) {
        this.personstatus = personstatus;
    }

    public LocalDate getStatsborgerskapDato() {
        return statsborgerskapDato;
    }

    public void setStatsborgerskapDato(LocalDate statsborgerskapDato) {
        this.statsborgerskapDato = statsborgerskapDato;
    }

    @Override
    public Bostedsadresse getBostedsadresse() {
        return bostedsadresse;
    }

    public void setBostedsadresse(Bostedsadresse bostedsadresse) {
        this.bostedsadresse = bostedsadresse;
    }

    public UstrukturertAdresse getPostadresse() {
        return postadresse;
    }

    public void setPostadresse(UstrukturertAdresse postadresse) {
        this.postadresse = postadresse;
    }

    public MidlertidigPostadresse getMidlertidigPostadresse() {
        return midlertidigPostadresse;
    }

    public void setMidlertidigPostadresse(MidlertidigPostadresse midlertidigPostadresse) {
        this.midlertidigPostadresse = midlertidigPostadresse;
    }

    public UstrukturertAdresse getGjeldendePostadresse() {
        return gjeldendePostadresse;
    }

    @Override
    public Postadresse hentGjeldendePostadresse() {
        return new Postadresse(
            gjeldendePostadresse.adresselinje1,
            gjeldendePostadresse.adresselinje2,
            gjeldendePostadresse.adresselinje3,
            gjeldendePostadresse.adresselinje4,
            gjeldendePostadresse.postnr,
            gjeldendePostadresse.poststed,
            gjeldendePostadresse.land != null ? LandkoderUtils.tilIso2(gjeldendePostadresse.land.getKode()) : null
        );
    }

    public void setGjeldendePostadresse(UstrukturertAdresse gjeldendePostadresse) {
        this.gjeldendePostadresse = gjeldendePostadresse;
    }

    public void setErEgenAnsatt(boolean erEgenAnsatt) {
        this.erEgenAnsatt = erEgenAnsatt;
    }
}
