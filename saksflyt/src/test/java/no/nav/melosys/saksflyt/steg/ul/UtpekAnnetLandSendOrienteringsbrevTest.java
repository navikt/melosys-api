package no.nav.melosys.saksflyt.steg.ul;

import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class UtpekAnnetLandSendOrienteringsbrevTest {

    @Mock
    BrevBestiller brevBestiller;
    private UtpekAnnetLandSendOrienteringsbrev utpekAnnetLandSendOrienteringsbrev;

    @Before
    public void settOpp() {
        utpekAnnetLandSendOrienteringsbrev = new UtpekAnnetLandSendOrienteringsbrev(brevBestiller);
    }

    @Test
    public void utfør() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST, "fritekst");

        utpekAnnetLandSendOrienteringsbrev.utfør(prosessinstans);

        ArgumentCaptor<Brevbestilling> captor = ArgumentCaptor.forClass(Brevbestilling.class);
        verify(brevBestiller).bestill(captor.capture());

        Brevbestilling brevbestilling = captor.getValue();
        assertThat(brevbestilling.getDokumentType()).isEqualTo(Produserbaredokumenter.ORIENTERING_UTPEKING_UTLAND);
        assertThat(brevbestilling.getFritekst()).isEqualTo("fritekst");

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.UL_SEND_UTLAND);
    }
}