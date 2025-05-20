Spécification Fonctionnelle : Développement d’un Jeu Blokus en Spring MVC
 1. Introduction
 Le jeu Blokus est un jeu de stratégie où les joueurs placent des pièces sur un plateau en respectant
 certaines règles. L’objectif de cette spécification fonctionnelle est de détailler les fonctionnalités
 attendues pour développer une version web multijoueur du jeu Blokus en utilisant le framework
 Spring MVC.
 L’application doit permettre à des joueurs de s’inscrire, se connecter, rejoindre des parties, jouer en
 temps réel et consulter leurs statistiques.
 2. Objectifs
 • Fournir une interface intuitive pour jouer au Blokus en ligne.
 • Assurer la conformité avec les règles officielles du jeu Blokus.
 • Permettre une expérience multijoueur en temps réel avec un maximum de 4 joueurs par
 partie.
 3. Fonctionnalités principales
 3.1 Gestion des utilisateurs
 • Inscription :
 • Les joueurs doivent pouvoir créer un compte avec un nom d’utilisateur unique, une
 adresse email et un mot de passe.
 • Validation des données utilisateur (email valide, mot de passe sécurisé, unicité du
 nom d’utilisateur).
 • Connexion :
 • Les joueurs doivent pouvoir se connecter à l’aide de leur nom d’utilisateur ou adresse
 email et leur mot de passe.
 • Utilisation de Spring Security pour la gestion des sessions et l’authentification.
 • Gestion du profil :
 • Les joueurs doivent pouvoir modifier leur nom d’utilisateur, email et mot de passe.
 • Affichage des statistiques du joueur : nombre de parties jouées, gagnées, et ratio de
 victoires.
 3.2 Gestion des parties
 • Création de partie :
 • Un joueur peut créer une partie avec les options suivantes :
 • Nom de la partie.
 • Nombre de joueurs (2, 3 ou 4). Attention, c’est le nombre de joueurs humains
 attendus. Au final il y aura bien 4 joueurs mais les joueurs humains
 manquants devront être remplacés par une IA.
• Mode de jeu (classique ou chronométré).
 • Rejoindre une partie :
 • Les joueurs peuvent rejoindre une partie en cours d’attente.
 • Les parties sont affichées dans un tableau avec leur état (en attente, en cours,
 terminée).
 • Déroulement de la partie :
 • Le plateau de jeu est un quadrillage de 20x20.
 • Chaque joueur dispose de 21 pièces de formes différentes.
 • Les règles du Blokus doivent être respectées :
 • La première pièce de chaque joueur doit toucher un coin de sa zone de départ.
 • Une pièce doit toucher au moins un coin d’une autre pièce du même joueur,
 mais pas ses côtés.
 • Les joueurs jouent à tour de rôle jusqu’à ce qu’ils ne puissent plus placer de
 pièces.
 3.3 Interaction avec le plateau de jeu
 • Affichage :
 • Le plateau de jeu est affiché sous forme de grille, avec une visualisation des pièces
 placées par chaque joueur.
 • Chaque pièce est colorée en fonction de son propriétaire.
 • Placement des pièces :
 • Les joueurs peuvent sélectionner une pièce à placer via une interface intuitive.
• Une prévisualisation de la pièce doit être affichée avant son placement pour vérifier
 sa validité.
 • Validation des règles :
 • Les règles du jeu sont vérifiées côté serveur lors du placement des pièces.
 • En cas de non-conformité, un message d’erreur est retourné au joueur.
 3.4 Fin de partie
 • Calcul des scores :
 • À la fin de la partie, les scores sont calculés :
 • Chaque carré non utilisé compte comme -1 point.
 • Bonus de 15 points si toutes les pièces sont placées.
 • Bonus de 5 points supplémentaires si la dernière pièce placée est un carré de
 taille 1.
 • Classement :
 • Les joueurs sont classés en fonction de leur score final.
 • Historique :
 • La partie est sauvegardée dans l’historique des joueurs.
 4. Technologies
 • Backend :
 • Springboot 3.3
 • Spring MVC (technologie à découvrir)
 • Spring Data JPA pour la persistance (technologie à découvrir)
 • H2, MySql ou Mongo pour la base de données. Le choix devra être justifié
 • Frontend :
 • Thymeleaf
 • Sécurité :
 • Spring Security pour la gestion des utilisateurs et des sessions.
 5. Livrables
 • Code source de l’application.
 • Un rapport final sur l’application, le développement, les choix, les difficultés, le bilan. 
• Tests unitaires et d’intégration (minimum 80% de couverture de code). Un soin particulier
 devra être apporté au test d’application SpringMVC. Ne l’ayant pas étudié, vous devrez
 rechercher des techniques cohérentes avec une approche de test structurel. 
• Une démonstration de l’application en ligne par une vidéo.
 • Un rapport pédagogique sur ce qu’il faut savoir en Spring MVC, Spring Security et Spring
 Data JPA. Dans ce rapport, j’attends comme un tuto-cours qui permet d’illustrer toutes les
 facettes importantes des technologies dont vous avez eu besoin. Il sera aussi important de
 montrer que vous avez compris comment tout cela fonctionne. 