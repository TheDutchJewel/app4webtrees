# Table des personnes

La **table des personnes** montre toutes les personnes de l'arbre généalogique en colonnes : ID, nom, sexe, naissance, lieu de naissance, décès, lieu de décès et profession. Ouvrez-la par **Affichage › Table des personnes** (Ctrl+4) ou par l'icône **Liste**. La fenêtre peut rester ouverte à côté de la fenêtre principale.

## Trier

Cliquez sur un en-tête de colonne pour trier selon cette colonne, cliquez de nouveau pour inverser l'ordre. Les dates sont triées selon le calendrier et non selon leur texte ; les personnes sans valeur viennent toujours en dernier.

## Filtrer

Sous chaque en-tête de colonne se trouve un champ de filtre. Plusieurs filtres s'appliquent ensemble.

- **Texte :** trouve des fragments, sans tenir compte des majuscules et minuscules – « hann » trouve Hannover.
- **1800-1850** sous naissance ou décès : années de–à ; également **-1850** ou **1800-**.
- `!` – le champ est vide. C'est ainsi que vous trouvez les lacunes, par exemple toutes les personnes sans lieu de naissance.
- `*` – le champ est rempli.

**Effacer les filtres** vide tous les champs. La ligne du haut indique combien de personnes correspondent.

Si la naissance manque, le baptême est affiché (avec ~) ; si le décès manque, l'inhumation (avec □). Les personnes privées ne sont pas listées.

## Travailler avec la table

- **Clic :** sélectionne la personne ; le panneau de la personne dans la fenêtre principale l'affiche.
- **Double-clic** ou **Enter :** mettre la personne au centre.
- **Touches fléchées, Page Up/Down :** se déplacer dans la table.
- **Clic droit :** Mettre au centre, Modifier la personne, Ouvrir dans webtrees.
- **Enregistrer en CSV …** écrit les lignes actuellement affichées dans un fichier pour les tableurs.

La table a besoin de l'arbre entier d'un coup ; le serveur doit utiliser api4webtrees 1.9 ou plus récent (voir [Listes](hilfe:listen)).
