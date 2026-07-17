package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        User::class,
        Fournisseur::class,
        Client::class,
        Depot::class,
        Article::class,
        Entree::class,
        Sortie::class,
        MouvementStock::class,
        StockDepot::class,
        BonSortiePhoto::class,
        ChargementProgramme::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun fournisseurDao(): FournisseurDao
    abstract fun clientDao(): ClientDao
    abstract fun depotDao(): DepotDao
    abstract fun articleDao(): ArticleDao
    abstract fun entreeDao(): EntreeDao
    abstract fun sortieDao(): SortieDao
    abstract fun mouvementStockDao(): MouvementStockDao
    abstract fun stockDepotDao(): StockDepotDao
    abstract fun bonSortiePhotoDao(): BonSortiePhotoDao
    abstract fun chargementProgrammeDao(): ChargementProgrammeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestistock_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
