//package no.nav.melosys.domain.dokument.organisasjon
//
//import no.nav.melosys.domain.dokument.felles.Periode
//import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse
//import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
//import org.assertj.core.api.Assertions
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.mockito.Mockito
//import java.util.*
//
//class OrganisasjonsDetaljerTest {
//    private var adresse: SemistrukturertAdresse? = null
//    private val linje1: String = "LINJE1  "
//    private val linje2: String = "LINJE2"
//    private val linje3: String = "LINJE3"
//    private val postnr: String = "postnummer"
//    private val poststed: String = "poststed"
//    private val poststedUtland: String = "poststedUtland"
//    @BeforeEach
//    fun setUp() {
//        val periode = Mockito.mock(
//            Periode::class.java
//        )
//        Mockito.`when`(periode.erGyldig()).thenReturn(true)
//        adresse = SemistrukturertAdresse()
//        adresse.setAdresselinje1(linje1)
//        adresse.setAdresselinje2(linje2)
//        adresse.setAdresselinje3(linje3)
//        adresse.setPostnr(postnr)
//        adresse.setPoststed(poststed)
//        adresse.setPoststedUtland(poststedUtland)
//        val kommunenr = "kommunenr"
//        adresse.setKommunenr(kommunenr)
//        adresse.setGyldighetsperiode(periode)
//    }
//
//    @Test
//    fun testKonverterForretningsadresseTilUstrukturertAdresse() {
//        val landkode = "NO"
//        adresse.setLandkode(landkode)
//        val orgDetaljer = OrganisasjonsDetaljer()
//        orgDetaljer.forretningsadresse = Arrays.asList<GeografiskAdresse?>(adresse)
//        val resultatAdresse = orgDetaljer.hentUstrukturertForretningsadresse()
//        Assertions.assertThat(resultatAdresse.getAdresselinje(1)).isEqualTo(linje1)
//        Assertions.assertThat(resultatAdresse.getAdresselinje(2)).isEqualTo(linje2)
//        Assertions.assertThat(resultatAdresse.getAdresselinje(3)).isEqualTo(linje3)
//        Assertions.assertThat(resultatAdresse.getAdresselinje(4)).isEqualTo("$postnr $poststed")
//        Assertions.assertThat(resultatAdresse.landkode).isEqualTo(landkode)
//    }
//
//    @Test
//    fun testKonverterUtenlandskForretningsadresseTilUstrukturertAdresse() {
//        val landkode = "DK"
//        adresse.setLandkode(landkode)
//        val orgDetaljer = OrganisasjonsDetaljer()
//        orgDetaljer.forretningsadresse = Arrays.asList<GeografiskAdresse?>(adresse)
//        val resultatAdresse = orgDetaljer.hentUstrukturertForretningsadresse()
//        Assertions.assertThat(resultatAdresse.getAdresselinje(1)).isEqualTo(linje1)
//        Assertions.assertThat(resultatAdresse.getAdresselinje(2)).isEqualTo(linje2)
//        Assertions.assertThat(resultatAdresse.getAdresselinje(3)).isEqualTo(linje3)
//        Assertions.assertThat(resultatAdresse.getAdresselinje(4)).isEqualTo(poststedUtland)
//        Assertions.assertThat(resultatAdresse.landkode).isEqualTo(landkode)
//    }
//
//    @Test
//    fun testKonverterForretningsadresseTilStrukturertAdresse() {
//        val landkode = "NO"
//        adresse.setLandkode(landkode)
//        val orgDetaljer = OrganisasjonsDetaljer()
//        orgDetaljer.forretningsadresse = Arrays.asList<GeografiskAdresse?>(adresse)
//        val resultatAdresse = orgDetaljer.hentStrukturertForretningsadresse()
//        Assertions.assertThat(resultatAdresse.gatenavn)
//            .isEqualTo(linje1.trim { it <= ' ' } + " " + linje2 + " " + linje3)
//        Assertions.assertThat(resultatAdresse.landkode).isEqualTo(landkode)
//        Assertions.assertThat(resultatAdresse.postnummer).isEqualTo(postnr)
//        // Ikke alltid poststed for norske registeradresser. Slåes opp med kodeverkservice ved behov
//        Assertions.assertThat(resultatAdresse.poststed).isEqualTo(poststed)
//    }
//
//    @Test
//    fun testKonverterUtenlandskForretningsadresseTilStrukturertAdresse() {
//        val landkode = "DK"
//        adresse.setLandkode(landkode)
//        val orgDetaljer = OrganisasjonsDetaljer()
//        orgDetaljer.forretningsadresse = Arrays.asList<GeografiskAdresse?>(adresse)
//        val resultatAdresse = orgDetaljer.hentStrukturertForretningsadresse()
//        Assertions.assertThat(resultatAdresse.gatenavn)
//            .isEqualTo(linje1.trim { it <= ' ' } + " " + linje2 + " " + linje3)
//        Assertions.assertThat(resultatAdresse.landkode).isEqualTo(landkode)
//        Assertions.assertThat(resultatAdresse.postnummer).isEqualTo(postnr)
//        Assertions.assertThat(resultatAdresse.poststed).isEqualTo(poststedUtland)
//    }
//
//    @Test
//    fun testNullAdresse() {
//        val orgDetaljer = OrganisasjonsDetaljer()
//        val resultatAdresse = orgDetaljer.hentStrukturertForretningsadresse()
//        Assertions.assertThat(resultatAdresse).isNull()
//    }
//}
