# Java Escape

Jeu JavaFX dans lequel le joueur doit resoudre des enigmes pour localiser et desamorcer une bombe cachee en ville.

## Apercu

Le jeu se deroule en deux phases :

1. **Briefing** -- Un dialogue d'introduction avec le Chef explique la situation via un effet machine a ecrire. Le joueur avance dans les dialogues avec la touche ESPACE.
2. **Quiz** -- Une serie de 10 questions a choix multiples (recuperees depuis l'API Open Trivia Database). Chaque reponse affiche un feedback visuel immediat (vert = correct, rouge = incorrect) ainsi qu'un score en temps reel.

## Prerequis

- **Java 21** ou superieur
- **Maven 3.8+**

## Lancement

### Depuis IntelliJ IDEA

Executer la classe `Launcher`.

### Depuis le terminal

```bash
mvn clean javafx:run
```

## Structure du projet

```
src/main/java/com/example/gamefx/
    GameApplication.java        # Point d'entree JavaFX (charge la vue FXML)
    Launcher.java               # Classe main (contourne les restrictions du module JavaFX)
    IntroductionController.java # Controleur : dialogues, typewriter, quiz, feedback

src/main/resources/com/example/gamefx/
    introduction-view.fxml      # Vue FXML (dialogue + quiz)
    style.css                   # Styles (theme sombre, boutons, feedback)
    images/
        chef.png                # Image du personnage Chef
```


## Fonctionnalites

- Dialogues avec effet machine a ecrire (`javafx.animation.Timeline`)
- Saut du dialogue en appuyant sur ESPACE
- Quiz a choix multiples charge depuis l'API OpenTDB
- Feedback visuel : bonne reponse en vert, mauvaise en rouge avec affichage de la bonne reponse
- Score affiche en temps reel pendant le quiz

