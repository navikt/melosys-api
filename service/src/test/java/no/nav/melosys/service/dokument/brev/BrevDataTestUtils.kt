package no.nav.melosys.service.dokument.brev

import no.nav.melosys.domain.*
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse
import no.nav.melosys.domain.dokument.person.adresse.Gateadresse
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser.OVER_18_AR
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie
import no.nav.melosys.domain.person.familie.OmfattetFamilie
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted

object BrevDataTestUtils {

    fun lagStrukturertAdresse(): StrukturertAdresse {
        return StrukturertAdresse().apply {
            gatenavn = "Strukturert Gate"
            husnummerEtasjeLeilighet = "12B"
            poststed = "Poststed"
            postnummer = "4321"
            landkode = Landkoder.BG.kode
        }
    }

    fun lagBostedsadresse(): Bostedsadresse {
        return Bostedsadresse().apply {
            land = Land(Land.BELGIA)
            poststed = "Sted"
            postnr = "1234"
            gateadresse = lagGateAdresse()
        }
    }

    private fun lagGateAdresse(): Gateadresse {
        return Gateadresse().apply {
            gatenavn = "Gate"
            gatenummer = 1
            husbokstav = "A"
            husnummer = 123
        }
    }

    fun lagNorskVirksomhet(): AvklartVirksomhet {
        return AvklartVirksomhet("Bedrift AS", "123456789", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID)
    }

    fun lagUtenlandskVirksomhet(): AvklartVirksomhet {
        return AvklartVirksomhet("Bedrift Utenlandsk AS", "123123123", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID)
    }

    fun lagForetakUtland(selvstendig: Boolean): ForetakUtland {
        return ForetakUtland().apply {
            navn = "Company International Ltd."
            orgnr = "12345678910"
            uuid = "49m8gf-9dk4j0"
            adresse = lagStrukturertAdresse()
            adresse.landkode = "NO"
            selvstendigNæringsvirksomhet = selvstendig
        }
    }

    fun lagPersonsaksopplysning(person: PersonDokument): Saksopplysning {
        return lagSaksopplysning(SaksopplysningType.PERSOPL, person)
    }

    fun lagSaksopplysning(type: SaksopplysningType, dokument: SaksopplysningDokument): Saksopplysning {
        return Saksopplysning().apply {
            this.type = type
            this.dokument = dokument
        }
    }

    fun lagMaritimtArbeidssted(): Arbeidssted {
        return lagMaritimtArbeidssted(Maritimtyper.SKIP)
    }

    fun lagMaritimtArbeidssted(maritimtype: Maritimtyper): Arbeidssted {
        val maritimtArbeid = lagMaritimtArbeid()
        val avklartMaritimtArbeid = lagAvklartMaritimtArbeid()
        return MaritimtArbeidssted(maritimtArbeid, avklartMaritimtArbeid)
    }

    fun lagAvklartMaritimtArbeid(): AvklartMaritimtArbeid {
        return AvklartMaritimtArbeid(
            "MaritimtArbeid",
            listOf(Avklartefakta(null, null, Avklartefaktatyper.ARBEIDSLAND, null, "GB"))
        )
    }

    fun lagMaritimtArbeid(): MaritimtArbeid {
        return MaritimtArbeid().apply {
            enhetNavn = "Dunfjæder"
            flaggLandkode = Landkoder.GB.kode
        }
    }

    fun lagAnmodningsperiodeSvarAvslag(): AnmodningsperiodeSvar {
        return AnmodningsperiodeSvar().apply {
            begrunnelseFritekst = "No tiendo"
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
        }
    }

    fun lagAnmodningsperiodeSvarInnvilgelse(): AnmodningsperiodeSvar {
        return AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = Anmodningsperiodesvartyper.DELVIS_INNVILGELSE
            begrunnelseFritekst = "OK"
        }
    }

    fun lagVilkaarsresultat(vilkaar: Vilkaar, oppfylt: Boolean, vararg vilkårbegrunnelser: Kodeverk): Vilkaarsresultat {
        return Vilkaarsresultat().apply {
            isOppfylt = oppfylt
            this.vilkaar = vilkaar
            begrunnelser = hashSetOf()
            for (begrunnelseKode in vilkårbegrunnelser) {
                val begrunnelse = VilkaarBegrunnelse().apply {
                    kode = begrunnelseKode.kode
                }
                begrunnelser.add(begrunnelse)
            }
        }
    }

    fun lagAvklarteMedfølgendeBarn(): AvklarteMedfolgendeFamilie {
        val omfattetBarn = OmfattetFamilie("fnrOmfattet").apply {
            sammensattNavn = "Omfattet Barn"
            ident = "123321123"
        }
        val ikkeOmfattetBarn = IkkeOmfattetFamilie("fnrIkkeOmfattet", OVER_18_AR.kode, null).apply {
            sammensattNavn = "Ikke Omfattet Barn"
            ident = "1111111111"
        }

        return AvklarteMedfolgendeFamilie(setOf(omfattetBarn), setOf(ikkeOmfattetBarn))
    }
}
