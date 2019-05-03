package no.nav.melosys.saksflyt.agent.ufm;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class OpprettSedDokumentTest {

    @Mock
    private SaksopplysningRepository saksopplysningRepository;
    @Mock
    private DokumentFactory dokumentFactory;

    private OpprettSedDokument opprettSedDokument;

    @Before
    public void setup() {
        opprettSedDokument = new OpprettSedDokument(saksopplysningRepository, dokumentFactory);
    }

    @Test
    public void utfoerSteg() throws Exception {

        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.SED_DOKUMENT, sedDokument);
        prosessinstans.setBehandling(new Behandling());

        opprettSedDokument.utfør(prosessinstans);

        verify(saksopplysningRepository, times(1)).save(any(Saksopplysning.class));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_OPPDATER_BEHANDLING_OG_MEDL);
    }

    @Test(expected = TekniskException.class)
    public void utfør_seddokumentFinnesIkke_forventException() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        opprettSedDokument.utfør(prosessinstans);
    }
}