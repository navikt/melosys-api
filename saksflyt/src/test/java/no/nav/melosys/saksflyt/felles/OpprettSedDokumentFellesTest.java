package no.nav.melosys.saksflyt.felles;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Periode;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.AnmodningUnntak;
import no.nav.melosys.domain.eessi.melding.Avsender;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Statsborgerskap;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OpprettSedDokumentFellesTest {

    @Mock
    private DokumentFactory dokumentFactory;
    @Mock
    private SaksopplysningRepository saksopplysningRepository;

    private OpprettSedDokumentFelles opprettSedDokumentFelles;

    @Before
    public void setup() {
        opprettSedDokumentFelles = new OpprettSedDokumentFelles(dokumentFactory, saksopplysningRepository);
    }

    @Test
    public void opprettSedSaksopplysning() {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        sedDokument.setBucType(BucType.LA_BUC_04);
        sedDokument.setSedType(SedType.A009);

        Behandling behandling = new Behandling();

        opprettSedDokumentFelles.opprettSedSaksopplysning(hentMelosysEessiMelding(), behandling);

        verify(dokumentFactory).lagInternXml(any(Saksopplysning.class));
        verify(saksopplysningRepository).save(any(Saksopplysning.class));
    }

    private MelosysEessiMelding hentMelosysEessiMelding() {
        MelosysEessiMelding melding = new MelosysEessiMelding();
        melding.setAktoerId("123");
        melding.setArtikkel("12_1");
        melding.setAvsender(new Avsender("GB:aopjfsa", "GB"));
        melding.setDokumentId("123321");
        melding.setGsakSaksnummer(432432L);
        melding.setJournalpostId("j123");
        melding.setLovvalgsland("SE");

        Periode periode = new Periode();
        periode.setFom(LocalDate.of(2012, 12, 12));
        periode.setTom(LocalDate.of(2012, 12, 13));
        melding.setPeriode(periode);

        Statsborgerskap statsborgerskap = new Statsborgerskap();
        statsborgerskap.setLandkode("SE");

        melding.setRinaSaksnummer("r123");
        melding.setSedId("s123");
        melding.setStatsborgerskap(
            Collections.singletonList(statsborgerskap));
        melding.setSedType("A009");
        melding.setBucType("LA_BUC_04");

        AnmodningUnntak anmodningUnntak = new AnmodningUnntak();
        anmodningUnntak.setUnntakFraLovvalgsland("NO");
        anmodningUnntak.setUnntakFraLovvalgsbestemmelse("16_1");
        melding.setAnmodningUnntak(anmodningUnntak);
        return melding;
    }
}