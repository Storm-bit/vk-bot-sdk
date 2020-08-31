package com.github.stormbit.sdk.utils.vkapi

import com.github.stormbit.sdk.callbacks.Callback
import com.github.stormbit.sdk.clients.Client
import com.github.stormbit.sdk.utils.Utils
import com.github.stormbit.sdk.utils.vkapi.docs.DocTypes
import net.dongliu.requests.Requests
import net.dongliu.requests.body.Part
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths

@Suppress("unused")
class Upload(client: Client) {
    private val log = LoggerFactory.getLogger(Upload::class.java)
    private val api: API = client.api()

    /* Async methods */

    fun uploadPhotoAsync(photo: String, peerId: Int = 0, callback: Callback<Any>) {
        var type: String? = null
        val photoFile = File(photo)
        if (photoFile.exists()) {
            type = "fromFile"
        }

        var photoUrl: URL? = null
        if (type == null) {
            try {
                photoUrl = URL(photo)
                type = "fromUrl"
            } catch (ignored: MalformedURLException) {
                log.error("Error when trying add photo to message: file not found, or url is bad. Your param: {}", photo)
                callback.onResult("false")
                return
            }
        }

        val photoBytes: ByteArray
        when (type) {

            "fromFile" -> {
                try {
                    photoBytes = Files.readAllBytes(Paths.get(photoFile.toURI()))
                } catch (ignored: IOException) {
                    log.error("Error when reading file {}", photoFile.absolutePath)
                    callback.onResult("false")
                    return
                }
            }

            "fromUrl" -> {
                try {
                    photoBytes = Utils.toByteArray(photoUrl)
                } catch (e: IOException) {
                    log.error("Error {} occurred when reading URL {}", e.toString(), photo)
                    callback.onResult("false")
                    return
                }
            }

            else -> {
                log.error("Bad 'photo' string: path to file, URL or already uploaded 'photo()_()' was expected.")
                callback.onResult("false")
                return
            }
        }

        if (photoBytes != null) {
            uploadPhotoAsync(photoBytes, peerId, callback)
        }
    }

    /**
     * Async uploading photos
     * @param photoBytes Photo bytes
     * @param callback callback
     */
    fun uploadPhotoAsync(photoBytes: ByteArray?, peerId: Int = 0, callback: Callback<Any>) {

        if (photoBytes != null) {

            val params_getMessagesUploadServer = JSONObject().put("peer_id", peerId)
            api.call("photos.getMessagesUploadServer", params_getMessagesUploadServer) { response ->

                if (response.toString().equals("false", true)) {
                    log.error("Can't get messages upload server, aborting. Photo wont be attached to message.")
                    callback.onResult("false")
                    return@call
                }

                val uploadUrl = JSONObject(response.toString()).getString("upload_url")

                val mimeType: String

                try {
                    mimeType = Utils.getMimeType(photoBytes)
                } catch (e: IOException) {
                    log.error(e.message)
                    callback.onResult("false")
                    return@call
                }

                val response_uploadFileString = Requests
                        .post(uploadUrl)
                        .multiPartBody(Part.file("photo", "image.$mimeType", photoBytes))
                        .send().readToText()

                if (response_uploadFileString.length < 2 || response_uploadFileString.contains("error") || !response_uploadFileString.contains("photo")) {
                    log.error("Photo wan't uploaded: {}", response_uploadFileString)
                    callback.onResult("false")
                    return@call
                }

                val getPhotoStringResponse: JSONObject

                try {
                    getPhotoStringResponse = JSONObject(response_uploadFileString)
                } catch (ignored: JSONException) {
                    log.error("Bad response of uploading photo: {}", response_uploadFileString)
                    callback.onResult("false")
                    return@call
                }

                if (!getPhotoStringResponse.has("photo") || !getPhotoStringResponse.has("server") || !getPhotoStringResponse.has("hash")) {
                    log.error("Bad response of uploading photo, no 'photo', 'server' of 'hash' param: {}", getPhotoStringResponse.toString())
                    callback.onResult("false")
                    return@call
                }

                val photoParam = getPhotoStringResponse.getString("photo")
                val serverParam = getPhotoStringResponse.get("server")
                val hashParam = getPhotoStringResponse.getString("hash")

                val params_photosSaveMessagesPhoto = JSONObject().put("photo", photoParam).put("server", serverParam).put("hash", hashParam)

                api.call("photos.saveMessagesPhoto", params_photosSaveMessagesPhoto) { response1 ->


                    if (response1.toString().equals("false", true)) {
                        log.error("Error when saving uploaded photo: response is 'false', see execution errors.")
                        callback.onResult("false")
                        return@call
                    }

                    val response_saveMessagesPhoto = JSONArray(response1.toString()).getJSONObject(0)

                    val ownerId = response_saveMessagesPhoto.getInt("owner_id")
                    val id = response_saveMessagesPhoto.getInt("id")

                    val attach = String.format("photo%s_%s", ownerId, id)
                    callback.onResult(attach)
                }
            }
        }
    }

