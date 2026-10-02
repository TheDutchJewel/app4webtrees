# La fenêtre principale

Sous **Affichage › Disposition**, vous choisissez entre trois dispositions. Toutes montrent le même arbre, organisé différemment.

## Navigateur (par défaut)

La barre d'outils en haut, en dessous la **personne centrale** avec ses conjoints et ses enfants, à droite ses ancêtres, génération par génération.

- **Un clic** sur une personne en fait la personne centrale.
- **Un double-clic** ouvre la [fiche individuelle](hilfe:person).
- **Bouton droit de la souris** : afficher comme personne centrale, modifier, ajouter aux favoris, ouvrir dans webtrees.
- La **fratrie** figure dans l'encadré en haut à gauche (demi-frères et demi-sœurs avec ½) ; un clic en fait la personne centrale.
- **Clavier :** flèche droite vers le père (avec Shift vers la mère), gauche vers l'enfant, haut/bas pour parcourir la fratrie, Enter ouvre la fiche (voir [Raccourcis clavier](hilfe:tasten)).
- Les **générations** (2 à 7) et le **zoom** (−, +, ajuster) se règlent dans le navigateur lui-même ; le choix est mémorisé.
- Un parent manquant apparaît comme « Père inconnu » ou « Mère inconnue ». Avec les droits de modification, **Ajouter un parent** crée la personne manquante.

**Barre d'outils :** Aller à, Modifier, Ajouter un parent, Favoris, Retour, Suivant, Historique, Personne de départ, Liste, Tableau, Imprimer, Accueil, Photos, Vérification, Lieux, Sources, Aide, Quitter. Si la largeur ne suffit pas, seules les icônes s'affichent (le nom apparaît au survol), et pour finir le reste passe dans le menu **Plus**. Désactivez les libellés sous **Affichage › Libellés de la barre d'outils**.

## Arbre au centre

Trois colonnes : à gauche la **liste des personnes** avec un champ de recherche (Ctrl+F y place le curseur), au milieu l'**arbre** en sablier autour de la personne centrale (faire glisser, zoomer avec la molette, déplier les branches vers le haut), à droite le **panneau de la personne** avec les détails de la personne sélectionnée.

En dézoomant, une case montre moins de choses plutôt que de rapetisser : d'abord sans image ni années, puis seulement le prénom, enfin une case dans la couleur du sexe. Clic droit dans la liste : comme personne centrale, profil, ajouter aux favoris, ouvrir dans webtrees.

## Vue famille

Comme « Arbre au centre », mais le milieu montre la **famille** de la personne centrale : en haut les parents des deux conjoints, au milieu le couple avec son mariage, en dessous les enfants avec leurs dates et leurs mariages. Si la personne s'est mariée plusieurs fois, chaque mariage a son propre **onglet**.

- **Un clic** sélectionne une personne ; le panneau de la personne à droite l'affiche.
- **Un double-clic** en fait la personne centrale – c'est ainsi que vous parcourez les familles vers le haut (parents) et vers le bas (enfants).
- **Bouton droit de la souris** : Mettre au centre, Ajouter un parent, Ouvrir dans webtrees.
- Un parent manquant apparaît comme « Père inconnu » ou « Mère inconnue » ; avec les droits de modification, **+ ajouter** le crée.
- La **fratrie** de la personne centrale est affichée au-dessus du couple. **Clavier :** flèche haut vers le père (avec Shift vers la mère), bas vers l'enfant, gauche/droite pour parcourir la fratrie, Tab pour l'onglet suivant.

La [table des personnes](hilfe:tabelle) (Ctrl+4) montre tout le monde sous forme de tableau.

## Sources

**Affichage › Sources** (Ctrl+5) ou l'icône **Sources** ouvre la gestion des sources : à gauche toutes les sources de l'arbre avec recherche et nombre de citations, à droite la source sélectionnée avec auteur, publication, dépôt d'archives et cote, texte, notes, médias et **Citée par** – chaque individu et chaque famille avec les faits qui portent la citation. Un clic sélectionne la personne, un double-clic en fait la personne centrale.

