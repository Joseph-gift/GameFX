package com.example.gamefx;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.Objects;

public class IntroductionController {

    @FXML private StackPane rootPane;
    @FXML private Label speakerLabel;
    @FXML private Label dialogueLabel;
    @FXML private Label continueLabel;
    @FXML private ImageView characterImage;
    @FXML private VBox quizContainer;
    @FXML private Label quizQuestionLabel;
    @FXML private Button answerRougeButton;
    @FXML private Button answerBlancButton;
    @FXML private Button answerNoirButton;
    @FXML private Button answerKakiButton;

    private static final Duration CHAR_DELAY = Duration.millis(30);
    private static final String QUIZ_QUESTION =
            "De qu'elle couleur est le cheval blanc d'Henry IV ?";
    private static final String CORRECT_ANSWER = "Blanc";

    private final List<DialogueLine> dialogues = List.of(
        new DialogueLine("Chef",
            "Écoute-moi bien. Une bombe a été placée quelque part en ville, "
            + "et tout repose sur toi. Nous n'avons pas de temps à perdre. "
            + "Chaque seconde compte."),
        new DialogueLine("Chef",
            "Voici la situation : tu vas devoir résoudre une série d'énigmes. "
            + "Chacune te donnera des indices pour localiser la bombe. Le temps "
            + "presse, mais nous avons encore une chance si tu agis rapidement "
            + "et avec précision."),
        new DialogueLine("Chef",
            "Je sais que ce n'est pas facile, mais je crois en toi. Nous avons "
            + "les outils nécessaires, et tu as l'intelligence pour déchiffrer "
            + "ces énigmes. Chaque réponse correcte nous rapproche de la solution."),
        new DialogueLine("Chef",
            "Ne laisse pas la pression te faire trébucher. Résous les énigmes, "
            + "trouve l'emplacement de la bombe, et nous pourrons la désamorcer "
            + "avant qu'il ne soit trop tard. On compte sur toi. La ville compte "
            + "sur toi.")
    );

    private int currentIndex = 0;
    private Timeline typewriterTimeline;
    private boolean isTyping = false;
    private String currentFullText = "";
    private boolean quizActive = false;
    private boolean missionReady = false;
    private boolean gameStarted = false;

    @FXML
    public void initialize() {
        loadCharacterImage();
        initializeQuiz();
        continueLabel.setVisible(false);

        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.SPACE) {
                        handleSpace();
                        event.consume();
                    }
                });
            }
        });

        showDialogue(currentIndex);
    }

    private void initializeQuiz() {
        quizQuestionLabel.setText(QUIZ_QUESTION);
        quizContainer.setVisible(false);
        quizContainer.setManaged(false);
    }

    private void loadCharacterImage() {
        try {
            Image img = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("images/chef.png")));
            characterImage.setImage(img);
        } catch (Exception e) {
            System.err.println("Image chef introuvable : " + e.getMessage());
        }
    }


    private void showDialogue(int index) {
        if (index >= dialogues.size()) {
            onDialoguesFinished();
            return;
        }

        DialogueLine line = dialogues.get(index);
        speakerLabel.setText(line.speaker() + " :");
        currentFullText = line.text();
        dialogueLabel.setText("");
        continueLabel.setVisible(false);

        startTypewriter(currentFullText);
    }

    private void handleSpace() {
        if (gameStarted) {
            return;
        }
        if (missionReady) {
            startGame();
            return;
        }
        if (quizActive) {
            return;
        }
        if (isTyping) {
            skipTypewriter();
        } else {
            currentIndex++;
            showDialogue(currentIndex);
        }
    }

    private void startTypewriter(String text) {
        stopTypewriter();
        isTyping = true;

        final int length = text.length();
        final int[] pos = {0};

        typewriterTimeline = new Timeline();
        typewriterTimeline.setCycleCount(length);
        typewriterTimeline.getKeyFrames().add(new KeyFrame(CHAR_DELAY, e -> {
            pos[0]++;
            dialogueLabel.setText(text.substring(0, pos[0]));
        }));

        typewriterTimeline.setOnFinished(e -> {
            isTyping = false;
            continueLabel.setVisible(true);
        });

        typewriterTimeline.play();
    }

    private void skipTypewriter() {
        stopTypewriter();
        dialogueLabel.setText(currentFullText);
        isTyping = false;
        continueLabel.setVisible(true);
    }

    private void stopTypewriter() {
        if (typewriterTimeline != null) {
            typewriterTimeline.stop();
        }
    }

    private void onDialoguesFinished() {
        stopTypewriter();
        isTyping = false;
        quizActive = true;
        speakerLabel.setText("");
        dialogueLabel.setText("");
        continueLabel.setVisible(false);
        setQuizVisible(true);
        setAnswerButtonsDisabled(false);
    }

    @FXML
    private void onRougeAnswer() {
        handleQuizAnswer("Rouge");
    }

    @FXML
    private void onBlancAnswer() {
        handleQuizAnswer("Blanc");
    }

    @FXML
    private void onNoirAnswer() {
        handleQuizAnswer("Noir");
    }

    @FXML
    private void onKakiAnswer() {
        handleQuizAnswer("Kaki");
    }

    private void handleQuizAnswer(String selectedAnswer) {
        if (!quizActive) {
            return;
        }

        quizActive = false;
        missionReady = true;
        setAnswerButtonsDisabled(true);
        setQuizVisible(false);

        speakerLabel.setText("Chef :");
        if (CORRECT_ANSWER.equals(selectedAnswer)) {
            dialogueLabel.setText("Exact. Bonne réponse, agent.");
        } else {
            dialogueLabel.setText("Raté. La bonne réponse était \"Blanc\".");
        }
        continueLabel.setText("Appuyez sur ESPACE pour commencer la mission...");
        continueLabel.setVisible(true);
    }

    private void setQuizVisible(boolean visible) {
        quizContainer.setVisible(visible);
        quizContainer.setManaged(visible);
    }

    private void setAnswerButtonsDisabled(boolean disabled) {
        answerRougeButton.setDisable(disabled);
        answerBlancButton.setDisable(disabled);
        answerNoirButton.setDisable(disabled);
        answerKakiButton.setDisable(disabled);
    }

    private void startGame() {
        gameStarted = true;
        missionReady = false;
        System.out.println("Lancement du jeu !");
    }

    private record DialogueLine(String speaker, String text) {}
}
