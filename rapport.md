
4. Choix de la Base de Données : H2
4.1. Pourquoi H2 ?
Facilité d’utilisation : H2 est une base de données embarquée, idéale pour le développement et les tests.
Aucune installation requise : Elle fonctionne en mémoire ou en mode fichier, ce qui simplifie la configuration.
Console web intégrée : Permet de visualiser et manipuler les données facilement pendant le développement.
4.2. Exemple de Configuration

 # application.properties
   spring.datasource.url=jdbc:h2:mem:blokusdb
   spring.datasource.driverClassName=org.h2.Driver
   spring.datasource.username=sa
   spring.datasource.password=
   spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
   spring.h2.console.enabled=true
Explication :
Cette configuration permet d’utiliser H2 en mémoire, ce qui accélère les tests et le développement. La console H2 est activée pour faciliter le debug

---

# 3. Fonctionnalités principales – Rapport détaillé avec extraits de code réels

---

## 3.1 Gestion des utilisateurs

### 1. Inscription

**But :** Permettre à un nouvel utilisateur de créer un compte avec un nom d’utilisateur unique, un email valide et un mot de passe sécurisé.

**Extrait de code – Contrôleur d’inscription :**
```java
@PostMapping("/register")
public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto userDto,
                          BindingResult result, Model model) {
    if (userService.existsByUsername(userDto.getUsername())) {
        result.rejectValue("username", "error.username", "Ce nom d'utilisateur est déjà utilisé");
    }
    if (userService.existsByEmail(userDto.getEmail())) {
        result.rejectValue("email", "error.email", "Cette adresse email est déjà utilisée");
    }
    if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
        result.rejectValue("confirmPassword", "error.password", "Les mots de passe ne correspondent pas");
    }
    if (result.hasErrors()) {
        return "auth/register";
    }
    userService.register(userDto);
    return "redirect:/login?success";
}
```
**Explication :**  
Ce contrôleur gère l’inscription. Il vérifie l’unicité du nom d’utilisateur et de l’email, la correspondance des mots de passe, puis délègue la création à la couche service.

---

### 2. Validation des données utilisateur

**Extrait de code – DTO avec validation :**
```java
public class UserRegistrationDto {
    @NotEmpty(message = "Le nom d'utilisateur ne peut pas être vide")
    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit avoir entre 3 et 50 caractères")
    private String username;

    @NotEmpty(message = "L'email ne peut pas être vide")
    @Email(message = "Veuillez fournir une adresse email valide")
    private String email;

    @NotEmpty(message = "Le mot de passe ne peut pas être vide")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    @NotEmpty(message = "La confirmation du mot de passe ne peut pas être vide")
    private String confirmPassword;
    // Getters et setters...
}
```
**Explication :**  
Les annotations de validation garantissent que les champs sont correctement remplis avant d’être traités par le backend.

---

### 3. Connexion