Avec les droits de modification : **+ Nouvelle source** (titre, auteur, publication, abréviation, dépôt d'archives avec cote – y compris un nouveau dépôt –, texte, note), **Modifier**, **Ajouter un scan ou un fichier …** (joint le fichier à la source comme document) et **Supprimer** (avec un avertissement si la source est encore citée). **Retirer les sources inutilisées …** liste toutes les sources que personne ne cite, à cocher puis supprimer. Dans la boîte de dialogue de citation, **Nouvelle source …** crée directement une source et **Source depuis un fichier …** en crée une à partir d'un scan : titre tiré du nom de fichier, fichier joint, source aussitôt sélectionnée.

Les **documents** se joignent à deux endroits, comme dans webtrees : à la **source** (le registre paroissial numérisé) ou à la **citation** elle-même (le scan de ce baptême précis). Aux deux endroits, vous pouvez **envoyer un fichier** ou choisir un **média existant** de l'arbre ; « détacher » ou ✕ supprime seulement le lien, l'objet média et le fichier sont conservés.

Dans la fiche individuelle, la zone de détail sous la table des événements (onglet **Sources**) montre chaque citation avec page, qualité, date, extrait, notes et médias ; un clic sur le titre ouvre la source ici. Une source sans enregistrement propre (« selon Martha Meier ») est affichée en italique.

Avec les droits de modification, les boutons en dessous sont **+ Citer une source** (chercher une source dans la gestion des sources ou la saisir en texte, avec page, qualité, date, extrait et note), **Modifier**, **Retirer**, **▲ ▼** (ordre) et **Copier vers …** – la même citation pour d'autres événements de cette personne, pour les parents, conjoints et enfants (comme citation générale sur l'enregistrement) ou pour l'union. Seul ce que vous saisissez est modifié ; tout le reste de la citation et de l'événement est conservé.

Dans les dispositions **Arbre au centre** et **Vue famille**, le bouton **Sources** se trouve dans la barre du haut ; dans le panneau de la personne à droite (onglet Événements), chaque citation est cliquable et ouvre la source.

Nécessite api4webtrees avec le niveau d'API 18 ; avec des serveurs plus anciens, l'icône ouvre comme avant la liste des sources de webtrees.


## Lieux

**Affichage › Lieux** (Ctrl+6) ou l’icône **Lieux** ouvre la gestion des lieux (à partir d’api4webtrees 1.13) : à gauche tous les lieux tels qu’ils figurent dans les événements, avec recherche, nombre d’événements et ◉ pour « coordonnées connues ». À droite le lieu sélectionné en onglets : **Personnes** (chaque individu et chaque famille avec leurs événements à cet endroit – un clic sélectionne la personne, un double-clic en fait la personne centrale), **Données** (niveaux, lieu supérieur, lieux inclus, fiche de lieu et identifiant GOV), **Notes**, **Sources**, **Médias** et **Coordonnées** avec carte. Dans webtrees, la note, l’identifiant GOV et les coordonnées d’un lieu se trouvent dans sa fiche de lieu (_LOC, GEDCOM-L) ; sans fiche, wtWin prend les coordonnées des données géographiques de webtrees ou d’un événement.

Avec le droit de modification, **Modifier** ouvre les données du lieu : identifiant GOV (avec **Chercher dans GOV**), note et coordonnées. **Rechercher les coordonnées …** interroge OpenStreetMap avec le nom du lieu ; un clic sur un résultat reprend latitude et longitude. Tout est enregistré dans la fiche de lieu (_LOC), que wtWin crée si nécessaire ; si le nom du lieu apparaît plusieurs fois dans l’arbre, les événements de ce lieu reçoivent un renvoi vers elle. Les administrateurs peuvent aussi écrire les coordonnées dans les données géographiques de webtrees – seules celles-ci sont lues par les cartes du navigateur.

## Sections : Accueil, Arbre, Photos

- **Accueil** (Ctrl+1) : message d'accueil, anniversaires à venir, modifications récentes de l'arbre, la personne de départ. Les modérateurs voient ici les modifications en attente et les acceptent ou les refusent.
- **Arbre** (Ctrl+2) : navigateur, vue de l'arbre ou famille.
- **Photos** (Ctrl+3) : toutes les images de l'arbre ; un clic ouvre la visionneuse. Si le module Sammlungen (collections) tourne sur le serveur, ses archives apparaissent aussi ici.

## Affichage

- **Apparence :** clair, sombre ou comme le système.
- **Langue :** comme le système ou fixée sur allemand, anglais, français, néerlandais ou espagnol. Prend effet immédiatement ; les libellés du serveur (types d'événements, lieux) arrivent dans la nouvelle langue au prochain rechargement. L'aide et la vérification de cohérence changent de langue elles aussi.
- **Code couleur :** colore les ancêtres de la personne de départ selon les quatre lignées des grands-parents et leurs descendants dans une cinquième couleur (d'après Mary Hill). Nécessite une personne de départ.
- **Afficher la fratrie et ses conjoints**, **Afficher les cousins** : pour la vue de l'arbre.
- **Générations :** combien de générations d'ancêtres l'arbre charge.

## Retour, suivant, historique

Chaque changement de personne centrale entre dans l'historique. **Alt+Left** revient en arrière, **Alt+Right** avance de nouveau ; l'icône **Historique** liste les dernières personnes. **Alt+Home** saute à la personne de départ que webtrees connaît pour votre compte.

## Barre d'état

En bas : serveur, compte (ou « invité ») et version du programme. **F5** recharge l'arbre, p. ex. après des modifications faites dans le navigateur.
