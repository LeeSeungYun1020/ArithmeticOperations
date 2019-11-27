package android.leeseungyun.arithmeticoperations

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.room.*
import androidx.room.Database
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Entity(tableName = "item")
data class Item(
    @PrimaryKey @ColumnInfo(name = "name") val itemName: String,
    var count: Int = 0
)

@Dao
interface ItemDao {
    @Query("SELECT * FROM item ORDER BY name ASC")
    fun getAll(): LiveData<List<Item>>

    @Query("SELECT * FROM item WHERE name=(:itemName) LIMIT 1")
    fun findByName(itemName: String): Item

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item)

    @Update
    suspend fun update(item: Item)

    @Query("DELETE FROM item")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(item: Item)


}

@Database(entities = [Item::class], version = 1, exportSchema = false)//엑스트라 쉬마는 테스트 위해 잠시 펄스
abstract class ItemDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: ItemDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ItemDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ItemDatabase::class.java,
                    "item"
                )
                    .addCallback(ItemDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                // return
                instance
            }
        }

        private class ItemDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.itemDao())
                    }
                }
            }
        }

        suspend fun populateDatabase(itemDao: ItemDao) {
            // 테스트 위해 데이터 베이스 초기화 // TODO("서버와 동기화")
            itemDao.deleteAll()
            itemDao.insert(Item("key", 10))
        }

    }
}

class ItemRepository(private val itemDao: ItemDao) {

    val allItems: LiveData<List<Item>> = itemDao.getAll()

    fun itemCount(itemName: String): Int {
        return itemDao.findByName(itemName).count
    }

    suspend fun insert(item: Item) {
        itemDao.insert(item)
    }

    suspend fun update(item: Item) {
        itemDao.update(item)
    }

    suspend fun delete(item: Item) {
        itemDao.delete(item)
    }
}

// Class extends AndroidViewModel and requires application as a parameter.
class ItemViewModel(application: Application) : AndroidViewModel(application) {

    // The ViewModel maintains a reference to the repository to get data.
    private val repository: ItemRepository
    // LiveData gives us updated words when they change.
    val allItems: LiveData<List<Item>>

    init {
        val itemDao = ItemDatabase.getDatabase(application, viewModelScope).itemDao()
        repository = ItemRepository(itemDao)
        allItems = repository.allItems
    }

    // 신규 아이템 추가용
    fun insert(item: Item) = viewModelScope.launch {
        repository.insert(item)
    }

    // 아이템 갯수를 수정
    fun update(item: Item) = viewModelScope.launch {
        repository.update(item)
    }

    // 기존 아이템 제거용
    fun delete(item: Item) = viewModelScope.launch {
        repository.delete(item)
    }
}