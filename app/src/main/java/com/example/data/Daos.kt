package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)
}

@Dao
interface FournisseurDao {
    @Query("SELECT * FROM fournisseurs ORDER BY nom ASC")
    fun getAllFournisseurs(): Flow<List<Fournisseur>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFournisseur(fournisseur: Fournisseur): Long

    @Update
    suspend fun updateFournisseur(fournisseur: Fournisseur)

    @Delete
    suspend fun deleteFournisseur(fournisseur: Fournisseur)

    @Query("SELECT * FROM fournisseurs WHERE code = :code LIMIT 1")
    suspend fun getFournisseurByCode(code: String): Fournisseur?
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY nom ASC")
    fun getAllClients(): Flow<List<Client>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)

    @Query("SELECT * FROM clients WHERE code = :code LIMIT 1")
    suspend fun getClientByCode(code: String): Client?
}

@Dao
interface DepotDao {
    @Query("SELECT * FROM depots ORDER BY nom ASC")
    fun getAllDepots(): Flow<List<Depot>>

    @Query("SELECT * FROM depots WHERE client_id = :clientId ORDER BY nom ASC")
    fun getDepotsByClient(clientId: Int): Flow<List<Depot>>

    @Query("SELECT * FROM depots WHERE client_id = :clientId ORDER BY nom ASC")
    suspend fun getDepotsByClientSync(clientId: Int): List<Depot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepot(depot: Depot): Long

    @Update
    suspend fun updateDepot(depot: Depot)

    @Delete
    suspend fun deleteDepot(depot: Depot)

    @Query("SELECT * FROM depots WHERE code = :code LIMIT 1")
    suspend fun getDepotByCode(code: String): Depot?
}

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY designation ASC")
    fun getAllArticles(): Flow<List<Article>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: Article): Long

    @Update
    suspend fun updateArticle(article: Article)

    @Delete
    suspend fun deleteArticle(article: Article)

    @Query("SELECT * FROM articles WHERE code = :code LIMIT 1")
    suspend fun getArticleByCode(code: String): Article?

    @Query("UPDATE articles SET stock_actuel = :stock WHERE id = :id")
    suspend fun updateStock(id: Int, stock: Int)

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    suspend fun getArticleById(id: Int): Article?
}

@Dao
interface EntreeDao {
    @Query("SELECT * FROM entrees ORDER BY date_entree DESC")
    fun getAllEntrees(): Flow<List<Entree>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntree(entree: Entree): Long
}

@Dao
interface SortieDao {
    @Query("SELECT * FROM sorties ORDER BY date_sortie DESC")
    fun getAllSorties(): Flow<List<Sortie>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSortie(sortie: Sortie): Long
}

@Dao
interface MouvementStockDao {
    @Query("SELECT * FROM mouvements_stock ORDER BY date_movement DESC")
    fun getAllMouvements(): Flow<List<MouvementStock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMouvement(mouvement: MouvementStock): Long
}

@Dao
interface StockDepotDao {
    @Query("SELECT * FROM stock_depot")
    fun getAllStockDepots(): Flow<List<StockDepot>>

    @Query("SELECT * FROM stock_depot WHERE depot_id = :depotId AND article_id = :articleId LIMIT 1")
    suspend fun getStockDepot(depotId: Int, articleId: Int): StockDepot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockDepot(stockDepot: StockDepot): Long

    @Update
    suspend fun updateStockDepot(stockDepot: StockDepot)

    @Query("UPDATE stock_depot SET quantite = :quantite WHERE id = :id")
    suspend fun updateQuantite(id: Int, quantite: Int)

    @Query("DELETE FROM stock_depot WHERE depot_id = :depotId AND article_id = :articleId")
    suspend fun deleteStockDepot(depotId: Int, articleId: Int)
}

@Dao
interface BonSortiePhotoDao {
    @Query("SELECT * FROM bon_sortie_photos ORDER BY created_at DESC")
    fun getAllBonSortiePhotos(): Flow<List<BonSortiePhoto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBonSortiePhoto(photo: BonSortiePhoto): Long

    @Delete
    suspend fun deleteBonSortiePhoto(photo: BonSortiePhoto)
}

@Dao
interface ChargementProgrammeDao {
    @Query("SELECT * FROM chargement_programmes ORDER BY date_chargement DESC")
    fun getAllChargements(): Flow<List<ChargementProgramme>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChargement(chargement: ChargementProgramme): Long

    @Update
    suspend fun updateChargement(chargement: ChargementProgramme)

    @Query("UPDATE chargement_programmes SET statut = :statut WHERE id = :id")
    suspend fun updateStatut(id: Int, statut: String)

    @Delete
    suspend fun deleteChargement(chargement: ChargementProgramme)
}

