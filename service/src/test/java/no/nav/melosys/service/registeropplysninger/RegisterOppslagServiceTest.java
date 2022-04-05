package no.nav.melosys.service.registeropplysninger;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterOppslagServiceTest {

    @Mock
    private EregFasade eregFasade;
    @Mock
    private PersondataFasade persondataFasade;

    private RegisterOppslagService registerOppslagService;

    @BeforeEach
    void setup() {
        registerOppslagService = new RegisterOppslagService(eregFasade);
    }

    @Test
    void hentOrganisasjon_gyldigOrgnrMedTommeMellomrom_returnererOrganisasjon() {
        final var orgnrMedWhitespace = " 123456789 ";

        var saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(new OrganisasjonDokument());
        when(eregFasade.hentOrganisasjon(orgnrMedWhitespace.trim())).thenReturn(saksopplysning);

        assertThat(registerOppslagService.hentOrganisasjon(orgnrMedWhitespace)).isInstanceOf(OrganisasjonDokument.class);
    }

    @Test
    void hentOrganisasjon_ugyldigOrgnr_kasterFeil() {
        final var ugyldigOrgnr = "1";

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> registerOppslagService.hentOrganisasjon(ugyldigOrgnr))
            .withMessageContaining("Ugyldig orgnr");
    }


}
