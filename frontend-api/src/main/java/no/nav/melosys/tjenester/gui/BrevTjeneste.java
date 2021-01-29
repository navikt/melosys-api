package no.nav.melosys.tjenester.gui;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.tjenester.gui.dto.brev.BrevmalDto;
import no.nav.melosys.tjenester.gui.dto.brev.BrevmalFeltDto;
import no.nav.melosys.tjenester.gui.dto.brev.FeltvalgDto;
import no.nav.melosys.tjenester.gui.dto.brev.MottakerDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import static java.util.Arrays.asList;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;

@Protected
@RestController
@RequestMapping("/brev")
@Api(tags = {"brev"})
@RequestScope
public class BrevTjeneste {

    @GetMapping("/tilgjengelige-maler")
    @ApiOperation(value = "Henter alle tilgjengelige brevmaler", response = BrevmalDto.class, responseContainer = "List")
    public List<BrevmalDto> hentTilgjengeligeMaler() {
        BrevmalDto forvaltningsmeldingSoknad = new BrevmalDto.Builder()
            .medType(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBeskrivelse(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD.getBeskrivelse())
            .build();

        BrevmalDto mangelbrev = new BrevmalDto.Builder()
            .medType(MANGELBREV_BRUKER)
            .medBeskrivelse(MANGELBREV_BRUKER.getBeskrivelse())
            .medFelter(asList(
                new BrevmalFeltDto.Builder()
                    .medKode("INNLEDNING_FRITEKST")
                    .medBeskrivelse("")
                    .medValg(
                        new FeltvalgDto.Builder()
                            .medInputType("TEXTAREA")
                            .medKode("FRITEKST")
                            .medBeskrivelse("Fritekst til innledning")
                            .build()
                    )
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKode("MANGLER_FRITEKST")
                    .medBeskrivelse("")
                    .medValg(
                        new FeltvalgDto.Builder()
                            .medInputType("TEXTAREA")
                            .medKode("FRITEKST")
                            .medBeskrivelse("Fritekst om manglende dokumentasjon")
                            .erPåkrevd()
                            .build()
                    )
                    .build()
                )
            )
            .medMuligeMottakere(asList(
                new MottakerDto.Builder()
                    .medType("Bruker eller brukers fullmektig")
                    .medRolle(Aktoersroller.BRUKER)
                    .medKanOverstyres(false)
                    .build(),
                new MottakerDto.Builder()
                    .medType("Arbeidsgiver eller arbeidsgivers fullmektig")
                    .medRolle(Aktoersroller.ARBEIDSGIVER)
                    .medKanOverstyres(false)
                    .build()
            ))
            .build();


        return asList(forvaltningsmeldingSoknad, mangelbrev);
    }
}
