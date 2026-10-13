# Archives

Si le module **Sammlungen** fonctionne sur le serveur, **Affichage › Archives** (Ctrl+9) ou l’icône **Archives** ouvre ses archives dans une fenêtre à part. L’[arbre sur ce PC](hilfe:lokal) contient déjà le module. Les archives contiennent plus que l’arbre : photos et documents ne doivent être liés à personne. Qui peut voir et modifier quoi, c’est webtrees qui le décide avec ses droits d’utilisateur.

## Disposition

- **À gauche** les collections : dossiers d’archives, collections thématiques, collections par type de média et les médias liés à aucune personne. En dessous, le nombre de fichiers du fonds libre, vers lesquels rien ne pointe dans l’arbre. C’est voulu, pas un défaut.
- **Au milieu** les fichiers de la collection choisie en grille, en trois tailles au choix en haut. Le badge sur l’image indique le nombre de personnes liées, **GEDCOM** signifie objet média sans personne, **libre** signifie fichier seul.
- **À droite** les informations du fichier choisi : description, date, personnes et mots-clés du fichier, collections, taille, chemin et qui y est lié dans l’arbre. Un clic sur un nom met la personne au centre.

Faites glisser les séparateurs pour régler la largeur des colonnes ; wtWin s’en souvient.

## Utilisation

Un clic sélectionne un fichier, **Ctrl+clic** en ajoute d’autres, **Maj+clic** toute une plage, **Ctrl+A** tous. Les flèches, **Début** et **Fin** parcourent la grille ; **Entrée** ou un double-clic ouvre : les images dans la visionneuse, les PDF dans la visionneuse PDF, le reste dans le navigateur.

Le **clic droit** propose : Ouvrir, Modifier les informations, Copier le chemin (aussi Ctrl+C), Enregistrer des copies dans un dossier et Ouvrir dans webtrees. Enregistrer des copies récupère les originaux sur ce PC ; les fichiers existants ne sont pas écrasés.

## Ajouter des fichiers

Qui a le droit de téléverser des médias dans webtrees ajoute des fichiers avec **Ajouter des fichiers …** ou les fait glisser depuis le gestionnaire de fichiers dans la fenêtre. Dossier et collection thématique sont préréglés sur la collection ouverte. Description, date, personnes et mots-clés sont facultatifs et ne sont écrits que dans les images (XMP).

wtWin téléverse l’original et ne réduit rien. Un fichier plus grand que ce que le serveur accepte est ignoré et signalé. La limite est fixée par le serveur (PHP `upload_max_filesize`). Ensuite, les fichiers ne sont liés à personne. Pour en lier un à une personne, une source ou un lieu, utilisez **Média existant › Depuis les archives** dans l’onglet **Médias**.

## Modifier les informations

Les gestionnaires de l’arbre modifient description, date, personnes et mots-clés d’une image par le clic droit, le bouton à droite ou le crayon de la visionneuse. Le module écrit les informations dans le fichier lui-même.

## Visages sur les photos de groupe

![](desktop-gesichter.jpg)


Clic droit sur une image › **Visages …** l’affiche en grand avec tous les visages marqués. Au survol, le nom apparaît au cadre ; à droite, la liste rangée par rangée depuis la gauche, et un clic sur un nom met la personne au centre. Les gestionnaires cochent **Marquer un visage**, tracent un cadre autour d’un visage et l’attribuent : à une personne de l’arbre, à un simple nom (quelqu’un de connu sans fiche) ou à « inconnu ». On déplace un cadre sélectionné en le faisant glisser, on le redimensionne à la poignée jaune, **Suppr** le retire. Les cadres sont enregistrés dans le fichier image lui-même (XMP) ; d’autres programmes comme digiKam les lisent aussi. L’image n’est pas liée à la personne – sauf si vous cochez **Lier aussi l’image à la personne choisie**. Dans la fiche, l’onglet **Médias** liste sous **Sur des photos de groupe** toutes les images où la personne est marquée.

## Cartes postales

Dans une collection de cartes postales, recto et verso forment une seule entrée (signe ↻). À droite **Retourner**, en dessous motif, transcription et note ; une date déduite apparaît comme « vers 1916 ». Un double-clic montre les deux faces dans la visionneuse ; **R** ou ↻ tourne de 90 degrés une image écrite en travers pour la lire, **Maj+R** revient (affichage seulement, rien n’est enregistré). **Modifier la carte postale …** change motif, transcription, note et date ; le module écrit les deux faces et laisse les champs inchangés.

## Gérer les collections

Les administrateurs créent une collection avec **+ Nouvelle collection** : nom, description, dossier des médias (ou aucun – c’est alors un album que l’on remplit image par image), type d’affichage, symbole, couleur, ordre, visible. Clic droit sur une collection : **Modifier la collection …**, **Masquer**/**Afficher**, **Supprimer la collection …** (les fichiers restent). Les collections masquées apparaissent pâles sous « masquée ». Clic droit sur des images : **Collections thématiques …** (une coche par album, effet immédiat, aussi pour plusieurs images), **Retirer de cette collection** et **Renommer …** (même dossier, même extension ; un objet média de l’arbre suit). **Paramètres des archives …** règle la durée du cache et la taille de page et vide le cache. Le module décide qui peut faire quoi.

## Comparaison avec webtrees

Pour les images qui sont des objets médias de l’arbre, la colonne de droite montre où fichier et webtrees diffèrent : description contre titre, personnes du fichier contre personnes liées. **→ copier dans le fichier** écrit la valeur de webtrees dans le fichier ; l’arbre ne change pas.

## Sauvegarder

Dans l’arbre sur ce PC, **Fichier › Sauvegarder l’arbre et les archives …** enregistre tout dans un fichier ZIP : la base de données avec les arbres et les collections, et tout le dossier des médias, fonds libre compris. Un fichier GEDCOM ou GEDZIP ne prend que les fichiers vers lesquels l’arbre pointe. Pour restaurer, quittez wtWin et placez le dossier `data` du fichier ZIP dans le dossier webtrees (son emplacement est indiqué sous [arbre sur ce PC](hilfe:lokal)).

Sur un serveur, c’est le serveur qui sauvegarde, par exemple nas4webtrees ou votre hébergeur.
