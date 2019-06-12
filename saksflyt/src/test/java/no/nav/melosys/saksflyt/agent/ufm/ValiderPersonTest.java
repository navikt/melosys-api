package no.nav.melosys.saksflyt.agent.ufm;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ValiderPersonTest {

    @Mock
    private AvklartefaktaService avklartefaktaService;

    private ValiderPerson validerPerson;

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    @Before
    public void setUp() {
        validerPerson = new ValiderPerson(avklartefaktaService);
    }

    @Test
    public void utfør() throws Exception {
        PersonDokument personDokument = new PersonDokument();
        personDokument.bostedsadresse = new Bostedsadresse();
        personDokument.bostedsadresse.setLand(new Land(Land.NORGE));
        personDokument.dødsdato = LocalDate.now();

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        saksopplysning.setDokument(personDokument);

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.getSaksopplysninger().add(saksopplysning);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        validerPerson.utfør(prosessinstans);

        verify(avklartefaktaService, times(2)).leggTilRegistrering(anyLong(), eq(Avklartefaktatype.VURDERING_UNNTAK_PERIODE), stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsExactly(
            Unntak_periode_begrunnelser.PERSON_DOD.getKode(),
            Unntak_periode_begrunnelser.BOSATT_I_NORGE.getKode()
        );
    }
}