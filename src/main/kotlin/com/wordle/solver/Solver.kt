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
    private val wordToFrequencyScore = mutableMapOf<String, Int>()
    private val wordToFrequencyAtPositionScore = mutableMapOf<String, Int>()

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

        guess.lowercase().forEachIndexed { index, c ->
            when (score[index]) {
                '0' -> {
                    matrix[c - 'a'].forEach { set ->
                        result = result.subtract(set)
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

        return result
    }

    private fun scoreWords() {
        val letterFrequency = Array(26) { 0 }
        val letterFrequencyAtPosition = Array(wordLength) { Array(26) { 0 } }
        // First pass: determine letter frequencies
        available.forEach { word ->
            word.forEachIndexed { index, c ->
                letterFrequency[c - 'a']++
                letterFrequencyAtPosition[index][c - 'a']++
            }
        }
        val letterRankingAtPosition = rankLetterAtPosition(letterFrequencyAtPosition)

        // If a letter appears more than once in the word, it should be valued less than the first appearance of any other letter
        val divisor = letterFrequency.maxOf { it } / (letterFrequency.minOf { it } - 1)

        // Second pass: loop through words in memory to build wordToScore map
        wordToFrequencyScore.clear()
        wordToFrequencyAtPositionScore.clear()
        available.forEach { word ->
            val counts = mutableMapOf<Char, Int>()
            var frequencyScore = 0
            var frequencyAtPositionScore = 0
            word.forEachIndexed { index, c ->
                val count = counts.getOrDefault(c, 0)
                when (count) {
                    0 -> frequencyScore += letterFrequency[c - 'a']
                    1 -> frequencyScore += letterFrequency[c - 'a'] / divisor - 1
                }
                counts[c] = count + 1
                frequencyAtPositionScore += letterRankingAtPosition[index][c - 'a']
            }
            wordToFrequencyScore[word] = frequencyScore
            wordToFrequencyAtPositionScore[word] = frequencyAtPositionScore
        }
    }

    fun guess(): Map<String, Int> {
        // Max Heap of guesses based on the frequency of their individual characters
        val topGuesses = PriorityQueue(compareByDescending<String> { word -> wordToFrequencyScore[word] }.thenByDescending {  word -> wordToFrequencyAtPositionScore[word] })
        topGuesses.addAll(available)

        val result = mutableMapOf<String, Int>()
        var index = 0
        while (topGuesses.isNotEmpty() && index < 5) {
            topGuesses.remove().also {
                result[it] = wordToFrequencyScore.getOrDefault(it, 0)
            }
            index++
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

    private fun rankLetter(letterFrequency: Array<Int>): Array<Int> {
        val letterRanking = Array(26) { 0 }
        val maxFrequency = PriorityQueue(compareBy<Char> { c -> letterFrequency[c - 'a'] })
        maxFrequency.addAll('a'..'z')

        var index = 0
        while (maxFrequency.isNotEmpty()) {
            letterRanking[maxFrequency.remove() - 'a'] = index + 1
            index++
        }

        return letterRanking
    }

    private fun rankLetterAtPosition(letterFrequencyAtPosition: Array<Array<Int>>): Array<Array<Int>> {
        val letterRankingAtPosition = Array(wordLength) { Array(26) { 0 } }

        letterFrequencyAtPosition.forEachIndexed { index, letterFrequency ->
            letterRankingAtPosition[index] = rankLetter(letterFrequency)
        }

        return letterRankingAtPosition
    }
}

