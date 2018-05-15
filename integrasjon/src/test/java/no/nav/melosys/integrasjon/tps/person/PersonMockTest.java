package no.nav.melosys.integrasjon.tps.person;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import no.nav.tjeneste.virksomhet.person.v3.informasjon.Aktoer;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;

public class PersonMockTest {
    @Test
    public void hentPerson() throws Exception {
        PersonMock personMock = new PersonMock();
        List<String> støttet = Arrays.asList("88888888884", "88888888885", "88888888886", "99999999999", "99999999991");

        for (String ident : støttet) {
            HentPersonRequest request = new HentPersonRequest();
            NorskIdent norskIdent = new NorskIdent();
            norskIdent.setIdent(ident);
            PersonIdent personIdent = new PersonIdent();
            personIdent.setIdent(norskIdent);
            request.setAktoer(personIdent);

            HentPersonResponse response = personMock.hentPerson(request);

            Aktoer aktoer = response.getPerson().getAktoer();
            assertThat(aktoer).isInstanceOf(PersonIdent.class);

            assertThat(((PersonIdent) aktoer).getIdent().getIdent()).isEqualTo(ident);
        }


    }

}