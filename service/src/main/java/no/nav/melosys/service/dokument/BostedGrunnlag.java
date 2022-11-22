package no.nav.melosys.service.dokument;

import java.util.Optional;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.util.MottatteOpplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BostedGrunnlag {
    private static final Logger log = LoggerFactory.getLogger(BostedGrunnlag.class);
    private final MottatteOpplysningerData grunnlagData;
    private final Bostedsadresse bostedsadresseFraRegister;
    private final KodeverkService kodeverkService;


    public BostedGrunnlag(MottatteOpplysningerData grunnlagData,
                          Bostedsadresse bostedsadresseFraRegister,
                          KodeverkService kodeverkService) {
        this.grunnlagData = grunnlagData;
        this.bostedsadresseFraRegister = bostedsadresseFraRegister;
        this.kodeverkService = kodeverkService;
    }

    public StrukturertAdresse hentBostedsadresse() {
        return finnBostedsadresse().orElseThrow(() ->
            new FunksjonellException("Bostedsadressen finnes ikke eller mangler landkode"));
    }

    public Optional<StrukturertAdresse> finnBostedsadresse() {
        return finnOppgittBostedsadresse().or(this::finnBostedsadresseFraRegister);
    }

    private Optional<StrukturertAdresse> finnOppgittBostedsadresse() {
        return grunnlagData != null ? Optional.ofNullable(MottatteOpplysningerUtils.hentBostedsadresse(grunnlagData)) :
            Optional.empty();
    }

    private Optional<StrukturertAdresse> finnBostedsadresseFraRegister() {
        if (bostedsadresseFraRegister == null) {
            log.warn("Fant ikke bostedsaddresse fra register, fordi bostedsadresseFraRegister er null");
            return Optional.empty();
        }

        if (StringUtils.isEmpty(bostedsadresseFraRegister.strukturertAdresse().getLandkode())) {
            log.info("Fant ikke Landkode i strukturertAdresse");
            return Optional.empty();
        }

        final var strukturertAdresse = bostedsadresseFraRegister.strukturertAdresse();
        if (StringUtils.isEmpty(strukturertAdresse.getPoststed()) && strukturertAdresse.getPostnummer() != null) {
            strukturertAdresse.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, strukturertAdresse.getPostnummer()));
        }

        return Optional.of(strukturertAdresse);
    }
}