**Extrait de code – Configuration Spring Security :**
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/register", "/login", "/css/**", "/js/**", "/images/**", "/webjars/**", "/h2-console/**").permitAll()
            .anyRequest().authenticated())
        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/home", true)
            .permitAll())
        .logout(logout -> logout
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
            .logoutSuccessUrl("/login?logout")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll())
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/h2-console/**"))
        .headers(headers -> headers
            .frameOptions(frameOptions -> frameOptions.sameOrigin()));
    return http.build();
}
```
**Explication :**  
Cette configuration permet à tous d’accéder aux pages d’inscription et de connexion, mais protège le reste de l’application.

---

### 4. Gestion du profil et statistiques

**Extrait de code – Entité User :**
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String password;
    private int gamesPlayed;
    private int gamesWon;
    // ...
}
```
**Explication :**  
L’entité User stocke les informations de profil et les statistiques de jeu.

---

## 3.2 Gestion des parties

### 1. Création de partie

**Extrait de code – Contrôleur de création de partie :**
```java
@PostMapping("/games/create")
public String createGame(@Valid @ModelAttribute("gameCreateDto") GameCreateDto gameCreateDto,
        BindingResult bindingResult, Model model,
        @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
        RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
        return "game/create";
    }
    User user = userService.findByUsername(userDetails.getUsername());
    Game game = gameService.createGame(gameCreateDto, user);
    redirectAttributes.addFlashAttribute("successMessage", "Partie créée avec succès!");
    return "redirect:/games/" + game.getId();
}
```
**Extrait de code – DTO de création de partie :**
```java
public class GameCreateDto {
    @NotEmpty(message = "Le nom de la partie est obligatoire")
    @Size(min = 3, max = 50, message = "Le nom doit contenir entre 3 et 50 caractères")
    private String name;
    @Min(value = 2, message = "Le nombre minimum de joueurs est 2")
    @Max(value = 4, message = "Le nombre maximum de joueurs est 4")
    private int maxPlayers;
    private boolean timedMode;
    // ...
}
```
**Explication :**  
Le contrôleur reçoit les paramètres de la partie et délègue la création à la couche service, qui gère aussi l’ajout des IA si besoin.

---

### 2. Rejoindre une partie

**Extrait de code – Contrôleur :**
```java
@PostMapping("/games/{id}/join")
public String joinGame(@PathVariable Long id, 
        @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
        RedirectAttributes redirectAttributes) {
    User user = userService.findByUsername(userDetails.getUsername());
    gameService.joinGame(id, user);
    redirectAttributes.addFlashAttribute("successMessage", "Vous avez rejoint la partie!");
    return "redirect:/games/" + id;
}
```
**Explication :**  
Le joueur rejoint une partie identifiée par son ID. Le service vérifie que la partie est en attente et ajoute le joueur.

---

### 3. Affichage des parties

**Extrait de code – Contrôleur :**
```java
@GetMapping("/games")
public String listGames(Model model, @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {
    User user = userService.findByUsername(userDetails.getUsername());
    List<Game> availableGames = gameService.findAvailableGames();
    List<Game> userGames = gameService.findUserGames(user.getId());
    model.addAttribute("availableGames", availableGames);
    model.addAttribute("userGames", userGames);
    model.addAttribute("user", user);
    return "game/list";
}
```
**Explication :**  
Le contrôleur récupère toutes les parties disponibles et celles de l’utilisateur pour affichage.

---

### 4. Déroulement de la partie

**Extrait de code – Entité Game :**
```java
@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    private GameStatus status;
    @Enumerated(EnumType.STRING)
    private GameMode mode;
    @Column(name = "expected_players")
    private int expectedPlayers;
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameUser> players = new ArrayList<>();
    // ...
}
```
**Explication :**  
Le plateau est représenté par une entité Game, qui gère les joueurs, le statut, le mode, etc.

---

### 5. Règles du Blokus et validation des coups

**Extrait de code – Service de logique de jeu :**
```java
@Override
@Transactional
public boolean placePiece(Long gameId, Long userId, String pieceId, String pieceColor, 
                         int x, int y, Integer rotation, Boolean flipped) {
    // ... validation des règles Blokus ...
    // Vérifie la position initiale, le contact par coin, et l’absence de contact par côté
    // Si le coup est valide, met à jour l’état du jeu
}
```
**Explication :**  
Avant de placer une pièce, le service vérifie que le coup est légal (contact par coin, pas par côté, etc.).

---

## 3.3 Interaction avec le plateau de jeu

### 1. Affichage du plateau

**Extrait de code – Vue Thymeleaf (exemple générique) :**
```html
<table>
  <tr th:each="row : ${game.board}">
    <td th:each="cell : ${row}" th:style="'background:' + ${cell.color}"></td>
  </tr>
</table>
```
**Explication :**  
Chaque case du plateau est colorée selon le propriétaire de la pièce, ce qui permet de visualiser l’état du jeu.

---

### 2. Placement et prévisualisation des pièces

**Extrait de code – Service :**
```java
public boolean placePiece(Long gameId, Long userId, String pieceId, String pieceColor, 
                         int x, int y, int rotation, boolean flipped) {
    // Vérifie la validité du coup, met à jour le plateau et passe au joueur suivant
}
```
**Explication :**  
Le backend vérifie la validité du coup et met à jour l’état du jeu.

---

## 3.4 Fin de partie

### 1. Calcul des scores et classement

**Extrait de code – Service :**
```java
@Override
@Transactional
public Game calculateScores(Long gameId) {
    // Calcule le score de chaque joueur selon les règles Blokus
    // Met à jour les scores dans la base
}
```
**Explication :**  
À la fin de la partie, le service calcule les scores et met à jour le classement.

---

### 2. Historique et statistiques

**Extrait de code – DTO de statistiques :**
```java
public class GameStatisticsDto {
    private Long gameId;
    private String gameName;
    private LocalDateTime dateCompleted;
    private String winnerUsername;
    private List<PlayerScoreDto> playerScores;
    // ...
}
```
**Explication :**  
Les statistiques de chaque partie sont stockées et consultables par l’utilisateur.

---