package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val password: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    val role: String, // "admin", "magasinier", "consultation"
    @ColumnInfo(name = "assigned_depot_id") val assignedDepotId: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "fournisseurs",
    indices = [Index(value = ["code"], unique = true)]
)
data class Fournisseur(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String,
    val nom: String,
    val telephone: String,
    val adresse: String,
    val email: String,
    val observations: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "clients",
    indices = [Index(value = ["code"], unique = true)]
)
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String,
    val nom: String,
    val telephone: String,
    val adresse: String,
    val email: String,
    val observations: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "depots",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["client_id"])
    ]
)
data class Depot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String,
    val nom: String,
    @ColumnInfo(name = "client_id") val clientId: Int,
    val adresse: String,
    val responsable: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "articles",
    indices = [Index(value = ["code"], unique = true)]
)
data class Article(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String,
    val designation: String,
    val categorie: String,
    val unite: String, // e.g. "kg", "litre", "unité"
    @ColumnInfo(name = "stock_actuel") val stockActuel: Int = 0,
    @ColumnInfo(name = "stock_minimum") val stockMinimum: Int = 0,
    val moule: String = "",
    val qualite: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "entrees",
    foreignKeys = [
        ForeignKey(
            entity = Fournisseur::class,
            parentColumns = ["id"],
            childColumns = ["fournisseur_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Depot::class,
            parentColumns = ["id"],
            childColumns = ["depot_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Article::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["fournisseur_id"]),
        Index(value = ["client_id"]),
        Index(value = ["depot_id"]),
        Index(value = ["article_id"])
    ]
)
data class Entree(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "date_entree") val dateEntree: Long,
    @ColumnInfo(name = "fournisseur_id") val fournisseurId: Int?,
    @ColumnInfo(name = "client_id") val clientId: Int,
    @ColumnInfo(name = "depot_id") val depotId: Int,
    @ColumnInfo(name = "article_id") val articleId: Int,
    val quantite: Int,
    @ColumnInfo(name = "numero_lot") val numeroLot: String,
    val observations: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sorties",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Depot::class,
            parentColumns = ["id"],
            childColumns = ["depot_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Article::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["client_id"]),
        Index(value = ["depot_id"]),
        Index(value = ["article_id"])
    ]
)
data class Sortie(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "date_sortie") val dateSortie: Long,
    @ColumnInfo(name = "client_id") val clientId: Int,
    @ColumnInfo(name = "depot_id") val depotId: Int,
    @ColumnInfo(name = "article_id") val articleId: Int,
    val quantite: Int,
    @ColumnInfo(name = "bon_sortie") val bonSortie: String,
    val observations: String,
    @ColumnInfo(name = "date_chargement") val dateChargement: String = "",
    val matricule: String = "",
    @ColumnInfo(name = "nom_cariste") val nomCariste: String = "",
    val controle: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "mouvements_stock",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Depot::class,
            parentColumns = ["id"],
            childColumns = ["depot_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Article::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Fournisseur::class,
            parentColumns = ["id"],
            childColumns = ["fournisseur_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["client_id"]),
        Index(value = ["depot_id"]),
        Index(value = ["article_id"]),
        Index(value = ["fournisseur_id"])
    ]
)
data class MouvementStock(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "ENTREE", "SORTIE"
    @ColumnInfo(name = "date_movement") val dateMovement: Long,
    @ColumnInfo(name = "client_id") val clientId: Int,
    @ColumnInfo(name = "depot_id") val depotId: Int,
    @ColumnInfo(name = "article_id") val articleId: Int,
    @ColumnInfo(name = "fournisseur_id") val fournisseurId: Int?,
    val quantite: Int,
    val reference: String,
    val observations: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stock_depot",
    foreignKeys = [
        ForeignKey(
            entity = Depot::class,
            parentColumns = ["id"],
            childColumns = ["depot_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Article::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["depot_id"]),
        Index(value = ["article_id"]),
        Index(value = ["depot_id", "article_id"], unique = true)
    ]
)
data class StockDepot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "depot_id") val depotId: Int,
    @ColumnInfo(name = "article_id") val articleId: Int,
    val quantite: Int
)

@Entity(
    tableName = "bon_sortie_photos",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Depot::class,
            parentColumns = ["id"],
            childColumns = ["depot_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["client_id"]),
        Index(value = ["depot_id"])
    ]
)
data class BonSortiePhoto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "bon_sortie_ref") val bonSortieRef: String,
    @ColumnInfo(name = "client_id") val clientId: Int,
    @ColumnInfo(name = "depot_id") val depotId: Int,
    @ColumnInfo(name = "chauffeur_matricule") val chauffeurMatricule: String,
    @ColumnInfo(name = "photo_url") val photoUrl: String,
    val notes: String = "",
    @ColumnInfo(name = "created_by") val createdBy: String = "Admin",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chargement_programmes",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Depot::class,
            parentColumns = ["id"],
            childColumns = ["depot_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["client_id"]),
        Index(value = ["depot_id"])
    ]
)
data class ChargementProgramme(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "programme_ref") val programmeRef: String,
    @ColumnInfo(name = "immatriculation_camion") val immatriculationCamion: String,
    @ColumnInfo(name = "chauffeur_nom") val chauffeurNom: String,
    @ColumnInfo(name = "client_id") val clientId: Int,
    @ColumnInfo(name = "depot_id") val depotId: Int,
    @ColumnInfo(name = "date_chargement") val dateChargement: Long,
    @ColumnInfo(name = "marchandise_details") val marchandiseDetails: String,
    @ColumnInfo(name = "quantite_prevue") val quantitePrevue: Int,
    val statut: String = "PLANIFIÉ", // "PLANIFIÉ", "EN_COURS", "CHARGÉ", "EXPÉDIÉ"
    val notes: String = "",
    @ColumnInfo(name = "created_by") val createdBy: String = "Admin",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

