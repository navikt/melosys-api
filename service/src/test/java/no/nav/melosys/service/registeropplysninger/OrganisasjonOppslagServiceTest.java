package no.nav.melosys.service.registeropplysninger;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisasjonOppslagServiceTest {

    @Mock
    private EregFasade eregFasade;
    @Mock
    private BehandlingService behandlingService;

    private OrganisasjonOppslagService organisasjonOppslagService;

    @BeforeEach
    void setup() {
        organisasjonOppslagService = new OrganisasjonOppslagService(eregFasade, behandlingService);
    }

    @Test
    void hentOrganisasjon_gyldigOrgnrMedTommeMellomrom_returnererOrganisasjon() {
        final var orgnrMedWhitespace = " 123456789 ";

        var saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(new OrganisasjonDokument());
        when(eregFasade.hentOrganisasjon(orgnrMedWhitespace.trim())).thenReturn(saksopplysning);

        assertThat(organisasjonOppslagService.hentOrganisasjon(orgnrMedWhitespace)).isInstanceOf(OrganisasjonDokument.class);
    }

    @Test
    void hentOrganisasjon_ugyldigOrgnr_kasterFeil() {
        final var ugyldigOrgnr = "1";

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> organisasjonOppslagService.hentOrganisasjon(ugyldigOrgnr))
            .withMessageContaining("Ugyldig orgnr");
    }

    @Test
    void hentOrganisasjonTilVirksomhet_virksomhetOrgnrFinnes_returnererOrganisasjon() {
        String orgnr = "123456789";
        var virksomhet = new Aktoer();
        virksomhet.setRolle(Aktoersroller.VIRKSOMHET);
        virksomhet.setOrgnr(orgnr);
        var fagsak = new Fagsak();
        fagsak.setAktører(Set.of(virksomhet));
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        when(behandlingService.hentBehandling(1L)).thenReturn(behandling);
        var saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(new OrganisasjonDokument());
        when(eregFasade.hentOrganisasjon(orgnr)).thenReturn(saksopplysning);

        assertThat(organisasjonOppslagService.hentOrganisasjonTilVirksomhet(1L)).isInstanceOf(OrganisasjonDokument.class);
    }


    @Test
    void hentOrganisasjonTilVirksomhet_virksomhetOrgnrFinnesIkke_kasterFeil() {
        var behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        when(behandlingService.hentBehandling(1L)).thenReturn(behandling);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> organisasjonOppslagService.hentOrganisasjonTilVirksomhet(1L))
            .withMessageContaining("Finner ingen virksomheter tilknyttet behandling 1");
    }
}
