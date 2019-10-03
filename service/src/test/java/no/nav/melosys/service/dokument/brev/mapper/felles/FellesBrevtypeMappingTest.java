package no.nav.melosys.service.dokument.brev.mapper.felles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import org.junit.Ignore;
import org.junit.Test;

import static no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactoryTest.hentAlleVerdierFraKodeverk;

public class FellesBrevtypeMappingTest {

    @Test
    public void testLovvalgsbestemmelseKode() throws Exception {
        List<Kodeverk> koder = new ArrayList<>();
        koder.addAll(Arrays.asList(hentAlleVerdierFraKodeverk(Lovvalgbestemmelser_883_2004.class)));
        koder.addAll(Arrays.asList(hentAlleVerdierFraKodeverk(Lovvalgbestemmelser_987_2009.class)));
        for (Kodeverk kode : koder) {
            LovvalgsbestemmelseKode.fromValue(kode.getKode());
        }
    }

    @Test
    public void testTilleggsbestemmelseKoder() throws Exception {
        Kodeverk[] koder = hentAlleVerdierFraKodeverk(Tilleggsbestemmelser_883_2004.class);
        for (Kodeverk kode : koder) {
            if (kode.getKode().equals("FO_883_2004_ART87_7")) continue;     // Ikke i bruk enda
            TilleggsbestemmelseKode.fromValue(kode.getKode());
        }
    }

    @Test
    public void testYrkesaktivitetKoder() throws Exception {
        Kodeverk[] koder = hentAlleVerdierFraKodeverk(Yrkesaktivitetstyper.class);
        for (Kodeverk kode : koder) {
            YrkesaktivitetsKode.fromValue(kode.getKode());
        }
    }

    @Test
    public void testYrkesgruppeKoder() throws Exception {
        Kodeverk[] koder = hentAlleVerdierFraKodeverk(Yrkesgrupper.class);
        for (Kodeverk kode : koder) {
            YrkesgruppeKode.fromValue(kode.getKode());
        }
    }

    @Ignore
    @Test
    public void testBehandlingstypeKode() throws Exception {
        Kodeverk[] koder = hentAlleVerdierFraKodeverk(Behandlingstyper.class);
        for (Kodeverk kode : koder) {
            if (kode.getKode().equals("ANKE")) continue;  // Ikke i bruk enda
            if (kode.getKode().equals("REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING")) continue;
            if (kode.getKode().equals("REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE")) continue;
            if (kode.getKode().equals("UTL_MYND_UTPEKT_SEG_SELV")) continue;
            if (kode.getKode().equals("ANMODNING_OM_UNNTAK_HOVEDREGEL")) continue;
            if (kode.getKode().equals("ØVRIGE_SED")) continue;
            if (kode.getKode().equals("VURDER_TRYGDETID")) continue;
            BehandlingstypeKode.fromValue(kode.getKode());
        }
    }
}
