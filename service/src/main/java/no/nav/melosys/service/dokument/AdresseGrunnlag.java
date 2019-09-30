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

public class AdresseGrunnlag {
    private final KodeverkService kodeverkService;
    private final SoeknadDokument søknad;
    private final PersonDokument person;

    public AdresseGrunnlag(SoeknadDokument søknad, PersonDokument person, KodeverkService kodeverkService) {
        this.kodeverkService = kodeverkService;
        this.søknad = søknad;
        this.person = person;
    }

    public Optional<StrukturertAdresse> finnAdresse() throws TekniskException {
        StrukturertAdresse adresse = hentBostedsadresse();

        if (adresse == null) {
            adresse = hentPostadresse();
        }

        return Optional.ofNullable(adresse);
    }

    private StrukturertAdresse hentPostadresse() throws TekniskException {
        UstrukturertAdresse ustrukturertAdresse = person.postadresse;

        if (ustrukturertAdresse.erTom()) {
            return null;
        }

        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.gatenavn = ustrukturertAdresse.adresselinje1;
        strukturertAdresse.poststed = ustrukturertAdresse.adresselinje2;
        strukturertAdresse.postnummer = ustrukturertAdresse.adresselinje3;
        strukturertAdresse.region = ustrukturertAdresse.adresselinje4;
        strukturertAdresse.landkode = LandkoderUtils.tilIso2(ustrukturertAdresse.land.getKode()).getKode();

        return strukturertAdresse;
    }

    private StrukturertAdresse hentBostedsadresse() throws TekniskException {
        StrukturertAdresse bostedsadresse = søknad != null ? SoeknadUtils.hentBostedsadresse(søknad) : null;
        if (bostedsadresse == null) {
            return hentBostedsadresseFraRegister();
        }
        return bostedsadresse;
    }

    private StrukturertAdresse hentBostedsadresseFraRegister() throws TekniskException {
        Bostedsadresse bostedsadresse = person.bostedsadresse;
        if (StringUtils.isEmpty(bostedsadresse.getLand().getKode())) {
            return null;
        }
        bostedsadresse.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, bostedsadresse.getPostnr(), LocalDate.now()));
        return StrukturertAdresse.av(bostedsadresse);
    }
}
