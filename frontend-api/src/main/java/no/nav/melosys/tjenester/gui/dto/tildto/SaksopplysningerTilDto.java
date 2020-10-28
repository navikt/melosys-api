package no.nav.melosys.tjenester.gui.dto.tildto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.domain.dokument.sakogbehandling.SobSakDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.behandlingsgrunnlag.soeknad.Periode;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.SaksopplysningerDto;
import no.nav.melosys.tjenester.gui.dto.dokument.PersonhistorikkDto;
import no.nav.melosys.tjenester.gui.dto.eessi.SedDokumentDto;
import no.nav.melosys.tjenester.gui.dto.inntekt.InntektDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.FellesKodeverk.POSTNUMMER;

/**
 * Denne klassen konverterer alle SaksopplysningDokumenter til et objekt tre for frontend.
 */
@Component
public class SaksopplysningerTilDto {
    private static final ZoneId TIME_ZONE_ID = ZoneId.systemDefault();

    //Medlemsperioder sorteres fra nyest til eldst.
    static final Comparator<Medlemsperiode> medlemsperiodeKomparator =
        (o1, o2) -> o2.getPeriode().getFom().compareTo(o1.getPeriode().getFom());

    private final KodeverkService kodeverkService;

    @Autowired
    public SaksopplysningerTilDto(KodeverkService kodeverkService) {
        this.kodeverkService = kodeverkService;
    }

    public SaksopplysningerDto getSaksopplysningerDto(Set<Saksopplysning> saksopplysningSet, Behandling behandling) {
        SaksopplysningerDto dto = new SaksopplysningerDto();
        Periode søknadsperiode = null;
        Land historiskStatsborgerskap = null;

        for (Saksopplysning saksopplysning : saksopplysningSet) {
            SaksopplysningType type = saksopplysning.getType();
            SaksopplysningDokument dokument = saksopplysning.getDokument();

            switch (type) {
                case PERSOPL:
                    dto.setPerson(new PersonTilDto((PersonDokument) dokument));
                    break;
                case ARBFORH:
                    ArbeidsforholdDokument arbeidsforholdDokument = (ArbeidsforholdDokument) dokument;
                    if (arbeidsforholdDokument != null && arbeidsforholdDokument.getArbeidsforhold() != null) {
                        arbeidsforholdDokument.getArbeidsforhold().sort(new ArbeidsforholdComparator());
                    }
                    dto.setArbeidsforhold(arbeidsforholdDokument);
                    break;
                case ORG:
                    dto.getOrganisasjoner().add((OrganisasjonDokument) dokument);
                    break;
                case MEDL:
                    MedlemskapDokument medlemskapDokument = (MedlemskapDokument) dokument;
                    if (medlemskapDokument != null && medlemskapDokument.getMedlemsperiode() != null) {
                        medlemskapDokument.getMedlemsperiode().sort(Comparator.comparing(Medlemsperiode::getType).thenComparing(medlemsperiodeKomparator));
                    }
                    dto.setMedlemskap(medlemskapDokument);
                    break;
                case INNTK:
                    dto.setInntekt(new InntektDto((InntektDokument) dokument));
                    break;
                case SOB_SAK:
                    dto.setSakOgBehandling((SobSakDokument) dokument);
                    break;
                case PERSHIST:
                    PersonhistorikkDokument personhistorikk = (PersonhistorikkDokument) dokument;
                    dto.setPersonhistorikk(new PersonhistorikkDto(personhistorikk));

                    dto.getPersonhistorikk().bostedsadressePerioder.forEach(bostedsadressePeriodeDto ->
                        bostedsadressePeriodeDto.bostedsadresse.setPoststed(
                            kodeverkService.dekod(POSTNUMMER, bostedsadressePeriodeDto.bostedsadresse.getPostnr(), LocalDate.now())));

                    if (!personhistorikk.statsborgerskapListe.isEmpty()) {
                        historiskStatsborgerskap = personhistorikk.statsborgerskapListe.get(0).statsborgerskap;
                    }
                    break;
                case SEDOPPL:
                    dto.setSed(SedDokumentDto.fra((SedDokument) dokument));
                    break;
                case UTBETAL:
                    break;
                default:
                    throw new IllegalArgumentException("Type " + type.getKode() + " ikke støttet.");
            }
        }

        LocalDate gjeldendeDato = hentGjeldendeDato(behandling);

        if (søknadsperiode != null && søknadsperiode.getFom() != null && søknadsperiode.getFom().isBefore(gjeldendeDato)) {
            dto.getPerson().statsborgerskap = historiskStatsborgerskap;
            dto.getPerson().statsborgerskapDato = søknadsperiode.getFom();
        } else {
            dto.getPerson().statsborgerskapDato = gjeldendeDato;
        }

        return dto;
    }

    /**
     * - Ved søknad tilbake i tid, brukes historisk statsborgerskap med fom-dato for søknad som dato
     * - Ved søknad framover i tid, brukes statsborgerskap fra TPS med gjeldende dato, avgrenset
     * av dato for henting av opplysninger (hvis tilstede) eller endring av behandling
     */
    private static LocalDate hentGjeldendeDato(Behandling behandling) {
        if (behandling.getSistOpplysningerHentetDato() != null) {
            return LocalDateTime.ofInstant(behandling.getSistOpplysningerHentetDato(), TIME_ZONE_ID).toLocalDate();
        }
        return LocalDateTime.ofInstant(behandling.getEndretDato(), TIME_ZONE_ID).toLocalDate();
    }

    /**
     * - Åpent arbeidsforhold uten sluttdato sorteres foran/over arbeidsforhold med sluttdato.
     * - Arbeidsforhold må ellers sorteres med nyeste fra-og-med-dato øverst.
     */
    static final class ArbeidsforholdComparator implements Comparator<Arbeidsforhold> {

        @Override
        public int compare(Arbeidsforhold a, Arbeidsforhold b) {
            if (a.getAnsettelsesPeriode().getTom() == null) {
                if (b.getAnsettelsesPeriode().getTom() == null) {
                    return b.getAnsettelsesPeriode().getFom().compareTo(a.getAnsettelsesPeriode().getFom());
                } else {
                    return -1;
                }
            } else if (b.getAnsettelsesPeriode().getTom() == null) {
                return 1;
            } else {
                return b.getAnsettelsesPeriode().getFom().compareTo(a.getAnsettelsesPeriode().getFom());
            }
        }
    }

}
