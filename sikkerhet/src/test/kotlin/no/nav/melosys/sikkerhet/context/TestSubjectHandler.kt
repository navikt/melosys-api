package no.nav.melosys.sikkerhet.context

class TestSubjectHandler : SubjectHandler() {

    override fun getOidcTokenString(): String? = null

    override fun getUserID(): String = "Z990007"

    override fun getUserName(): String = "Testy test"

    override fun getGroups(): List<String> = emptyList()
}