    /**
     * Async uploading doc
     * @param doc Doc link: url, from disk or already uploaded to VK as doc{owner_id}_{id}
     * @param callback callback
     */
    fun uploadDocAsync(doc: JSONObject, peerId: Int, callback: Callback<Any?>) {
        var type: String? = null
        val fileNameField: String
        val docFile = File(doc.getString("doc"))

        if (docFile.exists()) {
            type = "fromFile"
        }

        var docUrl: URL? = null

        if (type == null) {
            try {
                docUrl = URL(doc.getString("doc"))
                type = "fromUrl"
            } catch (ignored: MalformedURLException) {
                log.error("Error when trying add doc to message: file not found, or url is bad. Your param: {}", doc)
                callback.onResult("false")
                return
            }
        }

        val docBytes: ByteArray?

        when (type) {
            "fromFile" -> {
                try {
                    docBytes = Files.readAllBytes(Paths.get(docFile.toURI()))
                    fileNameField = docFile.name
                } catch (ignored: IOException) {
                    log.error("Error when reading file {}", docFile.absolutePath)
                    callback.onResult("false")
                    return
                }
            }

            "fromUrl" -> {
                try {
                    val conn = docUrl!!.openConnection()
                    try {
                        docBytes = Utils.toByteArray(conn)
                        fileNameField = Utils.guessFileNameByContentType(conn.contentType)
                    } finally {
                        Utils.close(conn)
                    }
                } catch (ignored: IOException) {
                    log.error("Error when reading URL {}", doc)
                    callback.onResult("false")
                    return
                }
            }

            else -> {
                log.error("Bad file or url provided as doc: {}", doc)
                return
            }
        }
        if (docBytes != null) {
            val params = JSONObject().put("peer_id", peerId).put("type", doc.getString("type"))

            api.call("docs.getMessagesUploadServer", params) { response ->
                if (response.toString().equals("false", true)) {
                    log.error("Can't get messages upload server, aborting. Doc wont be attached to message.")
                    callback.onResult("false")
                    return@call
                }

                val uploadUrl = JSONObject(response.toString()).getString("upload_url")

                val response_uploadFileString = Requests
                        .post(uploadUrl)
                        .multiPartBody(Part.file("file", fileNameField, docBytes))
                        .send().readToText()

                if (response_uploadFileString.length < 2 || response_uploadFileString.contains("error") || !response_uploadFileString.contains("file")) {
                    log.error("Doc won't uploaded: {}", response_uploadFileString)
                    callback.onResult("false")
                    return@call
                }

                val getFileStringResponse: JSONObject

                getFileStringResponse = try {
                    JSONObject(response_uploadFileString)
                } catch (ignored: JSONException) {
                    log.error("Bad response of uploading file: {}", response_uploadFileString)
                    callback.onResult("false")

                    return@call
                }

                if (!getFileStringResponse.has("file")) {
                    log.error("Bad response of uploading doc, no 'file' param: {}", getFileStringResponse.toString())
                    callback.onResult("false")

                    return@call
                }

                val fileParam = getFileStringResponse.getString("file")
                val params_photosSaveMessagesPhoto = JSONObject().put("file", fileParam)

                api.call("docs.save", params_photosSaveMessagesPhoto) { response1 ->
                    if (response1.toString().equals("false", true)) {
                        log.error("Error when saving uploaded doc: response is 'false', see execution errors.")
                        callback.onResult("false")
                        return@call
                    }

                    val response_saveMessagesPhotoe = JSONArray(response1.toString()).getJSONObject(0)
                    val ownerId = response_saveMessagesPhotoe.getInt("owner_id")
                    val id = response_saveMessagesPhotoe.getInt("id")
                    val attach = "doc" + ownerId + '_' + id

                    callback.onResult(attach)
                }
            }
        }
    }

