package no.nav.melosys.service.dokument;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.util.BehandlingsgrunnlagUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.apache.commons.lang3.StringUtils;

public class BostedGrunnlag {
    private final KodeverkService kodeverkService;

    private final BehandlingsgrunnlagData grunnlagData;
    private final PersonDokument person;

    public BostedGrunnlag(BehandlingsgrunnlagData grunnlagData,
                          PersonDokument person,
                          KodeverkService kodeverkService) {
        this.grunnlagData = grunnlagData;
        this.person = person;
        this.kodeverkService = kodeverkService;
    }

    public StrukturertAdresse hentBostedsadresse() throws FunksjonellException {
        return finnBostedsadresse().orElseThrow(() ->
            new FunksjonellException("Bostedsadressen finnes ikke eller mangler landkode"));
    }

    public Optional<StrukturertAdresse> finnBostedsadresse() {
        StrukturertAdresse bostedsadresse = grunnlagData != null ? BehandlingsgrunnlagUtils.hentBostedsadresse(grunnlagData) : null;
        if (bostedsadresse == null) {
            return finnBostedsadresseFraRegister();
        }
        return Optional.of(bostedsadresse);
    }

    private Optional<StrukturertAdresse> finnBostedsadresseFraRegister() {
        Bostedsadresse bostedsadresse = person.bostedsadresse;
        if (StringUtils.isEmpty(bostedsadresse.getLand().getKode())) {
            return Optional.empty();
        }
        bostedsadresse.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, bostedsadresse.getPostnr(), LocalDate.now()));
        return Optional.of(StrukturertAdresse.av(bostedsadresse));
    }
}
