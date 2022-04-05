package com.wordle.solver

import java.io.File
import java.io.FileNotFoundException
import java.util.*

class Solver(val wordLength: Int = 5, path: String = "") {
    private var available: Set<String> = emptySet()

    /**
     *  Indexes the set of all words containing a specific letter by the index it's located at
     *  To get all words containing some letter union each set in that row
     */
    private val matrix = Array(26) { Array(wordLength) { mutableSetOf<String>() } }
    private val wordToFrequencyScore = mutableMapOf<String, Double>()
    private val wordToFrequencyAtIndexScore = mutableMapOf<String, Double>()


    // These track what is known about the secret word so far
    // only needed if you want to print this info to the user
//    private val guesses = mutableListOf<String>()
//    private val indicesWithLetter = mutableMapOf<Char, MutableSet<Int>>()
//    private val indicesWithoutLetter = mutableMapOf<Char, MutableSet<Int>>()
//    private val inWord = mutableSetOf<Char>()
//    private val notInWord = mutableSetOf<Char>()

    init {
        val bufferedReader = if (path.isEmpty()) {
            this.javaClass.classLoader.getResourceAsStream("com/wordle/solver/words.txt")?.bufferedReader()
        } else {
            File(path).bufferedReader()
        }

        bufferedReader?.let { reader ->
            // Read words from file, build matrix and determine letter frequencies
            val allStrings = mutableSetOf<String>()
            reader.forEachLine { word ->
                allStrings.add(word)
                word.forEachIndexed { index, c ->
                    matrix[c - 'a'][index].add(word)
                }
            }
            available = allStrings
            println("Loaded ${allStrings.size} words.")

            scoreWords()
            println("Assigned scores to words.")
        } ?: run {
            throw FileNotFoundException("Can't find words.txt")
        }
    }

    /**
     * Update What is known about the secret word
     * guess: String composed of wordLength letters [A-Z][a-z]
     * score: String composed of wordLength digits [0-2]
     *   - 0: Wrong letter
     *   - 1: Correct letter, wrong position
     *   - 2: Correct letter, correct position
     * returns number of possible words remaining
     */
    fun update(guess: String, score: String): Int {
        available = findRemaining(available, guess, score)

        // recalculate word scores to reflect only the words remaining
        scoreWords()

        return available.size
    }

    private fun findRemaining(available: Set<String>, guess: String, score: String): Set<String> {
        var result = available

        val letterToScores = mutableMapOf<Char, List<Int>>()
        guess.lowercase().forEachIndexed { index, c ->
            letterToScores[c] = letterToScores.getOrDefault(c, emptyList()).plus(score[index].digitToInt())
            when (score[index]) {
                '0' -> {
                    matrix[c - 'a'].forEachIndexed { i, set ->
                        // special case when giving feedback on word with duplicate letters
                        if (i == index || guess.count { it == c } == 1) {
                            result = result.subtract(set)
                        }
                    }
                }
                '1' -> {
                    var row = emptySet<String>()
                    matrix[c - 'a'].forEachIndexed { i, set ->
                        if (i != index) {
                            row = row.union(set)
                        }
                    }
                    result = result.intersect(row)
                }
                '2' -> {
                    result = result.intersect(matrix[c - 'a'][index])
                }
            }
        }

        // If there are duplicates of a letter found, filter out any words that don't have at least that many instances of the letter
        letterToScores.forEach { entry ->
            // count how many of the letter are present
            val count = entry.value.count { it > 0 }
            if (entry.value.count { it > 0 } > 0 ) {
                result = result.filter { word -> word.count { it == entry.key } >= count }.toSet()
            }
        }

        return result
    }

