package no.nav.melosys.service.vedtak.data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avgift.Trygdeavgift;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadFtrl;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.JuridiskArbeidsgiverNorge;
import no.nav.melosys.domain.behandlingsgrunnlag.data.LoennOgGodtgjoerelse;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.eessi.sed.Adressetype;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.integrasjon.pdl.dto.person.Navn;
import no.nav.melosys.integrasjon.pdl.dto.person.Statsborgerskap;
import no.nav.melosys.service.vedtak.publisering.dto.Fullmektig;
import no.nav.melosys.service.vedtak.publisering.dto.*;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagBostedsadresse;

public class FattetVedtakTestData {

    private static LocalDate NOW = LocalDate.now();
    private static String ORGNR = "987654321";
    private static String FNR = "12345678901";
    private static String FORNAVN = "For";
    private static String MELLOMNANV = "Mellom";
    private static String ETTERNAVN = "Etter";
    private static String LANDKODE_NO = "NO";

    public static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setBehandlingsgrunnlag(lagBehandlingsgrunnlag());
        behandling.setFagsak(lagFagsak());
        behandling.getSaksopplysninger().add(lagPersonDokument());
        return behandling;
    }

    public static Behandlingsresultat lagBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setVedtakMetadata(lagVedtakMetadata());
        behandlingsresultat.setMedlemAvFolketrygden(lagMedlemAvFolketrygden());
        return behandlingsresultat;
    }

    public static FattetVedtak lagFattetVedtak() {
        return new FattetVedtak(lagSak(),
            lagVedtak(),
            lagSoeknad(),
            lagSaksopplysninger(),
            lagAvklarteFakta(),
            lagLovvalgOgMedlemskapsperioder(),
            lagFullmektig(),
            lagRepresentantAvgift()
        );
    }

    private static MedlemAvFolketrygden lagMedlemAvFolketrygden() {
        MedlemAvFolketrygden medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setFastsattTrygdeavgift(lagFastsattTrygdeavgift());
        medlemAvFolketrygden.setMedlemskapsperioder(lagMedlemskapsperioder());

        return medlemAvFolketrygden;
    }

    private static Collection<Medlemskapsperiode> lagMedlemskapsperioder() {
        Medlemskapsperiode m = new Medlemskapsperiode();
        m.setBestemmelse(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8);
        m.setFom(NOW);
        m.setTom(NOW.plusYears(1));
        m.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        m.setTrygdedekning(Trygdedekninger.HELSE_OG_PENSJONSDEL);
        m.setMedlemskapstype(Medlemskapstyper.FRIVILLIG);
        m.setTrygdeavgift(lagTrygdeavgift());

        return List.of(m);
    }

    private static Collection<Trygdeavgift> lagTrygdeavgift() {
        Trygdeavgift norsk = new Trygdeavgift();
        norsk.setAvgiftForInntekt(Trygdeavgift.AvgiftForInntekt.NORSK_INNTEKT);
        norsk.setTrygdesats(BigDecimal.valueOf(2.3));
        norsk.setTrygdeavgiftsbeløpMd(BigDecimal.valueOf(1150));
        norsk.setAvgiftskode("M2E");
        Trygdeavgift utenlandsk = new Trygdeavgift();
        utenlandsk.setAvgiftForInntekt(Trygdeavgift.AvgiftForInntekt.UTENLANDSK_INNTEKT);
        utenlandsk.setTrygdesats(BigDecimal.valueOf(4.3));
        utenlandsk.setTrygdeavgiftsbeløpMd(BigDecimal.valueOf(430));
        utenlandsk.setAvgiftskode("M2D");

        return List.of(norsk, utenlandsk);
    }

    private static FastsattTrygdeavgift lagFastsattTrygdeavgift() {
        FastsattTrygdeavgift trygdeavgift = new FastsattTrygdeavgift();
        Aktoer betalesAv = new Aktoer();
        betalesAv.setOrgnr(ORGNR);
        betalesAv.setRolle(Aktoersroller.ARBEIDSGIVER);

        trygdeavgift.setBetalesAv(betalesAv);
        trygdeavgift.setTrygdeavgiftstype(Trygdeavgift_typer.ENDELIG);
        trygdeavgift.setAvgiftspliktigNorskInntektMnd(50000L);
        trygdeavgift.setAvgiftspliktigUtenlandskInntektMnd(10000L);
        trygdeavgift.setRepresentantNr("000123");

        return trygdeavgift;
    }

    private static VedtakMetadata lagVedtakMetadata() {
        VedtakMetadata vedtakMetadata = new VedtakMetadata();
        vedtakMetadata.setVedtaksdato(Instant.now());
        vedtakMetadata.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);
        return vedtakMetadata;
    }

    private static Fagsak lagFagsak() {
        Aktoer rep = new Aktoer();
        rep.setRepresenterer(Representerer.BRUKER);
        rep.setOrgnr(ORGNR);
        rep.setRolle(Aktoersroller.REPRESENTANT);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");
        fagsak.setRegistrertDato(Instant.now());
        fagsak.setAktører(Set.of(rep));
        fagsak.setType(Sakstyper.FTRL);
        return fagsak;
    }

    private static Behandlingsgrunnlag lagBehandlingsgrunnlag() {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(lagSoeknadFtrlData());
        return behandlingsgrunnlag;
    }

    private static BehandlingsgrunnlagData lagSoeknadFtrlData() {
        SoeknadFtrl soeknadFtrl = new SoeknadFtrl();
        return soeknadFtrl;
    }

    private static Saksopplysning lagPersonDokument() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.setStatsborgerskap(new Land(Land.BELGIA));
        personDokument.setFnr(FNR);
        personDokument.setFornavn(FORNAVN);
        personDokument.setMellomnavn(MELLOMNANV);
        personDokument.setEtternavn(ETTERNAVN);
        personDokument.setBostedsadresse(lagBostedsadresse());
        return lagSaksopplysning(SaksopplysningType.PERSOPL, personDokument);
    }

    private static Saksopplysning lagSaksopplysning(SaksopplysningType type, SaksopplysningDokument dokument) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(type);
        saksopplysning.setDokument(dokument);
        return saksopplysning;
    }

    private static Sak lagSak() {
        return new Sak(FNR,
            1001,
            "2001",
            Sakstyper.FTRL.getKode(),
            NOW);
    }

    private static Vedtak lagVedtak() {
        return new Vedtak(NOW,
            NOW.plusDays(1),
            Vedtakstyper.FØRSTEGANGSVEDTAK.getKode(),
            "Saksbehandler",
            "Saksbehandler2"
        );
    }

    private static LoennOgGodtgjoerelse lagLoennOgGodtgjørelse() {
        return new LoennOgGodtgjoerelse(true,
            true,
            true,
            true,
            new BigDecimal(10_000),
            new BigDecimal(1_000),
            true, new BigDecimal(500),
            true,
            true
        );
    }

    private static Soeknad lagSoeknad() {
        return new Soeknad(Trygdedekninger.UTEN_DEKNING,
            lagLoennOgGodtgjørelse(),
            lagJuridiskArbeidsgiverNorge(),
            List.of(lagForetakUtland()),
            NOW,
            new Periode(NOW, NOW)
        );
    }

    private static ForetakUtland lagForetakUtland() {
        var f = new ForetakUtland();
        f.adresse = lagStrukturertAdresse();
        f.navn = "Navn";
        f.orgnr = ORGNR;
        f.uuid = "300";
        f.selvstendigNæringsvirksomhet = true;
        return f;
    }

    private static StrukturertAdresse lagStrukturertAdresse() {
        return new StrukturertAdresse(
            "tilleggsnavn",
            "gatenavn",
            "2",
            "23",
            "1010",
            "POSTSTED",
            "region",
            LANDKODE_NO
        );
    }

    private static JuridiskArbeidsgiverNorge lagJuridiskArbeidsgiverNorge() {
        var juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();
        juridiskArbeidsgiverNorge.antallAdmAnsatte = 5;
        juridiskArbeidsgiverNorge.antallAnsatte = 100;
        juridiskArbeidsgiverNorge.antallUtsendte = 12;
        juridiskArbeidsgiverNorge.andelOmsetningINorge = new BigDecimal(100_000);
        juridiskArbeidsgiverNorge.andelOppdragINorge = new BigDecimal(10_000);
        juridiskArbeidsgiverNorge.andelKontrakterINorge = new BigDecimal(11_000);
        juridiskArbeidsgiverNorge.andelRekruttertINorge = new BigDecimal(12_000);
        return juridiskArbeidsgiverNorge;
    }

    private static Adresse lagAdresse() {
        return new Adresse(Adressetype.BOSTEDSADRESSE, "Gatenavn", "22", "1000", "POSTSTED");
    }

    private static Person lagPerson() {
        return new Person("1234",
            new Navn(FORNAVN, MELLOMNANV, ETTERNAVN, null),
            new Statsborgerskap(LANDKODE_NO, NOW, NOW.minusMonths(1), NOW.plusMonths(1), null),
            null,
            List.of(lagAdresse())
        );
    }

    private static Saksopplysninger lagSaksopplysninger() {
        return new Saksopplysninger(lagPerson(),
            NOW,
            new Periode(NOW, NOW.plusMonths(1))
        );
    }

    private static Collection<AvklarteFakta> lagAvklarteFakta() {
        return List.of(new AvklarteFakta(Avklartefaktatyper.ARBEIDSLAND.getKode(), LANDKODE_NO, "Begrunnelse", "Begrunnelse fritekst"));
    }

    private static Collection<LovvalgOgMedlemskapsperiode> lagLovvalgOgMedlemskapsperioder() {
        return List.of(new LovvalgOgMedlemskapsperiode(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8,
            null,
            LANDKODE_NO, new Periode(NOW, NOW.plusYears(1)),
            InnvilgelsesResultat.INNVILGET,
            Trygdedekninger.HELSE_OG_PENSJONSDEL,
            Medlemskapstyper.FRIVILLIG,
            new no.nav.melosys.service.vedtak.publisering.dto.FastsattTrygdeavgift(
                new BetalesAv(ORGNR, Aktoersroller.ARBEIDSGIVER),
                new no.nav.melosys.service.vedtak.publisering.dto.Trygdeavgift(1_000L, new BigDecimal(1_500), new BigDecimal(2), "avgiftskode"),
                null
            )));
    }

    private static Identifikator lagIdentifikator() {
        return new Identifikator("Ident", IdentifikatorType.BRUKER);
    }

    private static Fullmektig lagFullmektig() {
        return new Fullmektig(lagIdentifikator());
    }

    private static RepresentantAvgift lagRepresentantAvgift() {
        return new RepresentantAvgift(lagIdentifikator(), "3001");
    }
}
