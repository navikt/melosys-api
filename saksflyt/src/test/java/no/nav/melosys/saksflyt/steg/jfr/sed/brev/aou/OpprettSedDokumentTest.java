package no.nav.melosys.saksflyt.steg.jfr.sed.brev.aou;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    public void utfør_forventFelt() throws MelosysException {
        Behandling behandling = new Behandling();
        behandling.setId(123L);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "123");
        prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, new Periode(LocalDate.now(), LocalDate.now().plusYears(1)));
        prosessinstans.setData(ProsessDataKey.LOVVALGSBESTEMMELSE, FO_883_2004_ART16_1.getKode());
        prosessinstans.setData(ProsessDataKey.UNNTAK_FRA_LOVVALGSBESTEMMELSE, FO_883_2004_ART12_1.getKode());
        prosessinstans.setData(ProsessDataKey.LOVVALGSLAND, Collections.singletonList("DE"));
        prosessinstans.setData(ProsessDataKey.UNNTAK_FRA_LOVVALGSLAND, "NO");
        when(dokumentFactory.lagInternXml(any())).thenReturn("xml");

        opprettSedDokument.utfør(prosessinstans);

        ArgumentCaptor<Saksopplysning> captor = ArgumentCaptor.forClass(Saksopplysning.class);
        verify(saksopplysningRepository).save(captor.capture());

        SedDokument sedDokument = (SedDokument) captor.getValue().getDokument();
        assertThat(sedDokument.getSedType()).isEqualTo(SedType.A001);
        assertThat(sedDokument.getBucType()).isEqualTo(BucType.LA_BUC_01);
        assertThat(sedDokument.getErElektronisk()).isFalse();
        assertThat(sedDokument.getFnr()).isEqualTo("123");
        assertThat(sedDokument.getLovvalgslandKode()).isEqualTo(Landkoder.DE);
        assertThat(sedDokument.getUnntakFraLovvalgslandKode()).isEqualTo(Landkoder.NO);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.OPPRETT_ANMODNINGSPERIODE_FRA_SED);
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK);
    }
}