    private fun scoreWords() {
        val letterFrequency = Array(26) { 0.0 }
        val tot =  available.size * wordLength
        val indexFrequencyOfLetter = Array(wordLength) { Array(26) { 0.0 } }
        val totAtIndex =  Array(wordLength) { 0.0 }
        // First pass: determine letter frequencies
        available.forEach { word ->
            word.forEachIndexed { index, c ->
                letterFrequency[c - 'a']++
                indexFrequencyOfLetter[index][c - 'a']++
                totAtIndex[index]++
            }
        }

        // Second pass: loop through available words to build wordToScore map
        wordToFrequencyScore.clear()
        wordToFrequencyAtIndexScore.clear()
        available.forEach { word ->
            var frequencyScore = 1.0
            var frequencyAtIndexScore = 1.0
            val indicesWithLetter = word.withIndex().groupBy( { it.value }, { it.index } )
            indicesWithLetter.forEach { entry ->
                entry.value.forEach { i ->
                    // Adjust percentages to give lower value to words with multiple copies of the same letter
                    frequencyScore *= (letterFrequency[entry.key - 'a'] / entry.value.size - entry.value.size) / tot
                    frequencyAtIndexScore *= (indexFrequencyOfLetter[i][entry.key - 'a'] / entry.value.size  - entry.value.size) / totAtIndex[i]
                }
            }
            wordToFrequencyScore[word] = frequencyScore
            wordToFrequencyAtIndexScore[word] = frequencyAtIndexScore
        }
    }

    fun guess(exclude: String = ""): Map<String, Pair<Double, Double>> {
        // Max Heap of guesses based on the frequency of their individual characters
        val topGuesses = PriorityQueue(compareByDescending<String> { word -> wordToFrequencyScore[word] }.thenByDescending { word -> wordToFrequencyAtIndexScore[word] } )
        topGuesses.addAll(available)

        val result = mutableMapOf<String, Pair<Double, Double>>()
        var index = 0
        while (topGuesses.isNotEmpty() && index < 5) {
            topGuesses.remove().also { word ->
                if (!word.any { it in exclude }) {
                    result[word] = Pair(wordToFrequencyScore.getOrDefault(word, 0.0), wordToFrequencyAtIndexScore.getOrDefault(word, 0.0))
                    index++
                }
            }
        }

        return result
    }

    fun reset(): Int {
        var allWords = emptySet<String>()

        for (c in 'a'..'z') {
            for (i in 0 until wordLength) {
                allWords = allWords.union(matrix[c - 'a'][i])
            }
        }
        available = allWords
        scoreWords()

//        guesses.clear()
//        indicesWithLetter.clear()
//        indicesWithoutLetter.clear()
//        inWord.clear()
//        notInWord.clear()

        return available.size
    }

//    private fun rankLetters(letterFrequency: Array<Int>): Array<Int> {
//        val letterRanking = Array(26) { 0 }
//        val maxFrequency = PriorityQueue(compareBy<Char> { c -> letterFrequency[c - 'a'] })
//        maxFrequency.addAll('a'..'z')
//
//        var index = 0
//        while (maxFrequency.isNotEmpty()) {
//            letterRanking[maxFrequency.remove() - 'a'] = index + 1
//            index++
//        }
//
//        return letterRanking
//    }
//
//    private fun rankIndex(letterFrequency: Array<Int>): Array<Int> {
//        val letterRanking = Array(wordLength) { 0 }
//        val maxFrequency = PriorityQueue(compareBy<Int> { i -> letterFrequency[i] })
//        maxFrequency.addAll(letterFrequency.indices)
//
//        var index = 0
//        while (maxFrequency.isNotEmpty()) {
//            letterRanking[maxFrequency.remove()] = index + 1
//            index++
//        }
//
//        return letterRanking
//    }
//
//    private fun rankLettersAtPosition(letterFrequencyAtPosition: Array<Array<Int>>): Array<Array<Int>> {
//        val letterRankingAtPosition = Array(26) { Array(wordLength) { 0 } }
//
//        letterFrequencyAtPosition.forEachIndexed { index, letterFrequency ->
//            letterRankingAtPosition[index] = rankIndex(letterFrequency)
//        }
//
//        return letterRankingAtPosition
//    }
}

