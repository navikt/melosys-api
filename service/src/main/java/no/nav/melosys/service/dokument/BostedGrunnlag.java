package no.nav.melosys.service.dokument;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.util.LandkoderUtils;
import no.nav.melosys.domain.util.SoeknadUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.apache.commons.lang3.StringUtils;

public class BostedGrunnlag {
    private final KodeverkService kodeverkService;

    private final SoeknadDokument søknad;
    private final PersonDokument person;

    public BostedGrunnlag(SoeknadDokument søknad,
                          PersonDokument person,
                          KodeverkService kodeverkService) {
        this.søknad = søknad;
        this.person = person;
        this.kodeverkService = kodeverkService;
    }

    public StrukturertAdresse hentBostedsadresse() throws TekniskException {
        return finnBostedsadresseFraDokument().orElseThrow(() ->
            new TekniskException("Bostedsadressen finnes ikke eller mangler landkode"));
    }

    public Optional<StrukturertAdresse> finnAdresse() throws TekniskException {
        Optional<StrukturertAdresse> adresse = finnBostedsadresseFraDokument();
        return adresse.isPresent() ? adresse : finnPostadresse();
    }

    private Optional<StrukturertAdresse> finnBostedsadresseFraDokument() throws TekniskException {
        StrukturertAdresse bostedsadresse = søknad != null ? SoeknadUtils.hentBostedsadresse(søknad) : null;
        if (bostedsadresse == null) {
            return finnBostedsadresseFraRegister();
        }
        return Optional.of(bostedsadresse);
    }

    private Optional<StrukturertAdresse> finnBostedsadresseFraRegister() throws TekniskException {
        Bostedsadresse bostedsadresse = person.bostedsadresse;
        if (StringUtils.isEmpty(bostedsadresse.getLand().getKode())) {
            return Optional.empty();
        }
        bostedsadresse.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, bostedsadresse.getPostnr(), LocalDate.now()));
        return Optional.of(StrukturertAdresse.av(bostedsadresse));
    }

    private Optional<StrukturertAdresse> finnPostadresse() throws TekniskException {
        UstrukturertAdresse ustrukturertAdresse = person.postadresse;

        if (ustrukturertAdresse.erTom()) {
            return Optional.empty();
        }

        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.gatenavn = ustrukturertAdresse.adresselinje1;
        strukturertAdresse.poststed = ustrukturertAdresse.adresselinje2;
        strukturertAdresse.postnummer = ustrukturertAdresse.adresselinje3;
        strukturertAdresse.region = ustrukturertAdresse.adresselinje4;
        strukturertAdresse.landkode = LandkoderUtils.tilIso2(ustrukturertAdresse.land.getKode());

        return Optional.of(strukturertAdresse);
    }
}
