package no.nav.melosys.tjenester.gui.avklartefakta;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.avgift.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Loenn_forhold;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.AvgiftsgrunnlagInfoDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.OppdaterAvgiftsgrunnlagDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.OppdaterBeregningsgrunnlagDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrygdeavgiftTjenesteTest extends JsonSchemaTestParent {

    private static final String TRYGDEAVGIFT_GRUNNLAG_SCHEMA = "trygdeavgift-grunnlag-schema.json";
    private static final String TRYGDEAVGIFT_GRUNNLAG_PUT_SCHEMA = "trygdeavgift-grunnlag-put-schema.json";
    private static final String TRYGDEAVGIFT_BEREGNING_SCHEMA = "trygdeavgift-beregning-schema.json";
    private static final String TRYGDEAVGIFT_BEREGNING_PUT_SCHEMA = "trygdeavgift-beregning-put-schema.json";

    @Mock
    private TrygdeavgiftsberegningService trygdeavgiftsberegningService;
    @Mock
    private TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService;
    @Mock
    private Aksesskontroll aksesskontroll;

    private TrygdeavgiftTjeneste trygdeavgiftTjeneste;

    private final long behandlingsresultatID = 1;

    @BeforeEach
    public void setup() {
        trygdeavgiftTjeneste = new TrygdeavgiftTjeneste(trygdeavgiftsgrunnlagService, trygdeavgiftsberegningService, aksesskontroll);
    }

    @Test
    void oppdaterAvgiftsgrunnlag_validerSchema() throws IOException {
        when(trygdeavgiftsgrunnlagService.oppdaterAvgiftsgrunnlag(eq(behandlingsresultatID), any()))
            .thenReturn(lagTrygdeavgiftsgrunnlag());

        OppdaterAvgiftsgrunnlagDto avgiftsgrunnlagDto = new OppdaterAvgiftsgrunnlagDto(
            Loenn_forhold.DELT_LØNN,
            new AvgiftsgrunnlagInfoDto(true, true, null),
            new AvgiftsgrunnlagInfoDto(true, true, null)
        );

        valider(avgiftsgrunnlagDto, TRYGDEAVGIFT_GRUNNLAG_PUT_SCHEMA);
        valider(trygdeavgiftTjeneste.oppdaterAvgiftsgrunnlag(behandlingsresultatID, avgiftsgrunnlagDto).getBody(), TRYGDEAVGIFT_GRUNNLAG_SCHEMA);
    }

    @Test
    void oppdaterBeregningsgrunnlag_validerSchema() throws IOException {
        when(trygdeavgiftsberegningService.hentBeregningsresultat(eq(behandlingsresultatID)))
            .thenReturn(lagTrygdeavgiftsberegningresultat());

        OppdaterBeregningsgrunnlagDto oppdaterBeregningsgrunnlagDto = new OppdaterBeregningsgrunnlagDto(100L, null);
        valider(oppdaterBeregningsgrunnlagDto, TRYGDEAVGIFT_BEREGNING_PUT_SCHEMA);
        valider(trygdeavgiftTjeneste.oppdaterBeregningsgrunnlag(behandlingsresultatID, oppdaterBeregningsgrunnlagDto).getBody(), TRYGDEAVGIFT_BEREGNING_SCHEMA);
    }

    private Trygdeavgiftsberegningsresultat lagTrygdeavgiftsberegningresultat() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        return new Trygdeavgiftsberegningsresultat(
            100L,
            null,
            aktoer,
            Collections.singleton(new Avgiftsperiode(
                LocalDate.now(), LocalDate.now(), Trygdedekninger.HELSEDEL, new BigDecimal("1.1"), new BigDecimal("1.1"), true)
            ));
    }

    private Trygdeavgiftsgrunnlag lagTrygdeavgiftsgrunnlag() {
        return new Trygdeavgiftsgrunnlag(
            Loenn_forhold.DELT_LØNN,
            new AvgiftsgrunnlagInfoNorge(true, true, null, NORSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV),
            new AvgiftsgrunnlagInfoUtland(true, true, null, UTENLANDSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV)
        );
    }
}
