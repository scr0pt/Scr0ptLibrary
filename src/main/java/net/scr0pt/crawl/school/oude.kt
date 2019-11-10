package net.scr0pt.crawl.school

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.minidev.json.JSONObject
import net.minidev.json.JSONValue
import net.scr0pt.thirdservice.mongodb.MongoConnection
import net.scr0pt.utils.curl.LongConnection
import org.bson.Document
import org.bson.conversions.Bson

/**
 * Created by Long
 * Date: 10/13/2019
 * Time: 10:09 AM
 */

fun main() {
    val mongoClient = MongoClients.create(MongoConnection.eduConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("edu-school-account")
    val collection2: MongoCollection<Document> = serviceAccountDatabase.getCollection("oude-student-infomation-2")
    val conn = LongConnection().apply {
        cookie("MOODLEID_", "%25ED%25C3%251CC%25B7d")
        headers(hashMapOf(
                "Connection" to "keep-alive",
                "Pragma" to "no-cache",
                "Cache-Control" to "no-cache",
                "Accept" to "*/*",
                "Origin" to "http://dttx.ou.edu.vn",
                "X-Requested-With" to "XMLHttpRequest",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36",
                "DNT" to "1",
                "Content-Type" to "application/x-www-form-urlencoded",
                "Referer" to "http://dttx.ou.edu.vn/emailchecker/",
                "Accept-Encoding" to "gzip, deflate",
                "Accept-Language" to "en,en-GB;q=0.9,vi;q=0.8,fr-FR;q=0.7,fr;q=0.6,en-US;q=0.5,ja;q=0.4"
        ))
    }
//    collection2.find(Filters.exists("email", false)).firstOrNull()?.let {
//    collection2.find().forEach {
    collection2.find(Filters.exists("email", false)).forEach {
        val MSSV = it.getString("MSSV")
        val response = conn.post("http://dttx.ou.edu.vn/emailchecker/modules/tkb/thongtinsv.php",
                data = hashMapOf("mssv" to MSSV))
        response?.jsonObj?.let {
            if (it.getAsString("app_error") == "false" && it.getAsString("data") != "false") {
                try {
                    (JSONValue.parse(it.getAsString("data")) as JSONObject?)?.let {
                        val email = it.getAsString("f_email2")
                        val ngaysinh = it.getAsString("f_ngaysinh")
                        val tenlop = it.getAsString("f_tenlop")
                        collection2.updateOne(Document("MSSV", MSSV), Updates.combine(
                                Updates.set("email", email),
                                Updates.set("tenlop", tenlop),
                                Updates.set("NGÀY SINH", ngaysinh)
                        ))
                        println(email)
                    }
                } catch (e: Exception) {
                    println(it)
                    e.printStackTrace()
                }
            }
        }
    }
}

fun main_old() {
    val mongoClient = MongoClients.create(MongoConnection.eduConnection)
    val serviceAccountDatabase = mongoClient.getDatabase("edu-school-account")
    val collection: MongoCollection<Document> = serviceAccountDatabase.getCollection("oude-student-infomation")
    val collection2: MongoCollection<Document> = serviceAccountDatabase.getCollection("oude-student-infomation-2")
//    collection2.deleteMany(Document())
    collection.find(Document()).forEach { old ->
        if (old.containsKey("MSSV")) {
            val mssv = old.getString("MSSV")
            if (mssv != null) {
                println(mssv)

                if (collection2.countDocuments(Document("MSSV", mssv)) > 0) {

                    val a = arrayListOf<Bson>()
                    old.forEach { t, u ->
                        if (t != "_id" && u != null) {
                            a.add(Updates.set(t, u))
                        }
                    }

                    collection2.updateOne(
                            Document("MSSV", mssv), Updates.combine(a)
                    )
                } else {
                    collection2.insertOne(old)
                }
            }
        }
    }
}