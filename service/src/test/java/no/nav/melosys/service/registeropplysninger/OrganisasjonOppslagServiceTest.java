package no.nav.melosys.service.registeropplysninger;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisasjonOppslagServiceTest {

    @Mock
    private EregFasade eregFasade;

    private OrganisasjonOppslagService organisasjonOppslagService;

    @BeforeEach
    void setup() {
        organisasjonOppslagService = new OrganisasjonOppslagService(eregFasade);
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


}
