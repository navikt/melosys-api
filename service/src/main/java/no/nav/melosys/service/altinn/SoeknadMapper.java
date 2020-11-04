package no.nav.melosys.service.altinn;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.melosys.domain.behandlingsgrunnlag.soeknad.Periode;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.soeknad.Soeknadsland;
import no.nav.melosys.soknad_altinn.*;

public final class SoeknadMapper {
    private SoeknadMapper() {
        throw new UnsupportedOperationException();
    }

    //TODO: MELSOYS-3527
    static Soeknad lagSoeknadDokument(MedlemskapArbeidEOSM søknad) {
        final Soeknad soeknad = new Soeknad();
        final Innhold innhold = søknad.getInnhold();
        soeknad.soeknadsland = hentsoeknadsland(innhold);
        soeknad.periode = lagPeriode(innhold);
        soeknad.personOpplysninger.medfolgendeBarn = hentMedfølgendeBarn(innhold);
        return soeknad;
    }

    private static Soeknadsland hentsoeknadsland(Innhold innhold) {
        Collection<String> landFraAltinn = List.of(innhold.getMidlertidigUtsendt().getArbeidsland());
        return Soeknadsland.av(landFraAltinn);
    }

    private static Periode lagPeriode(Innhold innhold) {
        Tidsrom utsendingsperiode = innhold.getMidlertidigUtsendt().getUtenlandsoppdraget()
            .getPeriodeUtland();
        LocalDate periodeFra = xmlCalTilLocalDate(utsendingsperiode.getPeriodeFra());
        LocalDate periodeTil = xmlCalTilLocalDate(utsendingsperiode.getPeriodeTil());
        return new Periode(periodeFra, periodeTil);
    }

    private static List<String> hentMedfølgendeBarn(Innhold innhold) {
        Barn barn = innhold.getArbeidstaker().getBarn();
        List<String> medfølgendeBarn = new ArrayList<>();

        if (barn != null && barn.getBarnet() != null) {
            medfølgendeBarn = barn.getBarnet().stream()
                .map(Barnet::getFoedselsnummer)
                .collect(Collectors.toList());
        }
        return medfølgendeBarn;
    }

    private static LocalDate xmlCalTilLocalDate(XMLGregorianCalendar calendar) {
        return calendar == null ? null : LocalDate.of(calendar.getYear(), calendar.getMonth(), calendar.getDay());
    }
}
