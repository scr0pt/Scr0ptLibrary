package net.scr0pt.crawl.school

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import net.scr0pt.thirdservice.mongodb.MongoConnection
import org.bson.Document
import java.util.*


fun main() {
    val mongoClient =
            MongoClients.create(MongoConnection.megaConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("microsoft")
    val collection: MongoCollection<org.bson.Document> = serviceAccountDatabase.getCollection("microsoft-account")
    collection.insertOne(
            org.bson.Document("email", "Rine1946@outlook.com").append("password", "TheOutlook22001@22")
                    .append("firstname", "Ahmed").append("lastname", "T. Parker")
                    .append("created_at", Date()).append("updated_at", Date())
                    .append("acc_status", "initial")
    )
}

fun main22() {
    val mongoClient = MongoClients.create(MongoConnection.eduConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("service-account")
    val schoolAccountDatabase = mongoClient.getDatabase("edu-school-account")
    val collection: MongoCollection<Document> = serviceAccountDatabase.getCollection("google")
    val collectionVimaru: MongoCollection<Document> = schoolAccountDatabase.getCollection("vinhuni-email-info")

    collection.find(Document("school", "vinhuni")).forEach {
        it.remove("give_way")
        val CMND = it.getString("cmt")
        it.remove("cmt")
        if (CMND != null) {
            it.append("cmnd", CMND)
        }

        val gender = it.getString("gender")
        when (gender) {
            "FeMale" -> it.replace("gender", "female")
            "Male" -> it.replace("gender", "male")
            "None" -> it.remove("gender")
        }

        it.remove("school")
        val hacked = it.getString("hacked")
        var login_status: String? = null
        var email_status: String? = null//ACC_DISABLE
        when (hacked) {
            "Not Yet" -> {

            }
            "No" -> {
                login_status = "PASSWORD_INCORRECT"
            }

            "Not Exist" -> {
                email_status = "NOT_EXIST"
            }
            "Yes" -> {
                login_status = "PASSWORD_CORRECT"
                email_status = "HACKED"
            }
        }

        it.append("login_status", login_status)
        it.append("email_status", email_status)
        it.remove("hacked")

        collectionVimaru.insertOne(it)
    }


}
