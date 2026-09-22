package com.bexmarket.ng.app

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Realtime
import io.appwrite.services.Storage

object AppwriteProvider {
    lateinit var client: Client
    lateinit var account: Account
    lateinit var databases: Databases
    lateinit var storage: Storage
    lateinit var realtime: Realtime
    lateinit var repository: ProductRepository
    lateinit var context: Context

    fun init(context: Context) {
        this.context = context.applicationContext
        client = Client(this.context)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("6a4d87e7001a45656a73")
            .setSelfSigned(true) // For self-signed certificates, only use for development

        account = Account(client)
        databases = Databases(client)
        storage = Storage(client)
        realtime = Realtime(client)
        repository = ProductRepository(databases, storage)
    }
}
