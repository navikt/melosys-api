package no.nav.melosys.integrasjon.ldap;

import java.util.List;
import java.util.Optional;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import no.nav.melosys.exception.TekniskException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LdapServiceTest {

    @Mock
    private LdapTemplate ldapTemplate;

    private LdapService ldapService;

    @BeforeEach
    public void setup() {
        ldapService = new LdapService(ldapTemplate);
    }

    @Test
    void hentBrukerinformasjon_gyldigIdent_brukerReturnert() throws TekniskException {
        LdapBruker ldapBruker = new LdapBruker("navn", List.of("en", "to"));
        when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class))).thenReturn(List.of(ldapBruker));
        Optional<LdapBruker> res = ldapService.finnBrukerinformasjon("Z123123");
        assertThat(res).isPresent().get().isEqualTo(ldapBruker);
    }

    @Test
    void hentBrukerinformasjon_identTomString_kasterException() throws TekniskException {
        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> ldapService.finnBrukerinformasjon(""))
            .withMessageContaining("Kan ikke slå opp brukernavn uten å ha ident");
    }

    @Test
    void mapFromContext_inneholderBrukerMedGrupper_verifiserNavnOgBrukerBlirParset() throws Exception {
        final String navnBruker = "Lars Saksbehandler";
        final List<String> grupper = List.of("CN=myGroup,OU=ApplGroups", "CN=ourGroup,OU=ApplGroups");

        BasicAttributes attributes = new BasicAttributes();
        attributes.put("displayName", navnBruker);
        attributes.put("cn", "L999999");

        BasicAttribute memberOf = new BasicAttribute("memberOf");
        grupper.forEach(memberOf::add);
        attributes.put(memberOf);

        LdapService.LdapBrukerMapper mapper = new LdapService.LdapBrukerMapper();
        LdapBruker ldapBruker = mapper.mapFromAttributes(attributes);
        assertThat(ldapBruker.getDisplayName()).isEqualTo(navnBruker);
        assertThat(ldapBruker.getGroups()).isEqualTo(List.of("myGroup", "ourGroup"));
    }

    @Test
    void finnNavnForIdent_medMelosysSomBruker_returnererMelosysBrukernavn() throws TekniskException {
        Optional<LdapBruker> bruker = ldapService.finnBrukerinformasjon("MELOSYS");

        assertThat(bruker)
            .isPresent().get()
            .extracting(LdapBruker::getDisplayName)
            .isEqualTo("MELOSYS");

        verify(ldapTemplate, never()).search(any(LdapQuery.class), any(AttributesMapper.class));
    }

    @Test
    void finnNavnForIdent_medUgyldigIdent_returnererMelosysBrukernavn() throws TekniskException {
        Optional<LdapBruker> bruker = ldapService.finnBrukerinformasjon("Ugyldig ident");

        assertThat(bruker)
            .isPresent().get()
            .extracting(LdapBruker::getDisplayName)
            .isEqualTo("MELOSYS");

        verify(ldapTemplate, never()).search(any(LdapQuery.class), any(AttributesMapper.class));
    }
}

