package no.nav.melosys.tjenester.gui.medlemskapsperiode;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService;
import no.nav.melosys.service.medlemskapsperiode.OpprettMedlemskapsperiodeService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.medlemskapsperiode.dto.MedlemskapsperiodeDto;
import no.nav.melosys.tjenester.gui.medlemskapsperiode.dto.UtledMedlemskapsperiodeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedlemskapsperiodeTjenesteTest {

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
    void hentMedlemskapsperioder_validerSchema() {
        final var medlemskapsperiode = lagMedlemskapsperiode();
        when(medlemskapsperiodeService.hentMedlemskapsperioder(behandlingID))
            .thenReturn(Collections.singleton(medlemskapsperiode));

        var res = medlemskapsperiodeTjeneste.hentMedlemskapsperioder(behandlingID);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(res.getBody()).hasSize(1)
            .flatExtracting(
                MedlemskapsperiodeDto::id, MedlemskapsperiodeDto::arbeidsland,
                MedlemskapsperiodeDto::fomDato, MedlemskapsperiodeDto::tomDato, MedlemskapsperiodeDto::trygdedekning,
                MedlemskapsperiodeDto::innvilgelsesResultat, MedlemskapsperiodeDto::medlemskapstype)
            .containsExactly(
                medlemskapsperiode.getId(), medlemskapsperiode.getArbeidsland(),
                medlemskapsperiode.getFom(), medlemskapsperiode.getTom(), medlemskapsperiode.getTrygdedekning(),
                medlemskapsperiode.getInnvilgelsesresultat(), medlemskapsperiode.getMedlemskapstype()
            );
    }

    @Test
    void opprettMedlemskapsperioderFraBestemmelse() {

        when(opprettMedlemskapsperiodeService.utledMedlemskapsperioderFraSøknad(eq(behandlingID), any(Folketrygdloven_kap2_bestemmelser.class)))
            .thenReturn(Collections.singleton(lagMedlemskapsperiode()));

        var request = new UtledMedlemskapsperiodeDto(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD);
        var res = medlemskapsperiodeTjeneste.opprettMedlemskapsperioderFraBestemmelse(
            behandlingID, request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private Medlemskapsperiode lagMedlemskapsperiode() {
        Medlemskapsperiode medlemskapsperiode = new Medlemskapsperiode();
        medlemskapsperiode.setId(1L);
        medlemskapsperiode.setFom(LocalDate.now());
        medlemskapsperiode.setTom(LocalDate.now().plusYears(1));
        medlemskapsperiode.setArbeidsland("BR");
        medlemskapsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.DELVIS_INNVILGET);
        medlemskapsperiode.setMedlemskapstype(Medlemskapstyper.FRIVILLIG);
        medlemskapsperiode.setTrygdedekning(Trygdedekninger.HELSE_OG_PENSJONSDEL);
        return medlemskapsperiode;
    }

}
