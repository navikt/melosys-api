package no.nav.melosys.service.dokument.brev.mapper.felles;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

import no.nav.dok.melosysbrev.felles.melosys_felles.LovvalgsbestemmelseKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.TilleggsbestemmelseKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.YrkesaktivitetsKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.YrkesgruppeKode;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import org.junit.Test;

public class FellesBrevtypeMappingTest {

    @Test
    public void testLovvalgsbestemmelseKode() throws Exception {
        List<String> uimplementerteEllerUgyldigeKoder = List.of(
            "FO_883_2004_ART11_3D", "FO_883_2004_ART15"
        );

        Stream<String> koder;
        koder = Stream.concat(hentAlleVerdierFraKodeverk(Lovvalgbestemmelser_883_2004.class),
                              hentAlleVerdierFraKodeverk(Lovvalgbestemmelser_987_2009.class));

        koder.filter(k -> !uimplementerteEllerUgyldigeKoder.contains(k))
            .forEach(LovvalgsbestemmelseKode::fromValue);
    }

    @Test
    public void testTilleggsbestemmelseKoder() throws Exception {
        List<String> uimplementerteEllerUgyldigeKoder = List.of(
            "FO_883_2004_ART87_8"
        );

        hentAlleVerdierFraKodeverk(Tilleggsbestemmelser_883_2004.class)
            .filter(k -> !uimplementerteEllerUgyldigeKoder.contains(k))
            .forEach(TilleggsbestemmelseKode::fromValue);
    }

    @Test
    public void testYrkesaktivitetKoder() throws Exception {
        hentAlleVerdierFraKodeverk(Yrkesaktivitetstyper.class)
            .forEach(YrkesaktivitetsKode::fromValue);
    }

    @Test
    public void testYrkesgruppeKoder() throws Exception {
        hentAlleVerdierFraKodeverk(Yrkesgrupper.class)
            .forEach(YrkesgruppeKode::fromValue);
    }

    /*@Test todo kommenter tilbake når vi brev har synket med vårt kodeverk
    public void testBehandlingstypeKode() throws Exception {
        List<String> uimplementerteEllerUgyldigeKoder = Arrays.asList(
            "SOEKNAD_IKKE_YRKESAKTIV",
            "ANKE", // Ikke i bruk enda
            "REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING",
            "REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE",
            "BESLUTNING_LOVVALG_ANNET_LAND",
            "BESLUTNING_LOVVALG_NORGE",
            "ANMODNING_OM_UNNTAK_HOVEDREGEL",
            "ØVRIGE_SED",
            "VURDER_TRYGDETID",
            "SOEKNAD_ARBEID_FLERE_LAND",
            "SOEKNAD_ARBEID_NORGE_BOSATT_ANNET_LAND"
        );

        hentAlleVerdierFraKodeverk(Behandlingstyper.class)
        .filter(k -> !uimplementerteEllerUgyldigeKoder.contains(k))
        .forEach(BehandlingstypeKode::fromValue);
    }*/

    public static Stream<String> hentAlleVerdierFraKodeverk(Class kodeverk) throws Exception {
        Method getValues = kodeverk.getDeclaredMethod("values");
        Object result = getValues.invoke(null);
        return Stream.of((Kodeverk[]) result).map(Kodeverk::getKode);
    }
}