    fun uploadPhotoChatAsync(photo: String, chatId: Int, callback: Callback<Any?>) {
        var type: String? = null
        val photoFile = File(photo)

        if (photoFile.exists()) {
            type = "fromFile"
        }

        var photoUrl: URL? = null

        if (type == null) {
            try {
                photoUrl = URL(photo)
                type = "fromUrl"
            } catch (ignored: MalformedURLException) {
                log.error("Error when trying add photo to message: file not found, or url is bad. Your param: {}", photo)
                callback.onResult("false")
                return
            }
        }

        val photoBytes: ByteArray

        photoBytes = when (type) {
            "fromFile" -> {
                try {
                    Files.readAllBytes(Paths.get(photoFile.toURI()))
                } catch (ignored: IOException) {
                    log.error("Error when reading file {}", photoFile.absolutePath)
                    callback.onResult("false")
                    return
                }
            }

            "fromUrl" -> {
                try {
                    Utils.toByteArray(photoUrl)
                } catch (ignored: IOException) {
                    log.error("Error {} occurred when reading URL {}", ignored.toString(), photo)
                    callback.onResult("false")
                    return
                }
            }

            else -> {
                log.error("Bad 'photo' string: path to file, URL or already uploaded 'photo()_()' was expected.")
                callback.onResult("false")
                return
            }
        }

        uploadPhotoChatAsync(photoBytes, chatId, callback)
    }

    fun uploadPhotoChatAsync(photoBytes: ByteArray?, chatId: Int, callback: Callback<Any?>) {
        if (photoBytes != null) {
            val params_getMessagesUploadServer = JSONObject().put("chat_id", chatId)

            api.call("photos.getChatUploadServer", params_getMessagesUploadServer) { response ->
                if (response.toString().equals("false", true)) {
                    log.error("Can't get messages upload server, aborting. Photo wont be attached to message.")
                    callback.onResult(false)
                    return@call
                }

                val uploadUrl = JSONObject(response.toString()).getString("upload_url")

                val mimeType: String

                try {
                    mimeType = Utils.getMimeType(photoBytes)
                } catch (e: IOException) {
                    log.error(e.message)
                    callback.onResult("false")
                    return@call
                }

                val responseUploadFileString = Requests
                        .post(uploadUrl)
                        .multiPartBody(Part.file("file", "photo.$mimeType", photoBytes))
                        .send().readToText()

                if (responseUploadFileString.length < 2 || responseUploadFileString.contains("error") || !responseUploadFileString.contains("response")) {
                    log.error("Photo wan't uploaded: {}", responseUploadFileString)
                    callback.onResult("false")
                    return@call
                }

                val getPhotoStringResponse: JSONObject

                getPhotoStringResponse = try {
                    JSONObject(responseUploadFileString)
                } catch (ignored: JSONException) {
                    log.error("Bad response of uploading photo: {}", responseUploadFileString)
                    callback.onResult("false")
                    return@call
                }

                if (!getPhotoStringResponse.has("response")) {
                    log.error("Bad response of uploading chat photo, no 'response' param: {}", getPhotoStringResponse.toString())
                    callback.onResult("false")
                    return@call
                }

                val responseParam = getPhotoStringResponse.getString("response")
                val params_photosSaveMessagesPhoto = JSONObject().put("file", responseParam)

                api.call("messages.setChatPhoto", params_photosSaveMessagesPhoto) { response1 ->
                    if (response1.toString().equals("false", true)) {
                        log.error("Error when saving uploaded photo: response is 'false', see execution errors.")
                        callback.onResult("false")
                        return@call
                    }

                    callback.onResult(response1)
                }
            }
        }
    }

