package no.nav.melosys.melosysmock.utils

import java.nio.charset.StandardCharsets

object FileUtils {
    fun hentBuc(bucID: String) = hentFil("eux/buc_$bucID.json")
    fun hentSed(bucID: String, sedID: String) = hentFil("eux/sed_${bucID}_$sedID.json")
    fun hentInstitusjoner() = hentFil("eux/institusjoner.json")
    fun hentFil(filnavn: String) : String = FileUtils::class.java.classLoader.getResource(filnavn)?.readText(StandardCharsets.UTF_8)!!
}
