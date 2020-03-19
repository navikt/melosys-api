package no.nav.melosys.integrasjon.ldap;

import java.util.List;
import java.util.Optional;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import no.nav.melosys.exception.TekniskException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LdapBrukeroppslagTest {

    @Mock
    private LdapTemplate ldapTemplate;

    private LdapBrukeroppslag ldapBrukeroppslag;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        ldapBrukeroppslag = new LdapBrukeroppslag(ldapTemplate);
    }

    @Test
    public void hentBrukerinformasjon_gyldigIdent_brukerReturnert() throws TekniskException {
        LdapBruker ldapBruker = new LdapBruker("navn", List.of("en", "to"));
        when(ldapTemplate.search(any(LdapQuery.class), any(AttributesMapper.class))).thenReturn(List.of(ldapBruker));
        Optional<LdapBruker> res = ldapBrukeroppslag.finnBrukerinformasjon("Z123123");
        assertThat(res).isPresent().get().isEqualTo(ldapBruker);
    }

    @Test
    public void hentBrukerinformasjon_identTomString_kasterException() throws TekniskException {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("Kan ikke slå opp brukernavn uten å ha ident");

        ldapBrukeroppslag.finnBrukerinformasjon("");
    }

    @Test
    public void hentBrukerinformasjon_ugyldigIdent_kasterException() throws TekniskException {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("Mulig LDAP-injection forsøk.");

        ldapBrukeroppslag.finnBrukerinformasjon("cn=killLDAP");
    }

    @Test
    public void mapFromContext_inneholderBrukerMedGrupper_verifiserNavnOgBrukerBlirParset() throws Exception {
        final String navnBruker = "Lars Saksbehandler";
        final List<String> grupper = List.of("CN=myGroup,OU=ApplGroups", "CN=ourGroup,OU=ApplGroups");

        BasicAttributes attributes = new BasicAttributes();
        attributes.put("displayName", navnBruker);
        attributes.put("cn", "L999999");

        BasicAttribute memberOf = new BasicAttribute("memberOf");
        grupper.forEach(memberOf::add);
        attributes.put(memberOf);

        LdapBrukeroppslag.LdapBrukerMapper mapper = new LdapBrukeroppslag.LdapBrukerMapper();
        LdapBruker ldapBruker = mapper.mapFromAttributes(attributes);
        assertThat(ldapBruker.getDisplayName()).isEqualTo(navnBruker);
        assertThat(ldapBruker.getGroups()).isEqualTo(List.of("myGroup", "ourGroup"));
    }
}