    /**
     * Upload group cover by file from url or from disk
     *
     * @param cover    cover
     * @param callback callback
     */
    fun uploadCoverGroupAsync(cover: String, groupId: Int, callback: Callback<Any>) {
        if (groupId == 0) {
            log.error("Please, provide group_id when initialising the client, because it's impossible to upload cover to group not knowing it id.")
            return
        }

        val bytes: ByteArray
        val coverFile = File(cover)

        if (coverFile.exists()) {
            bytes = try {
                Utils.toByteArray(coverFile.toURI().toURL())
            } catch (ignored: IOException) {
                log.error("Cover file was exists, but IOException occurred: {}", ignored.toString())
                return
            }
        } else {
            val coverUrl: URL
            try {
                coverUrl = URL(cover)
                bytes = Utils.toByteArray(coverUrl)
            } catch (ignored: IOException) {
                log.error("Bad string was provided to uploadCover method: path to file or url was expected, but got this: {}, error: {}", cover, ignored.toString())
                return
            }
        }

        uploadCoverGroupAsync(bytes, groupId, callback)
    }


    /**
     * Updating cover by bytes (of file or url)
     *
     * @param bytes    bytes[]
     * @param groupId  group id
     * @param callback response will return to callback
     */
    private fun uploadCoverGroupAsync(bytes: ByteArray, groupId: Int, callback: Callback<Any>? = null) {
        val params_getUploadServer: JSONObject = JSONObject()
                .put("group_id", groupId)
                .put("crop_x", 0)
                .put("crop_y", 0)
                .put("crop_x2", 1590)
                .put("crop_y2", 400)

        api.call("photos.getOwnerCoverPhotoUploadServer", params_getUploadServer) { response: Any ->
            val uploadUrl = JSONObject(response.toString()).getString("upload_url")

            val mimeType: String

            try {
                mimeType = Utils.getMimeType(bytes)
            } catch (e: IOException) {
                log.error(e.message)
                callback?.onResult("false")
                return@call
            }

            var coverUploadedResponseString = Requests
                    .post(uploadUrl)
                    .multiPartBody(Part.file("photo", "image.$mimeType", bytes))
                    .send().readToText()

            coverUploadedResponseString = if (coverUploadedResponseString != null && coverUploadedResponseString.length > 2) coverUploadedResponseString else "{}"

            val coverUploadedResponse = JSONObject(coverUploadedResponseString)

            if (coverUploadedResponse.has("hash") && coverUploadedResponse.has("photo")) {
                val hash_field = coverUploadedResponse.getString("hash")
                val photo_field = coverUploadedResponse.getString("photo")

                val params_saveCover = JSONObject()
                        .put("hash", hash_field)
                        .put("photo", photo_field)

                val responseS = JSONObject(api.callSync("photos.saveOwnerCoverPhoto", params_saveCover))

                if (responseS.toString().length < 10 || responseS.toString().contains("error")) {
                    log.error("Some error occurred, cover not uploaded: {}", responseS)
                }

                callback?.onResult(responseS)
            } else {
                log.error("Error occurred when uploading cover: no 'photo' or 'hash' param in response {}", coverUploadedResponse)

                callback?.onResult("false")
            }
        }
    }

