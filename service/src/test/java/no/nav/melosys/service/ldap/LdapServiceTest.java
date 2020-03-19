package no.nav.melosys.service.ldap;

import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ldap.LdapBruker;
import no.nav.melosys.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LdapServiceTest {

    @Mock
    private LdapBrukeroppslag brukeroppslag;

    private final String melosysAdGruppe = "MELOSY123";
    private final String ident = "Z990007";

    private LdapService ldapService;

    @Before
    public void setup() {
        ldapService = new LdapService(brukeroppslag, melosysAdGruppe);
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    public void harTilgangTilMelosys_harKorrektAdGruppe_harTilgang() throws TekniskException, IkkeFunnetException {
        when(brukeroppslag.finnBrukerinformasjon(eq(ident)))
            .thenReturn(Optional.of(new LdapBruker("navn", Collections.singleton(melosysAdGruppe))));
        assertThat(ldapService.harTilgangTilMelosys()).isTrue();
    }

    @Test
    public void harTilgangTilMelosys_harIkkeKorrektAdGruppe_harTilgang() throws TekniskException, IkkeFunnetException {
        when(brukeroppslag.finnBrukerinformasjon(eq(ident)))
            .thenReturn(Optional.of(new LdapBruker("navn", Collections.emptyList())));
        assertThat(ldapService.harTilgangTilMelosys()).isFalse();
    }

    @Test
    public void finnNavnForIdent_brukerEksistererKallerToGanger_returnerNavnLdapSkalKunKallesEnGang() throws TekniskException {
        LdapBruker ldapBruker = new LdapBruker("navnesen navn", Collections.emptyList());
        when(brukeroppslag.finnBrukerinformasjon(eq(ident))).thenReturn(Optional.of(ldapBruker));

        assertThat(ldapService.finnNavnForIdent(ident)).isPresent().get().isEqualTo(ldapBruker.getDisplayName());
        assertThat(ldapService.finnNavnForIdent(ident)).isPresent().get().isEqualTo(ldapBruker.getDisplayName());

        verify(brukeroppslag, times(1)).finnBrukerinformasjon(eq(ident));
    }
}