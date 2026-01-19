package no.nav.melosys.service.dokument.sed.bygger

import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.OrganisasjonDokumentTestFactory
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.arbeidsforholdDokument
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.dokument.person.Familierelasjon
import no.nav.melosys.domain.dokument.person.KjoennsType
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse
import no.nav.melosys.domain.dokument.person.adresse.Gateadresse
import no.nav.melosys.domain.dokument.personDokumentForTest
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.mottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.domain.mottatteopplysninger.soeknad
import no.nav.melosys.domain.personDokument
import no.nav.melosys.domain.saksopplysning
import java.time.LocalDate

// For gjenbruk av AbstraktSedDatabygger implementasjonen i nåværende og fremtidige tester
object DataByggerStubs {

    private fun hentStrukturertAddresseStub() = StrukturertAdresse(
        husnummerEtasjeLeilighet = "25",
        gatenavn = "Gatenavn",
        postnummer = "0165",
        region = "Region",
        landkode = Landkoder.NO.kode
    )

    fun hentBehandlingStub(): Behandling {
        return Behandling.forTest {
            id = 1L
            fagsak {
                medBruker()
                medTrygdemyndighet { institusjonID = "SE:123321" }
            }
            mottatteOpplysninger {
                soeknad {
                    erSelvstendig = true
                    selvstendigForetak("12312312")

                    foretakUtlandMedDetaljer {
                        navn = "navn foretak"
                        uuid = "uuid"
                        adresse(hentStrukturertAddresseStub())
                    }

                    fysiskeArbeidssted {
                        virksomhetNavn = "foretaknavn"
                        landkode = hentStrukturertAddresseStub().landkode
                        gatenavn = hentStrukturertAddresseStub().gatenavn
                        postnummer = hentStrukturertAddresseStub().postnummer
                    }

                    utenlandskIdent("439205843", "SE")
                    maritimtArbeid { enhetNavn = "enhet" }
                }
            }
            saksopplysning {
                type = SaksopplysningType.ARBFORH
                arbeidsforholdDokument { }
            }
            saksopplysning {
                type = SaksopplysningType.PERSOPL
                personDokument {
                    erEgenAnsatt = true
                    fødselsdato = LocalDate.now()
                    bostedsadresse = Bostedsadresse(
                        gateadresse = Gateadresse(),
                        land = Land(Land.NORGE),
                        poststed = "1212"
                    )
                    kjønn = KjoennsType("M")
                    fornavn = "Mrfornavn"
                    etternavn = "Spock"
                    statsborgerskap = Land(Land.NORGE)
                    familiemedlem {
                        navn = "farnavn"
                        fnr = "111111111"
                        familierelasjon = Familierelasjon.FARA
                    }
                }
            }
        }
    }

    fun lagPersonDokument(): PersonDokument = personDokumentForTest {
        erEgenAnsatt = true
        fødselsdato = LocalDate.now()
        bostedsadresse = Bostedsadresse(
            gateadresse = Gateadresse(),
            land = Land(Land.NORGE),
            poststed = "1212"
        )
        kjønn = KjoennsType("M")
        fornavn = "Mrfornavn"
        etternavn = "Spock"
        statsborgerskap = Land(Land.NORGE)
        familiemedlem {
            navn = "farnavn"
            fnr = "111111111"
            familierelasjon = Familierelasjon.FARA
        }
    }

    fun hentBehandlingMedManglendeAdressefelterStub(
        fysiskArbeidsstedManglerLandkode: Boolean,
        arbeidsgivendeForetakUtlandManglerLandkode: Boolean,
        selvstendigForetakUtlandManglerLandkode: Boolean
    ): Behandling {
        val behandling = hentBehandlingStub()
        val mottatteOpplysningerData = behandling.mottatteOpplysninger!!.mottatteOpplysningerData

        val fysiskeArbeidssteder = mottatteOpplysningerData.arbeidPaaLand.fysiskeArbeidssteder as MutableList<FysiskArbeidssted>
        val fysiskArbeidssted = fysiskeArbeidssteder.removeAt(0)
        fysiskArbeidssted.adresse.poststed = null
        if (fysiskArbeidsstedManglerLandkode) {
            fysiskArbeidssted.adresse.landkode = null
        }
        fysiskeArbeidssteder.add(fysiskArbeidssted)

        val foretakUtlandList = mottatteOpplysningerData.foretakUtland as MutableList<ForetakUtland>
        val foretakUtland = foretakUtlandList.removeAt(0)
        foretakUtland.adresse.postnummer = null
        foretakUtland.adresse.poststed = null
        if (arbeidsgivendeForetakUtlandManglerLandkode || selvstendigForetakUtlandManglerLandkode) {
            foretakUtland.adresse.landkode = null
        }
        foretakUtland.selvstendigNæringsvirksomhet = selvstendigForetakUtlandManglerLandkode
        foretakUtlandList.add(foretakUtland)

        return behandling
    }

    fun hentOrganisasjonDokumentSetStub(): Set<OrganisasjonDokument> {
        val orgDokumentHashSet = hashSetOf<OrganisasjonDokument>()
        val organisasjonDokument = OrganisasjonDokumentTestFactory.builder()
            .organisasjonsDetaljer(mockk<OrganisasjonsDetaljer>(relaxed = true))
            .orgnummer("orgnr")
            .build()

        every { organisasjonDokument.organisasjonDetaljer.hentStrukturertForretningsadresse() } returns hentStrukturertAddresseStub()
        orgDokumentHashSet.add(organisasjonDokument)

        return orgDokumentHashSet
    }
}