    /* Sync */

    private fun uploadPhotoToAlbum(photo: String, id: Int, album_id: Int): String? {
        if (id == 0) {
            log.error("Please, provide group_id when initialising the client, because it's impossible to upload cover to group not knowing it id.")
            return null
        }

        val bytes: ByteArray
        val coverFile = File(photo)

        if (coverFile.exists()) {
            bytes = try {
                Utils.toByteArray(coverFile.toURI().toURL())
            } catch (ignored: IOException) {
                log.error("Cover file was exists, but IOException occurred: {}", ignored.toString())
                return null
            }

        } else {
            val coverUrl: URL
            try {
                coverUrl = URL(photo)
                bytes = Utils.toByteArray(coverUrl)
            } catch (ignored: IOException) {
                log.error("Bad string was provided to uploadPhotoToAlbum method: path to file or url was expected, but got this: {}, error: {}", photo, ignored.toString())
                return null
            }
        }

        return uploadPhotoToAlbum(bytes, id, album_id)
    }

    fun uploadPhotoToAlbum(photoBytes: ByteArray?, album_id: Int, group_id: Int? = null): String? {
        if (photoBytes != null) {
            val params_getMessagesUploadServer = JSONObject().put("album_id", album_id)

            if (group_id != null) {
                params_getMessagesUploadServer.put("group_id", group_id)
            }

            val response = api.callSync("photos.getUploadServer", params_getMessagesUploadServer)

            if (response.toString().equals("false", true)) {
                log.error("Can't get messages upload server, aborting. Photo wont be attached to message.")
                return null
            }

            val uploadUrl = JSONObject(response.toString()).getString("upload_url")

            val mimeType: String

            try {
                mimeType = Utils.getMimeType(photoBytes)
            } catch (e: IOException) {
                log.error(e.message)
                return null
            }

            // Uploading the photo
            val response_uploadFileString = Requests
                    .post(uploadUrl)
                    .multiPartBody(Part.file("photo", "image.$mimeType", photoBytes))
                    .send().readToText()

            if (response_uploadFileString.length < 2 || response_uploadFileString.contains("error") || !response_uploadFileString.contains("photo")) {
                log.error("Photo wan't uploaded: {}", response_uploadFileString)
                return null
            }

            val getPhotoStringResponse: JSONObject

            getPhotoStringResponse = try {
                JSONObject(response_uploadFileString)
            } catch (ignored: JSONException) {
                log.error("Bad response of uploading photo: {}", response_uploadFileString)
                return null
            }

            if (!getPhotoStringResponse.has("photo") || !getPhotoStringResponse.has("server") || !getPhotoStringResponse.has("hash")) {
                log.error("Bad response of uploading photo, no 'photo', 'server' of 'hash' param: {}", getPhotoStringResponse.toString())
                return null
            }

            val photoParam = getPhotoStringResponse.getString("photo")
            val serverParam = getPhotoStringResponse["server"]
            val hashParam = getPhotoStringResponse.getString("hash")
            val params_photosSaveMessagesPhoto = JSONObject().put("photo", photoParam).put("server", serverParam.toString() + "").put("hash", hashParam)

            val response1 = api.callSync("photos.saveMessagesPhoto", params_photosSaveMessagesPhoto)

            if (response1.toString().equals("false",true)) {
                log.error("Error when saving uploaded photo: response is 'false', see execution errors.")
                return null
            }

            val response_saveMessagesPhoto = JSONArray(response1.toString()).getJSONObject(0)
            val ownerId = response_saveMessagesPhoto.getInt("owner_id")
            val id = response_saveMessagesPhoto.getInt("id")

            return String.format("photo%s_%s", ownerId, id)
        }

        return null
    }

