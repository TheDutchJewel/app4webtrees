# Fiche individuelle et modification

La fiche individuelle s'ouvre par un **double-clic** sur une personne, avec **Ctrl+E** ou par l'icône **Modifier**. Elle montre tout ce qui concerne une personne et sert aussi à la modifier.

## Présentation

Nom, dates et image en haut. En dessous, les onglets :

- **Données :** tous les événements sous forme de table (événement, date, lieu / description), plus l'âge au décès.
- **Biographie :** la chronologie avec le mariage, les naissances des enfants et l'âge à chaque événement. Elle liste aussi les événements où la personne était parrain, marraine ou témoin (« Marraine à : Baptême de … ») ; un clic mène à l'enfant ou au couple.
- **Parrains, marraines et témoins :** sous le baptême et le mariage apparaît une ligne « Parrains et marraines : » ou « Témoins : » – dans la chronologie, dans la zone de détail de la table des données, dans la vue famille et sur les fiches. Les parrains et marraines ayant leur propre enregistrement sont soulignés et cliquables, les autres (issus d'une note « Paten: … » ou du champ GEDCOM-L _GODP) apparaissent en texte. ⓘ déplie une note sur le parrain ou la marraine, l'icône de source ouvre la source. Les parrains et marraines vivants que vous n'êtes pas autorisé à voir apparaissent seulement comme « Privé ». Sous la table des données, la section **Rôles de parrain, marraine et témoin** liste tous les baptêmes et mariages où la personne était parrain, marraine ou témoin ; un clic sur le titre la replie. Les deux nécessitent api4webtrees 1.11 ou plus récent ; avec un module plus ancien, la note sur les parrains reste simplement une note.
- **Type de mariage :** le mariage civil et le mariage religieux apparaissent comme des événements distincts avec leur type ; les listes et la vérification utilisent le mariage civil lorsque les deux existent.
- **Parents/fratrie** : parents et fratrie, y compris demi-frères et demi-sœurs. Un clic passe à cette personne.
- **Conjoints/enfants** : les unions à gauche, les enfants de l'union sélectionnée à droite, en dessous ses événements (mariage, divorce, résidence …) à ajouter, modifier et supprimer. Un double-clic sur un conjoint ou un enfant affiche sa fiche ; **+** ajoute un conjoint ou un enfant de cette union.
- **Nom** : prénoms, nom de famille et suffixe du nom ont chacun leur champ lors de la modification.
- **Simple / Complet** (en bas de la fiche) : « Simple » affiche dans l'onglet Données un formulaire avec nom, naissance, baptême, religion, profession, mariage par union, décès et inhumation à remplir directement ; « Complet » affiche la table de tous les événements avec l'âge et des marques pour note et source (cliquez sur un en-tête de colonne pour trier).
  Seul ce que vous avez modifié est enregistré ; les sources, notes et autres détails de l'événement sont conservés. Si un événement existe plusieurs fois, le formulaire modifie le premier. Une date que le programme ne sait pas interpréter (« printemps 1850 ») est enregistrée comme texte de date. L'enregistrement a lieu quand vous quittez un champ (Tab ou clic ailleurs), comme partout dans le programme. Une date non valide reste en rouge et n'est pas envoyée tant que vous ne l'avez pas corrigée ou choisi « Enregistrer quand même en texte » ; ce n'est qu'alors que la fermeture ou la sortie du programme demande d'abord confirmation.
- **Notes**, **Sources**, **Médias**.
- **Carte :** les lieux de vie sous forme de liste avec des liens vers OpenStreetMap.

## Feuilleter

**Page Up** et **Page Down** passent à la personne précédente ou suivante de la liste, **Ctrl+Home** et **Ctrl+End** à la première et à la dernière. **Esc** ferme la fenêtre.

## Modifier

La modification nécessite les droits de modification dans webtrees. Les changements partent immédiatement vers webtrees ; selon les réglages de l'arbre, ils s'appliquent tout de suite ou attendent l'approbation d'un modérateur.

- **Modifier un événement :** double-cliquez sur la ligne (ou Modifier). La date et le lieu se choisissent au lieu de se taper : exacte, vers, avant, après, entre ; jour, mois, année. Les lieux de l'arbre sont proposés pendant la saisie.
- **Ajouter un événement**, y compris les événements familiaux comme le mariage.
- **Ajouter un parent** (Ctrl+N) : créer parents, conjoint, enfant ou frère/sœur avec nom et premiers événements. Dans le navigateur, cette commande figure aussi dans le menu du clic droit.
- **Saisir parrains, marraines et témoins :** sélectionnez le baptême ou le mariage dans la table des données et cliquez sur **Modifier les parrains et marraines …** ou **Modifier les témoins …** dans la zone de détail en dessous (pour les mariages, aussi dans l'onglet Conjoints/enfants). Ordonnez la liste avec les flèches, ✕ retire une entrée. **Personne de l'arbre …** cherche par nom et lie la personne ; **Sans enregistrement …** ajoute quelqu'un qui n'a pas d'entrée propre – tel qu'inscrit dans le registre paroissial : « Friedrich Plate, cultivateur à Celle ». Le rôle (parrain/marraine, témoin ou un autre comme « sage-femme ») et une note sur la personne se trouvent sous la liste. Les parrains et marraines liés sont enregistrés comme le fait webtrees, les personnes sans enregistrement dans les champs GEDCOM-L _GODP et _WITN ; les sources d'un parrain ou d'une marraine sont conservées. Les parrains et marraines enregistrés seulement sur la personne (exports anciens) sont déplacés dans le baptême d'une simple coche. Nécessite api4webtrees 1.12 ou plus récent.
- **Type de mariage :** dans la boîte de dialogue d'un mariage, **Type** permet de choisir entre non précisé, civil, religieux, partenariat enregistré et union libre. Une seconde cérémonie (comme le mariage religieux après le civil) s'ajoute comme un autre événement « Mariage » de l'union.
- **Supprimer :** un événement ou la personne entière, après confirmation.
- **Notes et médias :** dans l’onglet **Notes**, vous ajoutez des notes générales sur la personne (**+ Nouvelle note**), les modifiez et les supprimez ; les notes des événements figurent en dessous, en lecture. Dans l’onglet **Médias**, vous joignez photos et scans (fichier, média existant, élément des archives ou simplement glissé depuis le gestionnaire de fichiers), changez titre et type et détachez des liens ; un clic sur une image l’ouvre. Nécessite api4webtrees au niveau d’API 23.
- **Modifier dans webtrees :** tout ce que le programme ne sait pas faire lui-même (par exemple les notes partagées), vous le faites sur la page de la personne dans le navigateur. Voir [webtrees dans le navigateur](hilfe:webtrees).

## Photos

**Ajouter une photo** choisit un fichier sur l'ordinateur et le joint à la personne ; l'image est réduite à la limite d'envoi du serveur. Un clic sur une image ouvre la visionneuse.

## Imprimer et partager

- **Fichier › Imprimer la fiche individuelle …** (Ctrl+P) et **Fiche individuelle en PDF …**
- **Personne › Copier le texte de la personne** (Ctrl+Shift+C) place tous les détails sous forme de texte dans le presse-papiers, p. ex. pour un e-mail ou un traitement de texte.
- Autres sorties : [Listes](hilfe:listen) (fiche individuelle sous forme de liste), [Tableaux](hilfe:tafeln), [Livres](hilfe:buecher).
