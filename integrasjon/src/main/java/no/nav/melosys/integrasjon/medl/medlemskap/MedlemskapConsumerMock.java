package no.nav.melosys.integrasjon.medl.medlemskap;

import java.time.LocalDate;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.kodeverk.*;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeResponse;

import static no.nav.melosys.integrasjon.KonverteringsUtils.localDateToXMLGregorianCalendar;

public class MedlemskapConsumerMock implements MedlemskapConsumer {

    @Override
    public HentPeriodeListeResponse hentPeriodeListe(HentPeriodeListeRequest req) {
        return new HentPeriodeListeResponse().withPeriodeListe(
            lagMockMedlemsperiode()
        );
    }

    @Override
    public HentPeriodeResponse hentPeriode(HentPeriodeRequest req) {
        return new HentPeriodeResponse().withPeriode(lagMockMedlemsperiode());
    }

    private Medlemsperiode lagMockMedlemsperiode() {
        try {
            return new Medlemsperiode()
                .withId(123L)
                .withFraOgMed(localDateToXMLGregorianCalendar(LocalDate.of(2017, 1, 1)))
                .withTilOgMed(localDateToXMLGregorianCalendar(LocalDate.of(2017, 2, 1)))
                .withType(new PeriodetypeMedTerm().withValue("PMMEDSKP"))
                .withStatus(new StatuskodeMedTerm().withValue("UAVK"))
                .withGrunnlagstype(new GrunnlagstypeMedTerm().withValue("FO_16"))
                .withLand(new LandkodeMedTerm().withValue("NOR"))
                .withLovvalg(new LovvalgMedTerm().withValue("UAVK"))
                .withTrygdedekning(new TrygdedekningMedTerm().withValue("Full"))
                .withKildedokumenttype(new KildedokumenttypeMedTerm().withValue("Henv_Soknad"))
                .withKilde(new KildeMedTerm().withValue("srvmelosys"));
        } catch (DatatypeConfigurationException e) {
            return null;
        }
    }
}
