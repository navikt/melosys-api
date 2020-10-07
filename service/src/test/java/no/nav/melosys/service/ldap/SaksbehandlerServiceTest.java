package no.nav.melosys.service.ldap;

import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ldap.LdapBruker;
import no.nav.melosys.integrasjon.ldap.LdapService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaksbehandlerServiceTest {

    @Mock
    private LdapService brukeroppslag;

    private final String melosysAdGruppe = "MELOSY123";
    private final String ident = "Z990007";

    private SaksbehandlerService saksbehandlerService;

    @BeforeEach
    public void setup() {
        saksbehandlerService = new SaksbehandlerService(brukeroppslag, melosysAdGruppe);
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    void harTilgangTilMelosys_harKorrektAdGruppe_harTilgang() throws TekniskException, IkkeFunnetException {
        when(brukeroppslag.finnBrukerinformasjon(eq(ident)))
            .thenReturn(Optional.of(new LdapBruker("navn", Collections.singleton(melosysAdGruppe))));
        assertThat(saksbehandlerService.harTilgangTilMelosys()).isTrue();
    }

    @Test
    void harTilgangTilMelosys_harIkkeKorrektAdGruppe_harTilgang() throws TekniskException, IkkeFunnetException {
        when(brukeroppslag.finnBrukerinformasjon(eq(ident)))
            .thenReturn(Optional.of(new LdapBruker("navn", Collections.emptyList())));
        assertThat(saksbehandlerService.harTilgangTilMelosys()).isFalse();
    }

    @Test
    void finnNavnForIdent_brukerEksistererKallerToGanger_returnerNavnLdapSkalKunKallesEnGang() throws TekniskException {
        LdapBruker ldapBruker = new LdapBruker("navnesen navn", Collections.emptyList());
        when(brukeroppslag.finnBrukerinformasjon(eq(ident))).thenReturn(Optional.of(ldapBruker));

        assertThat(saksbehandlerService.finnNavnForIdent(ident)).isPresent().get().isEqualTo(ldapBruker.getDisplayName());
        assertThat(saksbehandlerService.finnNavnForIdent(ident)).isPresent().get().isEqualTo(ldapBruker.getDisplayName());

        verify(brukeroppslag, times(1)).finnBrukerinformasjon(eq(ident));
    }
}