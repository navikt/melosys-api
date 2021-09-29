package no.nav.melosys.tjenester.gui.medlemskapsperiode;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService;
import no.nav.melosys.service.medlemskapsperiode.OpprettMedlemskapsperiodeService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import no.nav.melosys.tjenester.gui.dto.MedlemskapsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.MedlemskapsperiodeOppdatering;
import no.nav.melosys.tjenester.gui.dto.UtledMedlemskapsperiodeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedlemskapsperiodeTjenesteTest extends JsonSchemaTestParent {

    private static final String MEDLEMSKAPSPERIODER_SCHEMA = "medlemskapsperioder-schema.json";
    private static final String MEDLEMSKAPSPERIODER_POST_SCHEMA = "medlemskapsperioder-post-schema.json";
    private static final String MEDLEMSKAPSPERIODER_PUT_SCHEMA = "medlemskapsperioder-put-schema.json";
    private static final String MEDLEMSKAPSPERIODER_BESTEMMELSE_SCHEMA = "medlemskapsperioder-bestemmelse-schema.json";
    private static final String MEDLEMSKAPSPERIODER_BESTEMMELSE_POST_SCHEMA = "medlemskapsperioder-bestemmelse-post-schema.json";

    @Mock
    private MedlemskapsperiodeService medlemskapsperiodeService;
    @Mock
    private Aksesskontroll aksesskontroll;
    @Mock
    private OpprettMedlemskapsperiodeService opprettMedlemskapsperiodeService;

    private MedlemskapsperiodeTjeneste medlemskapsperiodeTjeneste;

    private final long behandlingID = 1231;

    @BeforeEach
    void setup() {
        medlemskapsperiodeTjeneste = new MedlemskapsperiodeTjeneste(medlemskapsperiodeService, opprettMedlemskapsperiodeService, aksesskontroll);
    }

    @Test
    void hentMedlemskapsperioder_validerSchema() throws IOException {
        final var medlemskapsperiode = lagMedlemskapsperiode();
        when(medlemskapsperiodeService.hentMedlemskapsperioder(eq(behandlingID)))
            .thenReturn(Collections.singleton(medlemskapsperiode));

        var res = medlemskapsperiodeTjeneste.hentMedlemskapsperioder(behandlingID);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(res.getBody()).hasSize(1)
            .flatExtracting(
                MedlemskapsperiodeDto::id, MedlemskapsperiodeDto::arbeidsland, MedlemskapsperiodeDto::bestemmelse,
                MedlemskapsperiodeDto::fomDato, MedlemskapsperiodeDto::tomDato, MedlemskapsperiodeDto::trygdedekning,
                MedlemskapsperiodeDto::innvilgelsesResultat, MedlemskapsperiodeDto::medlemskapstype)
            .containsExactly(
                medlemskapsperiode.getId(), medlemskapsperiode.getArbeidsland(), medlemskapsperiode.getBestemmelse(),
                medlemskapsperiode.getFom(), medlemskapsperiode.getTom(), medlemskapsperiode.getDekning(),
                medlemskapsperiode.getInnvilgelsesresultat(), medlemskapsperiode.getMedlemskapstype()
            );

        validerArray(res.getBody(), MEDLEMSKAPSPERIODER_SCHEMA);

    }

    @Test
    void opprettMedlemskapsperioder_validerSchema() throws IOException {
        var opprettMedlemskapsperiodeRequest = new MedlemskapsperiodeOppdatering(LocalDate.now(), LocalDate.now(),
            Trygdedekninger.HELSE_OG_PENSJONSDEL, InnvilgelsesResultat.DELVIS_INNVILGET);

        when(medlemskapsperiodeService.opprettMedlemskapsperiode(eq(behandlingID), any(), any(), any(), any()))
            .thenReturn(lagMedlemskapsperiode());

        valider(opprettMedlemskapsperiodeRequest, MEDLEMSKAPSPERIODER_POST_SCHEMA);
        medlemskapsperiodeTjeneste.opprettMedlemskapsperiode(behandlingID, opprettMedlemskapsperiodeRequest);
    }

    @Test
    void oppdaterMedlemskapsperioder_validerSchema() throws IOException {
        var oppdaterMedlemskapsperiodeRequest = new MedlemskapsperiodeOppdatering(LocalDate.now(), LocalDate.now(),
            Trygdedekninger.HELSE_OG_PENSJONSDEL, InnvilgelsesResultat.DELVIS_INNVILGET);

        when(medlemskapsperiodeService.oppdaterMedlemskapsperiode(eq(behandlingID), anyLong(), any(), any(), any(), any()))
            .thenReturn(lagMedlemskapsperiode());

        valider(oppdaterMedlemskapsperiodeRequest, MEDLEMSKAPSPERIODER_PUT_SCHEMA);
        medlemskapsperiodeTjeneste.oppdaterMedlemskapsperiode(behandlingID, 1, oppdaterMedlemskapsperiodeRequest);
    }

    @Test
    void opprettMedlemskapsperioderFraBestemmelse() throws IOException {

        when(opprettMedlemskapsperiodeService.utledMedlemskapsperioderFraSøknad(eq(behandlingID), any(Folketrygdloven_kap2_bestemmelser.class)))
            .thenReturn(Collections.singleton(lagMedlemskapsperiode()));

        var request = new UtledMedlemskapsperiodeDto(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD);
        valider(request, MEDLEMSKAPSPERIODER_BESTEMMELSE_POST_SCHEMA);
        var res = medlemskapsperiodeTjeneste.opprettMedlemskapsperioderFraBestemmelse(
            behandlingID, request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        validerArray(res.getBody(), MEDLEMSKAPSPERIODER_SCHEMA);
    }

    @Test
    void hentBestemmelserMedVilkår_validerSchema() throws Exception {
        when(opprettMedlemskapsperiodeService.hentBestemmelserMedVilkaar()).thenCallRealMethod();
        when(opprettMedlemskapsperiodeService.hentMuligeBegrunnelser(any(Vilkaar.class))).thenCallRealMethod();
        validerArray(medlemskapsperiodeTjeneste.hentBestemmelserMedVilkaar().getBody(), MEDLEMSKAPSPERIODER_BESTEMMELSE_SCHEMA);
    }

    private Medlemskapsperiode lagMedlemskapsperiode() {
        Medlemskapsperiode medlemskapsperiode = new Medlemskapsperiode();
        medlemskapsperiode.setId(1L);
        medlemskapsperiode.setFom(LocalDate.now());
        medlemskapsperiode.setTom(LocalDate.now().plusYears(1));
        medlemskapsperiode.setArbeidsland("BR");
        medlemskapsperiode.setBestemmelse(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD);
        medlemskapsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.DELVIS_INNVILGET);
        medlemskapsperiode.setMedlemskapstype(Medlemskapstyper.FRIVILLIG);
        medlemskapsperiode.setTrygdedekning(Trygdedekninger.HELSE_OG_PENSJONSDEL);
        return medlemskapsperiode;
    }

}
