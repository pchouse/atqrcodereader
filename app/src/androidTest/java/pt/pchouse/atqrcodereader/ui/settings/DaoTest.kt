package pt.pchouse.atqrcodereader.ui.settings

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pt.pchouse.atqrcodereader.AppDatabase
import java.io.IOException

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class DaoTest {

    private lateinit var dao: Dao
    private lateinit var db: AppDatabase

    @Before
    fun createDatabase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.settings()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun testReadNull() {
        runBlocking {
            assertNull(dao.getAsync())
        }
    }

    @Test
    @Throws(Exception::class)
    fun testCreateAndUpdate() {
        val settings = Model(1, true, "insert")
        runBlocking {

            dao.saveAsync(settings)

            val inserted = dao.getAsync()!!

            assertEquals(settings.uid, inserted.uid)
            assertEquals(settings.showAllFields, inserted.showAllFields)
            assertEquals(settings.apiUrl, inserted.apiUrl)

            inserted.apiUrl = "update"
            inserted.showAllFields = !settings.showAllFields

            dao.saveAsync(inserted)
            val updated = dao.getAsync()!!

            assertEquals(inserted.uid, updated.uid)
            assertEquals(inserted.showAllFields, updated.showAllFields)
            assertEquals(inserted.apiUrl, updated.apiUrl)
        }
    }

}