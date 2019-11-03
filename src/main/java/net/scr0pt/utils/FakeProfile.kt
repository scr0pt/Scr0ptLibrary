package net.scr0pt.utils

import com.google.gson.Gson
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import net.scr0pt.utils.curl.LongConnection
import org.jsoup.Jsoup

/**
 * Created by Long
 * Date: 10/15/2019
 * Time: 10:23 PM
 */


fun main() {

}

class PersonProfile(
        val firstName: String,
        val address: String,
        val lastName: String,
        val maidenName: String,
        val birthday: String,
        val email: String,
        val username: String,
        val password: String
)

object FakeProfileV2 {
    fun getNewProfile(): PersonProfile? {
        try {
            val doc = LongConnection().get("https://www.fakenamegenerator.com/gen-random-us-us.php?fref=gc")?.doc
                    ?: return null
            val element = doc?.selectFirst(".info .content .address") ?: return null
            val name = element?.selectFirst("h3")?.text() ?: return null
            val address = element?.selectFirst(".adr")?.text() ?: return null
            val birthdayStr = doc?.select("dl.dl-horizontal > dt:containsOwn(Birthday) ~ dd")?.text()
                    ?: return null//July 2, 1961
            val username = doc?.select("dl.dl-horizontal > dt:containsOwn(Username) ~ dd")?.text() ?: return null
            val password = doc?.select("dl.dl-horizontal > dt:containsOwn(Password) ~ dd")?.text() ?: return null
            val email = doc?.select("dl.dl-horizontal > dt:containsOwn(Email Address) ~ dd")?.text()?.trim()?.substringBefore(" ")?.trim() ?: return null
            val maidenName =doc?.select("dl.dl-horizontal > dt:containsOwn(maiden name) ~ dd")?.text()?.trim() ?: return null
            return PersonProfile(
                    firstName = name.substringBefore(" "),
                    lastName = name.substringAfter(" "),
                    maidenName = maidenName,
                    address = address,
                    birthday = birthdayStr,
                    username = username,
                    email = email,
                    password = password
            )
        } catch (e: Exception) {
            return null
        }
    }
}

object FakeProfile {
    fun getNewProfile(): Result? {
        try {
            val response =
                    Jsoup.connect("https://randomuser.me/api/").ignoreHttpErrors(true).ignoreContentType(true).execute()
            println(response.body())
            val randomUserResponse =
                    Gson().fromJson<RandomUserResponse>(response.body(), RandomUserResponse::class.java) ?: return null
            return randomUserResponse.results?.first()
        } catch (e: Exception) {
            return null
        }
    }
}


class RandomUserResponse {
    @SerializedName("results")
    @Expose
    var results: ArrayList<Result>? = null
}


class Name {
    @SerializedName("title")
    @Expose
    var title: String? = null
    @SerializedName("first")
    @Expose
    var first: String? = null
    @SerializedName("last")
    @Expose
    var last: String? = null
}


class Result {
    @SerializedName("gender")
    @Expose
    var gender: String? = null
    @SerializedName("name")
    @Expose
    var name: Name? = null
    @Expose
    var email: String? = null
}
