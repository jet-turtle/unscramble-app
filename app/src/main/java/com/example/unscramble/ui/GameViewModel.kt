package com.example.unscramble.ui

import androidx.lifecycle.ViewModel
import com.example.unscramble.data.Lang
import com.example.unscramble.data.MAX_NO_OF_WORDS
import com.example.unscramble.data.SCORE_INCREASE
import com.example.unscramble.data.allWordsEn
import com.example.unscramble.data.allWordsRu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel(
    private val isTesting: Boolean = false
) : ViewModel() {

    // Game UI state
    // Backing property to avoid state updates from other classes
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private lateinit var currentWord: String
    // Set of words used in the game
    private var usedWords: MutableSet<String> = mutableSetOf()

    init {
        if (!isTesting) {
            resetGame()
        }
    }

    private fun pickRandomWordAndShuffle(): String {
        var allWords: Set<String> = when (_uiState.value.lang) {
            Lang.RU -> allWordsRu.map { it.lowercase() }.toSet()
            Lang.EN -> allWordsEn.map { it.lowercase() }.toSet()
        }
        when {
            allWords.size == 0 -> allWords = setOf("test", "empty")
            usedWords.size == allWords.size -> usedWords.clear()
        }
        // Continue picking up a new random word until you get one that hasn't been used before
        currentWord = allWords.random()
        if (usedWords.contains(currentWord)) {
            return pickRandomWordAndShuffle()
        } else {
            usedWords.add(currentWord)
            return shuffleCurrentWord(currentWord)
        }
    }

    private fun shuffleCurrentWord(word: String): String {
        val tempWord = word.toCharArray()
        // Scramble the word
        tempWord.shuffle()
        while (String(tempWord).equals(word)) {
            tempWord.shuffle()
        }
        return String(tempWord)
    }

//    var userGuess by mutableStateOf("")
//        private set

    fun updateUserGuess(guessedWord: String){
        _uiState.update { currentState ->
            currentState.copy(
                userGuess = guessedWord
            )
        }
    }

    private fun updateGameState(updatedScore: Int) {
        if (_uiState.value.currentWordCount == MAX_NO_OF_WORDS) {
            // Last round in the game, update isGameOver to true, don't pick a new word
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = false,
                    score = updatedScore,
                    isGameOver = true
                )
            }
        } else {
            // Normal round in the game
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = false,
                    currentScrambledWord = pickRandomWordAndShuffle(),
                    score = updatedScore,
                    currentWordCount = currentState.currentWordCount.inc(),
                )
            }
        }
    }

    fun checkUserGuess() {
        if (_uiState.value.userGuess.equals(currentWord, ignoreCase = true)) {
            val updatedScore = _uiState.value.score.plus(SCORE_INCREASE)
            updateGameState(updatedScore)
        } else {
            // User's guess is wrong, show an error
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = true
                )
            }
        }
        // Reset user guess
        updateUserGuess("")
    }

    fun skipWord() {
        updateGameState(_uiState.value.score)
        // Reset user guess
        updateUserGuess("")
    }

    fun setLang(lang: Lang) {
        _uiState.update { currentState ->
            currentState.copy(
                lang = lang
            )
        }
        _uiState.update { currentState ->
            currentState.copy(
                currentScrambledWord = pickRandomWordAndShuffle()
            )
        }
    }

    fun resetGame() {
        usedWords.clear()

        val currentLang = _uiState.value.lang
        _uiState.value = GameUiState(
            currentScrambledWord = pickRandomWordAndShuffle(),
            lang = currentLang,
            currentWordCount = 1,
            score = 0
        )
    }
}
