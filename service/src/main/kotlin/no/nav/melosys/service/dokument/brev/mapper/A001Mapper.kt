package no.nav.melosys.service.dokument.brev.mapper

import no.nav.dok.melosysbrev._000115.*
import no.nav.dok.melosysbrev.felles.melosys_felles.KjoennKode
import no.nav.dok.melosysbrev.felles.melosys_felles.YrkesaktivitetsKode
import no.nav.dok.melosysbrev.felles.melosys_felles.YrkesgruppeKode
import no.nav.melosys.domain.Anmodningsperiode
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.UtenlandskMyndighet
import no.nav.melosys.domain.adresse.Adresse
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.domain.util.IsoLandkodeKonverterer
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.service.brev.felles.LovvalgsbestemmelseKodeMapper
import no.nav.melosys.service.brev.felles.TilleggsbestemmelseKodeMapper
import no.nav.melosys.service.dokument.brev.BrevDataA001
import no.nav.melosys.service.dokument.brev.BrevDataUtils
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.IkkeFysiskArbeidssted
import no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils
import no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactory
import org.apache.commons.lang3.StringUtils
import java.time.Instant
import java.time.LocalDate
import javax.xml.datatype.DatatypeConfigurationException


internal class A001Mapper {
    fun mapSEDA001(brevData: BrevDataA001): SEDA001 {
        val seda001 = SEDA001()

        seda001.antallVedlegg = "0"

        seda001.datoSendt = BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone(Instant.now())

        seda001.landkodeAvsender = hentIso3Landkode(Landkoder.NO.kode)

        seda001.trygdemyndighet =
            mapTrygdemyndighet(brevData.utenlandskMyndighet ?: throw FunksjonellException("Forventer at utenlandsk myndighet ikke er null"))

        seda001.person = mapPerson(
            brevData.persondata ?: throw FunksjonellException("Forventer at persondata ikke er null"),
            brevData.bostedsadresse ?: throw FunksjonellException("Forventer at bostedsadresse ikke er null"),
            brevData.bostedsadresseTypeKode,
            brevData.utenlandskIdent
        )

        seda001.selvstendigNæringsvirksomhetListe = mapSelvstendigvirksometliste(brevData.selvstendigeVirksomheter)
        seda001.foretakListe = mapForetakliste(brevData.arbeidsgivendeVirksomheter)

        seda001.arbeidsstedListe = mapArbeidsstedliste(brevData.arbeidssteder)

        seda001.lovvalgsPeriodeListe = mapAnmodningsperioder(brevData.anmodningsperioder)

        // Alle lovvalgsperiodene må ha samme landkode
        val anmodningsperiode = brevData.anmodningsperioder.first()
        seda001.lovvalgsbestemmelse = LovvalgsbestemmelseKodeMapper.map(anmodningsperiode.unntakFraBestemmelse)
        seda001.lovvalgsLand = hentIso3Landkode(anmodningsperiode.lovvalgsland.kode) // Alltid Norge

        anmodningsperiode.tilleggsbestemmelse?.let {
            seda001.tilleggsbestemmelse = TilleggsbestemmelseKodeMapper.map(anmodningsperiode.tilleggsbestemmelse)
        }

        // Mangler implementasjon i oppgavene. Lev1 støtter ikke purring
        seda001.forespørselType = ForespoerselTypeKode.FOERSTEGANG

        seda001.tidligereAnmodningListe = mapTidligereAnmodningListe(brevData.tidligereAnmodninger)

        seda001.tidligereLovvalgsperiodeListe = mapTidligereLovvalgsperioder(brevData.tidligereLovvalgsperioder)

        VilkaarbegrunnelseFactory.mapAnmodningBegrunnelser(brevData.anmodningBegrunnelser)
            .map { VilkaarBegrunnelseType().apply { standardBegrunnelse = it } }
            .ifPresent { seda001.vilkårBegrunnelse = it }

        VilkaarbegrunnelseFactory.mapAnmodningUtenArt12Begrunnelser(brevData.anmodningUtenArt12Begrunnelser)
            .map { VilkaarBegrunnelseUtenArt12Type().apply { standardBegrunnelse = it } }
            .ifPresent { seda001.vilkårBegrunnelseUtenArt12 = it }

        seda001.fritekst = brevData.anmodningFritekstBegrunnelse
        seda001.ytterligereInformasjon = mapYtterligereInformasjon(brevData)

        brevData.ansettelsesperiode?.let {
            seda001.ansettelsesPeriode = AnsettelsesPeriodeType().apply { fomDato = it.fom.toString() }
        }

        return seda001
    }

