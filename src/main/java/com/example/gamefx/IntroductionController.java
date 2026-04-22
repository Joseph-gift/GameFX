package com.example.gamefx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class IntroductionController {

    @FXML private StackPane rootPane;
    @FXML private Label speakerLabel;
    @FXML private Label dialogueLabel;
    @FXML private Label continueLabel;
    @FXML private Button quitButton;
    @FXML private ImageView characterImage;
    @FXML private VBox quizContainer;
    @FXML private Label quizQuestionLabel;
    @FXML private Label quizScoreLabel;
    @FXML private Label quizFeedbackLabel;
    @FXML private Button answerRougeButton;
    @FXML private Button answerBlancButton;
    @FXML private Button answerNoirButton;
    @FXML private Button answerKakiButton;

    private static final Duration CHAR_DELAY = Duration.millis(30);
    private static final Duration FEEDBACK_DELAY = Duration.millis(1500);
    private static final int QUIZ_QUESTION_COUNT = 10;
    private static final String TRIVIA_API_URL =
            "https://opentdb.com/api.php?amount=50&type=multiple&encode=url3986";

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
    private int currentQuestionIndex = 0;
    private int correctAnswersCount = 0;
    private List<QuizQuestion> quizQuestions = List.of();
    private String quizLoadErrorMessage;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        loadCharacterImage();
        initializeQuiz();
        setQuitButtonVisible(false);
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

        preloadQuizQuestionsAsync();
        showDialogue(currentIndex);
    }

    private void initializeQuiz() {
        quizQuestionLabel.setText("Chargement des questions...");
        quizContainer.setVisible(false);
        quizContainer.setManaged(false);
        setAnswerButtonsDisabled(true);
    }

    private void preloadQuizQuestionsAsync() {
        CompletableFuture
                .supplyAsync(this::fetchRandomQuizQuestions)
                .whenComplete((questions, throwable) -> Platform.runLater(() -> {
                    if (throwable != null) {
                        onQuizLoadFailed(throwable);
                        return;
                    }

                    quizQuestions = questions;
                    currentQuestionIndex = 0;
                    correctAnswersCount = 0;

                    if (quizActive) {
                        showCurrentQuizQuestion();
                    }
                }));
    }

    private void onQuizLoadFailed(Throwable throwable) {
        Throwable cause = throwable;
        if (throwable instanceof CompletionException completionException
                && completionException.getCause() != null) {
            cause = completionException.getCause();
        }

        quizLoadErrorMessage = "Impossible de charger les questions du quiz depuis l'API.";
        quizQuestionLabel.setText(quizLoadErrorMessage);
        setAnswerButtonsDisabled(true);
        System.err.println("Erreur chargement quiz : " + cause.getMessage());

        if (quizActive) {
            setQuizVisible(true);
            continueLabel.setVisible(false);
            setQuitButtonVisible(true);
        }
    }

    private List<QuizQuestion> fetchRandomQuizQuestions() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TRIVIA_API_URL))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Statut HTTP inattendu : " + response.statusCode());
            }

            JsonObject payload = JsonParser.parseString(response.body()).getAsJsonObject();
            int responseCode = payload.get("response_code").getAsInt();
            if (responseCode != 0) {
                throw new IllegalStateException("L'API OpenTDB a répondu avec le code : " + responseCode);
            }

            JsonArray results = payload.getAsJsonArray("results");
            List<QuizQuestion> parsedQuestions = new ArrayList<>();
            for (JsonElement resultElement : results) {
                JsonObject result = resultElement.getAsJsonObject();
                if (!"multiple".equals(result.get("type").getAsString())) {
                    continue;
                }

                String question = decodeTriviaText(result.get("question").getAsString());
                String correctAnswer = decodeTriviaText(result.get("correct_answer").getAsString());

                JsonArray incorrectAnswersRaw = result.getAsJsonArray("incorrect_answers");
                if (incorrectAnswersRaw.size() != 3) {
                    continue;
                }

                List<String> answers = new ArrayList<>();
                answers.add(correctAnswer);
                for (JsonElement incorrectAnswer : incorrectAnswersRaw) {
                    answers.add(decodeTriviaText(incorrectAnswer.getAsString()));
                }
                Collections.shuffle(answers);

                parsedQuestions.add(new QuizQuestion(question, correctAnswer, List.copyOf(answers)));
            }

            if (parsedQuestions.size() < QUIZ_QUESTION_COUNT) {
                throw new IllegalStateException(
                        "Pas assez de questions reçues : " + parsedQuestions.size() + " < " + QUIZ_QUESTION_COUNT);
            }

            Collections.shuffle(parsedQuestions);
            return List.copyOf(parsedQuestions.subList(0, QUIZ_QUESTION_COUNT));
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de récupérer le quiz OpenTDB", e);
        }
    }

    private String decodeTriviaText(String encodedText) {
        return URLDecoder.decode(encodedText, StandardCharsets.UTF_8);
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
        speakerLabel.setText("Chef :");
        dialogueLabel.setText("Dernier contrôle avant la mission.");
        continueLabel.setVisible(false);
        setQuitButtonVisible(false);
        setQuizVisible(true);

        if (quizLoadErrorMessage != null) {
            quizQuestionLabel.setText(quizLoadErrorMessage);
            setAnswerButtonsDisabled(true);
            setQuitButtonVisible(true);
            return;
        }

        if (quizQuestions.isEmpty()) {
            quizQuestionLabel.setText("Chargement des questions...");
            setAnswerButtonsDisabled(true);
            return;
        }

        showCurrentQuizQuestion();
    }

    @FXML
    private void onRougeAnswer() {
        handleQuizAnswer(answerRougeButton.getText());
    }

    @FXML
    private void onBlancAnswer() {
        handleQuizAnswer(answerBlancButton.getText());
    }

    @FXML
    private void onNoirAnswer() {
        handleQuizAnswer(answerNoirButton.getText());
    }

    @FXML
    private void onKakiAnswer() {
        handleQuizAnswer(answerKakiButton.getText());
    }

    private void handleQuizAnswer(String selectedAnswer) {
        if (!quizActive || quizQuestions.isEmpty()) {
            return;
        }

        setAnswerButtonsDisabled(true);

        QuizQuestion currentQuestion = quizQuestions.get(currentQuestionIndex);
        boolean isCorrect = currentQuestion.correctAnswer().equals(selectedAnswer);

        if (isCorrect) {
            correctAnswersCount++;
        }

        highlightAnswers(currentQuestion.correctAnswer(), selectedAnswer, isCorrect);
        updateScoreLabel();

        PauseTransition pause = new PauseTransition(FEEDBACK_DELAY);
        pause.setOnFinished(e -> {
            clearAnswerStyles();
            quizFeedbackLabel.setText("");

            currentQuestionIndex++;
            if (currentQuestionIndex < quizQuestions.size()) {
                showCurrentQuizQuestion();
            } else {
                finishQuiz();
            }
        });
        pause.play();
    }

    private void highlightAnswers(String correctAnswer, String selectedAnswer, boolean isCorrect) {
        List<Button> buttons = List.of(answerRougeButton, answerBlancButton, answerNoirButton, answerKakiButton);

        for (Button btn : buttons) {
            btn.getStyleClass().removeAll("quiz-answer-correct", "quiz-answer-incorrect", "quiz-answer-disabled");

            if (btn.getText().equals(correctAnswer)) {
                btn.getStyleClass().add("quiz-answer-correct");
            } else if (btn.getText().equals(selectedAnswer)) {
                btn.getStyleClass().add("quiz-answer-incorrect");
            } else {
                btn.getStyleClass().add("quiz-answer-disabled");
            }
        }

        quizFeedbackLabel.getStyleClass().removeAll("quiz-feedback-correct", "quiz-feedback-incorrect");
        if (isCorrect) {
            quizFeedbackLabel.setText("Bonne réponse !");
            quizFeedbackLabel.getStyleClass().add("quiz-feedback-correct");
        } else {
            quizFeedbackLabel.setText("Mauvaise réponse. La bonne réponse était : " + correctAnswer);
            quizFeedbackLabel.getStyleClass().add("quiz-feedback-incorrect");
        }
    }

    private void clearAnswerStyles() {
        List<Button> buttons = List.of(answerRougeButton, answerBlancButton, answerNoirButton, answerKakiButton);
        for (Button btn : buttons) {
            btn.getStyleClass().removeAll("quiz-answer-correct", "quiz-answer-incorrect", "quiz-answer-disabled");
        }
    }

    private void updateScoreLabel() {
        quizScoreLabel.setText("Score : " + correctAnswersCount + "/" + (currentQuestionIndex + 1));
    }

    private void showCurrentQuizQuestion() {
        QuizQuestion question = quizQuestions.get(currentQuestionIndex);
        List<String> answers = question.answers();
        if (answers.size() != 4) {
            throw new IllegalStateException("Chaque question doit avoir exactement 4 réponses.");
        }

        clearAnswerStyles();
        quizFeedbackLabel.setText("");

        String questionText = question.question() == null ? "" : question.question().trim();
        if (questionText.isEmpty()) {
            questionText = "(Question API indisponible)";
        }
        quizQuestionLabel.setText(
                "Question " + (currentQuestionIndex + 1) + "/" + quizQuestions.size() + " : " + questionText);
        quizScoreLabel.setText(currentQuestionIndex > 0
                ? "Score : " + correctAnswersCount + "/" + currentQuestionIndex
                : "");
        answerRougeButton.setText(answers.get(0));
        answerBlancButton.setText(answers.get(1));
        answerNoirButton.setText(answers.get(2));
        answerKakiButton.setText(answers.get(3));
        setAnswerButtonsDisabled(false);
    }

    private void finishQuiz() {
        quizActive = false;
        missionReady = true;
        setAnswerButtonsDisabled(true);
        setQuizVisible(false);

        speakerLabel.setText("Chef :");
        dialogueLabel.setText("Quiz terminé. Score : " + correctAnswersCount + "/" + quizQuestions.size() + ".");
        continueLabel.setText("Appuyez sur ESPACE pour commencer la mission...");
        continueLabel.setVisible(true);
        setQuitButtonVisible(true);
    }

    @FXML
    private void onQuit() {
        if (rootPane.getScene() != null && rootPane.getScene().getWindow() instanceof Stage stage) {
            stage.close();
        }
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

    private void setQuitButtonVisible(boolean visible) {
        quitButton.setVisible(visible);
        quitButton.setManaged(visible);
    }

    private void startGame() {
        gameStarted = true;
        missionReady = false;
        setQuitButtonVisible(false);
        System.out.println("Lancement du jeu !");
    }

    private record DialogueLine(String speaker, String text) {}
    private record QuizQuestion(String question, String correctAnswer, List<String> answers) {}
}
