package android.leeseungyun.arithmeticoperations

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import org.jetbrains.anko.db.*

const val DATABASE_VERSION = 1

class DBHelper(ctx: Context) :
    ManagedSQLiteOpenHelper(ctx, "MainDB", null, DATABASE_VERSION) {

    private var itemKeyCount = 0

    init {
        instance = this
    }

    companion object {
        private var instance: DBHelper? = null

        @Synchronized
        fun getInstance(ctx: Context) = instance ?: DBHelper(ctx.applicationContext)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable("Item", true,
            "id" to INTEGER + PRIMARY_KEY + UNIQUE,
            "name" to TEXT,
            "count" to INTEGER)
        db.insert("Item",
            "name" to "key",
                    "count" to itemKeyCount
            )
        Log.d("DATABASE", "onCreate - ${db.select("Item").exec { 
            while (this.moveToNext())
                println("${this.getString(0)}")
        }}")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        itemKeyCount = db.select("Item", "name")
            .whereArgs("(name = {itemName})",
                "itemName" to "key").exec {
                parseList(classParser<Int>())[0]
            }
        db.dropTable("Item", true)
        onCreate(db)
    }

}

data class Item(val id: Int, val name: String, val count:Int)