    /**
     * @param photo String URL, link to vk doc or path to file
     * @return this
     */
    fun uploadPhoto(photo: String, peerId: Int = 0): String? {
        var type: String? = null
        val photoFile = File(photo)

        if (photoFile.exists()) {
            type = "fromFile"
        }

        var photoUrl: URL? = null

        if (type == null) {
            try {
                photoUrl = URL(photo)
                type = "fromUrl"
            } catch (ignored: MalformedURLException) {
                log.error("Error when trying add photo to message: file not found, or url is bad. Your param: {}", photo)
                return null
            }
        }

        val photoBytes: ByteArray

        photoBytes = when (type) {
            "fromFile" -> {
                try {
                    Files.readAllBytes(Paths.get(photoFile.toURI()))
                } catch (ignored: IOException) {
                    log.error("Error when reading file {}", photoFile.absolutePath)
                    return null
                }
            }

            "fromUrl" -> {
                try {
                    Utils.toByteArray(photoUrl)
                } catch (e: IOException) {
                    log.error("Error {} occured when reading URL {}", e.toString(), photo)
                    return null
                }
            }

            else -> {
                log.error("Bad 'photo' string: path to file, URL or already uploaded 'photo()_()' was expected.")
                return null
            }
        }

        return uploadPhoto(photoBytes, peerId)
    }

    /**
     * Synchronous adding photo to the message
     *
     * @param photoBytes photo bytes
     * @return this
     */
    fun uploadPhoto(photoBytes: ByteArray?, peerId: Int = 0): String? {
        if (photoBytes != null) {

            // Getting of server for uploading the photo
            val getUploadServerResponse = api.callSync("photos.getMessagesUploadServer", "peer_id", peerId)
            var uploadUrl = if (getUploadServerResponse.has("response")) if (getUploadServerResponse.getJSONObject("response").has("upload_url")) getUploadServerResponse.getJSONObject("response").getString("upload_url") else null else null

            // Some error
            if (uploadUrl == null) {
                log.error("No upload url in response: {}", getUploadServerResponse)
                return null
            }

            // if album_id == 3 make it negative
            uploadUrl = uploadUrl.replace("aid=3".toRegex(), String.format("aid=%s", -getUploadServerResponse.getJSONObject("response").getInt("album_id")))

            val mimeType: String

            try {
                mimeType = Utils.getMimeType(photoBytes)
            } catch (e: IOException) {
                log.error(e.message)
                return null
            }

            // Uploading the photo
            val uploadingOfPhotoResponseString = Requests
                    .post(uploadUrl)
                    .multiPartBody(Part.file("photo", "photo.$mimeType", photoBytes))
                    .send().readToText()

            val uploadingOfPhotoResponse: JSONObject

            uploadingOfPhotoResponse = try {
                JSONObject(uploadingOfPhotoResponseString)
            } catch (e: JSONException) {
                log.error("Bad response of uploading photo: {}, error: {}", uploadingOfPhotoResponseString, e.toString())
                return null
            }

            // Getting necessary params
            val server: String
            val photo_param: String
            val hash: String

            if (uploadingOfPhotoResponse.has("server") && uploadingOfPhotoResponse.has("photo") && uploadingOfPhotoResponse.has("hash")) {
                server = "" + uploadingOfPhotoResponse.getInt("server")
                photo_param = uploadingOfPhotoResponse["photo"].toString()
                hash = uploadingOfPhotoResponse.getString("hash")
            } else {
                log.error("No 'photo', 'server' or 'hash' param in response {}", uploadingOfPhotoResponseString)
                return null
            }

            // Saving the photo
            val saveMessagesPhotoResponse = api.callSync("photos.saveMessagesPhoto", "server", server, "photo", photo_param, "hash", hash)
            val photoAsAttach = if (saveMessagesPhotoResponse.has("response")) "photo" + saveMessagesPhotoResponse.getJSONArray("response").getJSONObject(0).getInt("owner_id") + "_" + saveMessagesPhotoResponse.getJSONArray("response").getJSONObject(0).getInt("id") else ""

            return photoAsAttach
        }

        return null
    }

