````text
# Feature: Vehicle Photo Management with Cloudinary Integration

## Contexte projet

Vous travaillez sur FleetPulse, une plateforme de gestion de flotte basée sur une architecture microservices.

Service concerné :
- Backend : services/fleet-api
- Framework : Spring Boot
- Persistence : Spring Data JPA + PostgreSQL
- Migration DB : Flyway
- Authentification : Keycloak OAuth2 Resource Server
- Storage images : Cloudinary

Objectif :
Ajouter une gestion complète des photos associées aux véhicules avec un upload sécurisé Cloudinary.

---

# Décisions architecturales obligatoires

## 1. Relation Vehicle / VehiclePhoto

Créer une entité séparée `VehiclePhoto`.

Relation choisie :
- Un véhicule possède 0 à 5 photos.
- VehiclePhoto possède obligatoirement un Vehicle.
- Ne PAS ajouter de collection `@OneToMany` dans Vehicle.

Pourquoi :
- Eviter le chargement automatique des collections.
- Eviter les problèmes N+1.
- Les photos seront récupérées explicitement via VehiclePhotoRepository.
- Respecter le principe "charger uniquement ce dont on a besoin".

La relation doit être uniquement :

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "vehicle_id", nullable = false)
private Vehicle vehicle;
````

---

# 2. Modèle Entity VehiclePhoto

Créer :

```
domain/entity/VehiclePhoto.java
```

Avec :

Champs :

* id (hérité de DomainEntity)
* vehicle
* url Cloudinary
* publicId Cloudinary
* uploadedAt

Contraintes :

* url obligatoire
* publicId obligatoire
* uploadedAt obligatoire

Exemple attendu :

```java
@Entity
@Table(name = "vehicle_photo")
public class VehiclePhoto extends DomainEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 255)
    private String publicId;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;
}
```

---

# 3. Migration Flyway

Créer une nouvelle migration :

```
db/migration/Vx__create_vehicle_photo.sql
```

Créer table :

vehicle_photo

Colonnes :

* id UUID primary key
* vehicle_id UUID NOT NULL
* url VARCHAR(500)
* public_id VARCHAR(255)
* uploaded_at TIMESTAMP

Relation :

vehicle_photo.vehicle_id → vehicle.id

Avec :

ON DELETE CASCADE

Justification :
Une photo est une composition du véhicule.
Si le véhicule disparaît, ses photos n'ont plus de sens.

Ajouter index :

idx_vehicle_photo_vehicle

---

# 4. Repository

Créer :

```
repository/VehiclePhotoRepository.java
```

Avec méthodes nécessaires :

```java
List<VehiclePhoto> findByVehicleId(UUID vehicleId);

long countByVehicleId(UUID vehicleId);
```

Prévoir suppression :

```java
void deleteById(UUID id);
```

---

# 5. Upload sécurisé Cloudinary

IMPORTANT :

Ne jamais exposer :

* cloud_name secret
* api_secret

au frontend.

Utiliser Signed Upload.

Flux obligatoire :

Frontend
|
| demande signature
v
Fleet API
|
| génère signature avec API_SECRET
v
Frontend
|
| upload direct image
v
Cloudinary
|
| retourne secure_url + public_id
v
Frontend
|
| sauvegarde metadata
v
Fleet API

---

# 6. Endpoint génération signature

Créer :

```
POST /api/vehicles/{vehicleId}/photos/upload-signature
```

Sécurité :

```java
@PreAuthorize("hasRole('ADMIN')")
```

Retour :

```json
{
 "signature":"",
 "timestamp":123456789,
 "apiKey":"",
 "cloudName":"",
 "folder":"fleetpulse/vehicles/{vehicleId}"
}
```

La signature doit contenir au minimum :

* folder
* timestamp

et être générée avec :

Cloudinary API secret.

---

# 7. Endpoint création photo

Créer :

```
POST /api/vehicles/{vehicleId}/photos
```

Responsabilité :

Créer uniquement la ressource VehiclePhoto.

Request :

```json
{
 "url":"https://res.cloudinary.com/...",
 "publicId":"fleetpulse/vehicles/id/photo1"
}
```

Avant insertion :

Vérifier :

* véhicule existe
* nombre photos < 5

Sinon :

Retourner erreur métier HTTP 400.

---

# 8. Endpoint récupération photos

Créer :

```
GET /api/vehicles/{vehicleId}/photos
```

Retour :

```json
[
 {
   "id":"",
   "url":"",
   "publicId":"",
   "uploadedAt":""
 }
]
```

Ne pas charger automatiquement dans VehicleResponse.

---

# 9. Endpoint suppression photo

Créer :

```
DELETE /api/vehicles/{vehicleId}/photos/{photoId}
```

Flux :

1. Vérifier existence photo
2. Vérifier appartenance au véhicule
3. Supprimer l'image Cloudinary via publicId
4. Supprimer la ligne DB

Utiliser Cloudinary SDK côté backend.

Ne jamais supprimer uniquement en base.

---

# 10. DTOs obligatoires

Créer :

```
dto/request/CreateVehiclePhotoRequest.java

dto/request/UploadSignatureResponse.java

dto/response/VehiclePhotoResponse.java
```

Ne jamais exposer directement les Entity.

---

# 11. Service Layer

Créer :

```
service/VehiclePhotoService.java
```

Toute la logique métier doit être ici :

* génération signature
* validation limite 5 photos
* création photo
* suppression Cloudinary
* suppression DB

Controller doit rester léger.

---

# 12. Tests obligatoires

Ajouter tests :

# Integration tests
VehiclePhotoRepositoryIT
+
VehiclePhotoControllerIT

Tester :

POST signature
POST photo
GET photos
DELETE photo

Tester aussi :

* utilisateur non authentifié → 401
* rôle incorrect → 403

Utiliser la configuration existante :

* Testcontainers PostgreSQL
* Keycloak test configuration

---

# 13. Documentation

Mettre à jour :

* OpenAPI annotations si nécessaires
* Collection Postman/Insomnia

Ajouter exemples :

* upload signature
* save photo
* list photos
* delete photo

---

# Contraintes de qualité

Respecter :

* architecture existante du projet
* conventions de nommage actuelles
* Lombok déjà utilisé
* MapStruct si présent
* validation Jakarta
* gestion globale des exceptions existante

Ne pas :

* modifier les entités existantes inutilement
* ajouter @OneToMany dans Vehicle
* stocker des images en base PostgreSQL
* faire transiter les fichiers binaires par Fleet API
* exposer Cloudinary API Secret

Livrable attendu :

Une implémentation complète production-ready de la gestion des photos véhicules avec Cloudinary Signed Upload, intégrée à l'architecture FleetPulse existante.
