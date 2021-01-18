package no.nav.melosys.service.altinn;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.melosys.domain.behandlingsgrunnlag.data.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.soknad_altinn.*;

public final class SoeknadMapper {
    private SoeknadMapper() {
        throw new UnsupportedOperationException();
    }

    static Soeknad lagSoeknad(MedlemskapArbeidEOSM søknad) {
        final Soeknad soeknad = new Soeknad();
        final Innhold innhold = søknad.getInnhold();
        if (innhold.getArbeidstaker().getUtenlandskIDnummer() != null) {
            soeknad.personOpplysninger.utenlandskIdent.add(lagUtenlandskIdent(innhold));
        }
        soeknad.juridiskArbeidsgiverNorge = lagJuridiskArbeidsgiverNorge(innhold.getArbeidsgiver());
        soeknad.soeknadsland = hentsoeknadsland(innhold);
        soeknad.periode = lagPeriode(innhold);
        soeknad.personOpplysninger.medfolgendeFamilie = hentMedfølgendeBarn(innhold);
        return soeknad;
    }

    private static UtenlandskIdent lagUtenlandskIdent(Innhold innhold) {
        UtenlandskIdent utenlandskIdent = new UtenlandskIdent();
        utenlandskIdent.ident = innhold.getArbeidstaker().getUtenlandskIDnummer();
        utenlandskIdent.landkode = innhold.getMidlertidigUtsendt().getArbeidsland();
        return utenlandskIdent;
    }

    private static JuridiskArbeidsgiverNorge lagJuridiskArbeidsgiverNorge(Arbeidsgiver arbeidsgiver) {
        JuridiskArbeidsgiverNorge juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();
        if (arbeidsgiver != null && !arbeidsgiver.isOffentligVirksomhet() && arbeidsgiver.getSamletVirksomhetINorge() != null) {
            SamletVirksomhetINorge samletVirksomhetINorge = arbeidsgiver.getSamletVirksomhetINorge();
            juridiskArbeidsgiverNorge.antallAnsatte = samletVirksomhetINorge.getAntallAnsatte().intValue();
            juridiskArbeidsgiverNorge.antallAdmAnsatte = samletVirksomhetINorge.getAntallAdministrativeAnsatteINorge().intValue();
            juridiskArbeidsgiverNorge.antallUtsendte = samletVirksomhetINorge.getAntallUtsendte().intValue();
            juridiskArbeidsgiverNorge.andelOmsetningINorge = new BigDecimal(samletVirksomhetINorge.getAndelOmsetningINorge());
            juridiskArbeidsgiverNorge.andelOppdragINorge = new BigDecimal(samletVirksomhetINorge.getAndelOppdragINorge());
            juridiskArbeidsgiverNorge.andelKontrakterINorge = new BigDecimal(samletVirksomhetINorge.getAndelKontrakterInngaasINorge());
            juridiskArbeidsgiverNorge.andelRekruttertINorge = new BigDecimal(samletVirksomhetINorge.getAndelRekrutteresINorge());
            juridiskArbeidsgiverNorge.ekstraArbeidsgivere = List.of(arbeidsgiver.getVirksomhetsnummer());
        }
        return juridiskArbeidsgiverNorge;
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

    private static List<MedfolgendeFamilie> hentMedfølgendeBarn(Innhold innhold) {
        Barn barn = innhold.getArbeidstaker().getBarn();
        List<MedfolgendeFamilie> medfølgendeBarn = new ArrayList<>();

        if (barn != null && barn.getBarnet() != null) {
            medfølgendeBarn = barn.getBarnet().stream()
                .map(mapBarnTilMedfølgendeFamilie)
                .collect(Collectors.toList());
        }
        return medfølgendeBarn;
    }

    private static LocalDate xmlCalTilLocalDate(XMLGregorianCalendar calendar) {
        return calendar == null ? null : LocalDate.of(calendar.getYear(), calendar.getMonth(), calendar.getDay());
    }

    private static Function<Barnet, MedfolgendeFamilie> mapBarnTilMedfølgendeFamilie
        = barnet -> MedfolgendeFamilie.tilBarnFraFnrOgNavn(barnet.getFoedselsnummer(), barnet.getNavn());
}
