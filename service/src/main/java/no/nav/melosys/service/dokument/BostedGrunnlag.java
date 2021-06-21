package no.nav.melosys.service.dokument;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.util.BehandlingsgrunnlagUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.apache.commons.lang3.StringUtils;

public class BostedGrunnlag {
    private final KodeverkService kodeverkService;

    private final BehandlingsgrunnlagData grunnlagData;
    private final Persondata persondata;

    public BostedGrunnlag(BehandlingsgrunnlagData grunnlagData,
                          Persondata persondata,
                          KodeverkService kodeverkService) {
        this.grunnlagData = grunnlagData;
        this.persondata = persondata;
        this.kodeverkService = kodeverkService;
    }

    public StrukturertAdresse hentBostedsadresse() {
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
        Bostedsadresse bostedsadresse = persondata.getBostedsadresse();
        if (StringUtils.isEmpty(bostedsadresse.getLand().getKode())) {
            return Optional.empty();
        }
        bostedsadresse.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, bostedsadresse.getPostnr(), LocalDate.now()));
        return Optional.of(bostedsadresse.tilStrukturertAdresse());
    }
}
