package no.nav.melosys.service.dokument;

import java.util.Optional;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.person.Master;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.util.BehandlingsgrunnlagUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.apache.commons.lang3.StringUtils;

public class BostedGrunnlag {
    private final BehandlingsgrunnlagData grunnlagData;
    private final Bostedsadresse bostedsadresseFraRegister;
    private final KodeverkService kodeverkService;


    public BostedGrunnlag(BehandlingsgrunnlagData grunnlagData,
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
        return grunnlagData != null ? Optional.ofNullable(BehandlingsgrunnlagUtils.hentBostedsadresse(grunnlagData)) :
            Optional.empty();
    }

    private Optional<StrukturertAdresse> finnBostedsadresseFraRegister() {
        if (bostedsadresseFraRegister == null
            || StringUtils.isEmpty(bostedsadresseFraRegister.strukturertAdresse().getLandkode())) {
            return Optional.empty();
        }

        final var strukturertAdresse = bostedsadresseFraRegister.strukturertAdresse();
        if (Master.TPS.name().equals(bostedsadresseFraRegister.kilde())) {
            strukturertAdresse.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, strukturertAdresse.getPostnummer()));
        }

        return Optional.of(strukturertAdresse);
    }
}
