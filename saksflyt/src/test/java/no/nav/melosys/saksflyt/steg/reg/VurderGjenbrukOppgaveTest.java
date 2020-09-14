package no.nav.melosys.saksflyt.steg.reg;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class VurderGjenbrukOppgaveTest {

    private final VurderGjenbrukOppgave vurderGjenbrukOppgave = new VurderGjenbrukOppgave();

    @Test
    public void utfør_opprettOppgave() {
        Prosessinstans prosessinstans = new Prosessinstans();

        vurderGjenbrukOppgave.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.OPPRETT_OPPGAVE);
    }

    @Test
    public void utfør_gjenbrukOppgave() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.OPPRETT_NY_SAK);

        vurderGjenbrukOppgave.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.GJENBRUK_OPPGAVE);
    }
}