    /**
     * Synchronous uploading doc
     * @param doc       String URL, link to vk doc or path to file
     * @param typeOfDoc Type of doc, 'audio_message' or 'graffiti' ('doc' as default)
     * @return attachment
     */
    fun uploadDoc(doc: String, peerId: Int = 0, typeOfDoc: DocTypes): String? {
        var type: String? = null
        val docFile = File(doc)

        if (docFile.exists()) {
            type = "fromFile"
        }

        var docUrl: URL? = null

        if (type == null) {
            try {
                docUrl = URL(doc)
                type = "fromUrl"
            } catch (ignored: MalformedURLException) {
                log.error("Error when trying add doc to message: file not found, or url is bad. Your param: {}", doc)
                return null
            }
        }

        val docBytes: ByteArray
        val fileNameField: String

        when (type) {
            "fromFile" -> {
                try {
                    docBytes = Files.readAllBytes(Paths.get(docFile.toURI()))
                    fileNameField = docFile.name
                } catch (e: IOException) {
                    log.error("Error when reading file {}", docFile.absolutePath)
                    return null
                }
            }

            "fromUrl" -> {
                try {
                    val conn = docUrl!!.openConnection()
                    try {
                        docBytes = Utils.toByteArray(conn)
                        fileNameField = Utils.guessFileNameByContentType(conn.contentType)
                    } finally {
                        Utils.close(conn)
                    }
                } catch (e: IOException) {
                    log.error("Error {} occurred when reading URL {}", e.toString(), doc)
                    return null
                }
            }

            else -> {
                log.error("Bad 'doc' string: path to file, URL or already uploaded 'doc()_()' was expected, but got this: {}", doc)
                return null
            }
        }

        return uploadDoc(docBytes, peerId, typeOfDoc, fileNameField)
    }

    fun uploadDoc(docBytes: ByteArray?, peerId: Int = 0, typeOfDoc: DocTypes, fileNameField: String?): String? {
        if (docBytes != null) {

            // Getting of server for uploading the photo
            val getUploadServerResponse = api.callSync("docs.getMessagesUploadServer", "peer_id", peerId, "type", typeOfDoc.type)
            val uploadUrl = if (getUploadServerResponse.has("response")) if (getUploadServerResponse.getJSONObject("response").has("upload_url")) getUploadServerResponse.getJSONObject("response").getString("upload_url") else null else null

            // Some error
            if (uploadUrl == null) {
                log.error("No upload url in response: {}", getUploadServerResponse)
                return null
            }

            // Uploading the photo
            val uploadingOfDocResponseString = Requests
                    .post(uploadUrl)
                    .multiPartBody(Part.file("file", fileNameField, docBytes))
                    .send().readToText()

            val uploadingOfDocResponse: JSONObject

            uploadingOfDocResponse = try {
                JSONObject(uploadingOfDocResponseString)
            } catch (e: JSONException) {
                log.error("Bad response of uploading doc: {}, error: {}", uploadingOfDocResponseString, e.toString())
                return null
            }

            // Getting necessary params
            val file: String

            file = if (uploadingOfDocResponse.has("file")) {
                uploadingOfDocResponse.getString("file")
            } else {
                log.error("No 'file' param in response {}", uploadingOfDocResponseString)

                return null
            }

            // Saving the photo
            val saveMessagesDocResponse = api.callSync("docs.save", "file", file)
            val docAsAttach = if (saveMessagesDocResponse.has("response")) "doc" + saveMessagesDocResponse.getJSONArray("response").getJSONObject(0).getInt("owner_id") + "_" + saveMessagesDocResponse.getJSONArray("response").getJSONObject(0).getInt("id") else ""

            return docAsAttach
        } else {
            log.error("Got file or url of doc to be uploaded, but some error occured and readed 0 bytes.")
        }

        return null
    }
}