    private fun mapYtterligereInformasjon(brevData: BrevDataA001): String? {
        if (brevData.anmodningsperioder.first().unntakFraBestemmelse in LovvalgsbestemmelseKodeMapper.GB_KONV_BESTEMMELSER) {
            val tekstGBKonv = "Issued under the EEA EFTA Convention."
            return if (brevData.ytterligereInformasjon == null) tekstGBKonv else tekstGBKonv + " " + brevData.ytterligereInformasjon
        }
        return brevData.ytterligereInformasjon
    }

    private fun mapTidligereLovvalgsperioder(tidligerePerioder: Collection<Lovvalgsperiode>): TidligereLovvalgsperiodeListeType {
        val tidligereLovvalgsperiodeListeType = TidligereLovvalgsperiodeListeType()
        for (lovvalgsperiode in tidligerePerioder) {
            val periode = PeriodeType()
            try {
                periode.fomDato = BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.fom)
                periode.tomDato = BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.tom)
            } catch (e: DatatypeConfigurationException) {
                throw TekniskException("Feil ved konvertering av dato for tidligere lovvalgsperiode", e)
            }
            periode.lovvalgsbestemmelse = LovvalgsbestemmelseKodeMapper.map(lovvalgsperiode.bestemmelse)
            tidligereLovvalgsperiodeListeType.tidligereLovvalgsperiode.add(periode)
        }
        return tidligereLovvalgsperiodeListeType
    }

    private fun mapTidligereAnmodningListe(tidligereAnmodningdatoer: List<LocalDate>): TidligereAnmodningListeType {
        val tidligereAnmodningListeType = TidligereAnmodningListeType()
        for (dato in tidligereAnmodningdatoer) {
            val tidligereAnmodningType = TidligereAnmodningType()
            try {
                tidligereAnmodningType.tidligereAnmodningsDato = BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone(dato)
            } catch (e: DatatypeConfigurationException) {
                throw TekniskException("Feil ved konvertering av dato for tidligere anmodning", e)
            }
            tidligereAnmodningListeType.tidligereAnmodning.add(tidligereAnmodningType)
        }
        return TidligereAnmodningListeType()
    }

    private fun mapTrygdemyndighet(utenlandskMyndighet: UtenlandskMyndighet): TrygdemyndighetType =
        TrygdemyndighetType().apply {
            trygdemyndighetsinstitusjon = utenlandskMyndighet.institusjonskode
            institusjonsnavn = utenlandskMyndighet.navn
            trygdemyndighetsland = hentIso3Landkode(utenlandskMyndighet.landkode.kode)
            trygdemyndighetsadresse = TrygdemyndighetsadresseType().apply {
                gatenavn = utenlandskMyndighet.getKombinertGateadresse()
                postnummer = utenlandskMyndighet.postnummer
                poststed = utenlandskMyndighet.poststed
            }
        }

    private fun mapPerson(
        personDok: Persondata,
        bostedsadresse: StrukturertAdresse,
        bostedsadresseTypeKode: BostedsadresseTypeKode?,
        utenlandskIdent: String?
    ): PersonType {
        val person = PersonType().apply {
            personnavn = BrevDataUtils.lagPersonnavn(personDok)
            statsborgerskapListe = mapStatsborgerskapListe(personDok)
            kjønn = KjoennKode.fromValue(personDok.hentKjønnType().kode)
            fødselsnummer = personDok.hentFolkeregisterident()
            //Fødeland og Fødested skal ikke fylles ut
            this.bostedsadresse = mapBostedAdresse(bostedsadresse, bostedsadresseTypeKode)
            this.utenlandskID = utenlandskIdent
        }

        try {
            person.fødselsdato = BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone(personDok.getFødselsdatoDato())
        } catch (e: DatatypeConfigurationException) {
            throw TekniskException("Konverteringsfeil ved konvertering av fødselsdato", e)
        }

        return person
    }

    private fun mapStatsborgerskapListe(persondata: Persondata) =
        StatsborgerskapListeType().apply {
            statsborgerskap.addAll(
                persondata.hentAlleStatsborgerskap().map {
                    StatsborgerskapType().apply { statsborgerskap = hentIso3Landkode(it.kode) }
                })
        }

    private fun mapArbeidsstedliste(arbeidssteder: List<Arbeidssted>): ArbeidsstedListeType {
        val arbeidsstedListe = ArbeidsstedListeType()
        for (arbeidssted in arbeidssteder) {
            val arbeidsstedBrev = if (arbeidssted.erFysisk()) {
                mapFysiskArbeidssted(arbeidssted as FysiskArbeidssted)
            } else {
                mapIkkeFysiskArbeidssted(arbeidssted as IkkeFysiskArbeidssted)
            }
            arbeidsstedListe.arbeidssted.add(arbeidsstedBrev)
        }
        return arbeidsstedListe
    }

    private fun mapFysiskArbeidssted(arbeidssted: FysiskArbeidssted): ArbeidsstedType =
        ArbeidsstedType().apply {
            navn = arbeidssted.foretakNavn
            ikkeFysiskArbeidssted = "false"
            yrkesgruppe = YrkesgruppeKode.ORDINAER
            adresse = AdresseType3().apply {
                val virksomhetsAdresse = arbeidssted.getAdresse()
                gatenavn = virksomhetsAdresse.gatenavn
                husnummer = virksomhetsAdresse.husnummerEtasjeLeilighet
                postnummer = virksomhetsAdresse.postnummer
                poststed = virksomhetsAdresse.poststed
                region = virksomhetsAdresse.region
                land = hentIso3Landkode(
                    virksomhetsAdresse.landkode
                        ?: throw FunksjonellException("Forventer at landkode til fysisk arbeidssted ikke er null")
                )
            }
        }

    private fun mapIkkeFysiskArbeidssted(arbeidssted: IkkeFysiskArbeidssted): ArbeidsstedType =
        ArbeidsstedType().apply {
            navn = arbeidssted.getEnhetNavn()
            ikkeFysiskArbeidssted = "true"
            yrkesgruppe = YrkesgruppeKode.fromValue(arbeidssted.yrkesgruppe.kode)
            adresse = AdresseType3().apply { land = hentIso3Landkode(arbeidssted.getLandkode()) }
        }

    private fun mapBostedAdresse(bosted: StrukturertAdresse, bostedsadresseType: BostedsadresseTypeKode?): BostedsadresseType =
        BostedsadresseType().apply {
            gatenavn = bosted.gatenavn
            husnummer = bosted.husnummerEtasjeLeilighet
            postnummer = bosted.postnummer
            poststed = bosted.poststed
            region = bosted.region
            land = hentIso3Landkode(bosted.landkode ?: throw FunksjonellException("Forventer at landkode til bostedsadresse ikke er null"))
            adresseType = bostedsadresseType ?: BostedsadresseTypeKode.BOSTEDSLAND
        }

    private fun mapForetakliste(arbeidsgivendeVirksomheter: List<AvklartVirksomhet>): ForetakListeType {
        val foretakListe = ForetakListeType()
        for (virksomhet in arbeidsgivendeVirksomheter) {
            foretakListe.foretak.add(ForetakType().apply {
                navn = virksomhet.navn
                orgnummer = virksomhet.orgnr
                yrkesaktivitet = YrkesaktivitetsKode.valueOf(virksomhet.yrkesaktivitet.kode)
                hovedvirksomhet = "true" // Kun et foretak i Lev1
                adresse = AdresseType().apply {
                    val virksomhetAdresse = virksomhet.adresse as StrukturertAdresse
                    adresselinje1 = Adresse.sammenslå(virksomhetAdresse.gatenavn, virksomhetAdresse.husnummerEtasjeLeilighet)
                    adresselinje2 = virksomhetAdresse.poststed
                    adresselinje3 = if (StringUtils.isEmpty(virksomhetAdresse.postnummer)) " " else virksomhetAdresse.postnummer
                    adresselinje4 = virksomhetAdresse.region
                    land = hentIso3Landkode(
                        virksomhetAdresse.landkode
                            ?: throw FunksjonellException("Forventer at landkode til foretak ikke er null")
                    )
                }
            })
        }
        return foretakListe
    }

    private fun mapSelvstendigvirksometliste(virksomheter: List<AvklartVirksomhet>): SelvstendigNaeringsvirksomhetListeType {
        val selvstendigeVirksomheter = SelvstendigNaeringsvirksomhetListeType()
        for (virksomhet in virksomheter) {
            selvstendigeVirksomheter.selvstendigNæringsvirksomhet.add(SelvstendigNaeringsvirksomhetType().apply {
                navn = virksomhet.navn
                orgnummer = virksomhet.orgnr
                adresse = AdresseType2().apply {
                    val virksomhetAdresse = virksomhet.adresse as StrukturertAdresse
                    adresselinje1 = Adresse.sammenslå(virksomhetAdresse.gatenavn, virksomhetAdresse.husnummerEtasjeLeilighet)
                    adresselinje2 = virksomhetAdresse.poststed
                    adresselinje3 = virksomhetAdresse.postnummer
                    adresselinje4 = virksomhetAdresse.region
                    land = hentIso3Landkode(
                        virksomhetAdresse.landkode
                            ?: throw FunksjonellException("Forventer at landkode til selvstendig næringsvirksomhet ikke er null")
                    )
                }
            })
        }
        return selvstendigeVirksomheter
    }

    private fun mapAnmodningsperioder(anmodningsperioder: Collection<Anmodningsperiode>): LovvalgsPeriodeListeType {
        val anmodningsperoderBrev = LovvalgsPeriodeListeType()
        for (periode in anmodningsperioder) {
            anmodningsperoderBrev.lovvalgsPeriode.add(mapAnmodningsperiode(periode))
            anmodningsperoderBrev.unntakFraLovvalgsland.add(mapUnntaksland(periode))
        }
        return anmodningsperoderBrev
    }

    private fun mapAnmodningsperiode(periode: Anmodningsperiode): LovvalgsPeriodeType {
        val lovvalgsperiodeBrev = LovvalgsPeriodeType()
        try {
            lovvalgsperiodeBrev.fomDato = BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone(periode.fom)
            lovvalgsperiodeBrev.tomDato = BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone(periode.tom)
        } catch (e: DatatypeConfigurationException) {
            throw TekniskException("Feil ved konvertering")
        }
        return lovvalgsperiodeBrev
    }

    private fun mapUnntaksland(periode: Anmodningsperiode): UnntakFraLovvalgslandType =
        UnntakFraLovvalgslandType().apply {
            unntakFraLovvalgsland.add(hentIso3Landkode(periode.unntakFraLovvalgsland.kode))
        }

    companion object {
        //A001 krever ISO-3
        private fun hentIso3Landkode(landkode: String): String =
            if (landkode.length == 2) IsoLandkodeKonverterer.tilIso3(landkode) else landkode
    }
}
