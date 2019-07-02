package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.sed.BucType;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
import no.nav.melosys.service.kafka.model.Periode;
import no.nav.melosys.service.kafka.model.Statsborgerskap;
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
        sedDokument.setBucType(BucType.LA_BUC_04);
        sedDokument.setSedType(SedType.A009);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding());
        prosessinstans.setBehandling(new Behandling());

        opprettSedDokument.utfør(prosessinstans);

        verify(saksopplysningRepository, times(1)).save(any(Saksopplysning.class));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_HENT_PERSON);
    }

    private MelosysEessiMelding hentMelosysEessiMelding() {
        MelosysEessiMelding melding = new MelosysEessiMelding();
        melding.setAktoerId("123");
        melding.setArtikkel("12_1");
        melding.setDokumentId("123321");
        melding.setGsakSaksnummer(432432L);
        melding.setJournalpostId("j123");
        melding.setLovvalgsland("SE");

        Periode periode = new Periode();
        periode.setFom("2012-12-12");
        periode.setTom("2012-12-13");
        melding.setPeriode(periode);

        Statsborgerskap statsborgerskap = new Statsborgerskap();
        statsborgerskap.setLandkode("SE");

        melding.setRinaSaksnummer("r123");
        melding.setSedId("s123");
        melding.setStatsborgerskap(
            Collections.singletonList(statsborgerskap));
        melding.setSedType("A009");
        melding.setBucType("LA_BUC_04");
        return melding;
    }
}