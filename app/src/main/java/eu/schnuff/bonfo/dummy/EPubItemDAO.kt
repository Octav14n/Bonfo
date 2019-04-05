package eu.schnuff.bonfo.dummy

import androidx.room.*

@Dao
interface EPubItemDAO {
    @Query("SELECT * FROM epubitem ORDER BY modified DESC")
    fun getAll(): List<EPubItem>

    @Update
    fun update(ePubItem: EPubItem)

    @Insert
    fun insert(vararg ePubItem: EPubItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(vararg ePubItem: EPubItem)

    @Delete
    fun delete(ePubItem: EPubItem)
}