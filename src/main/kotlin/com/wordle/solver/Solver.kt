package com.wordle.solver

import java.io.File
import java.util.*

class Solver {
    private var available: Set<String> = emptySet()
    /**
     *  Indexes the set of all words containing a specific letter by the index it's located at
     *  To get all words containing some letter union each set in that row
     */
    private val matrix = Array(26) { Array(5) { mutableSetOf<String>() } }

    // The number of times each letter occurs in the word list
    private val letterFrequency = Array(26) { 0 }
    private val letterRanking = Array(26) { 0 }
    private val wordToScore = mutableMapOf<String, Int>()

    // These track what is known about the secret word so far
    // only necessary if you want to print this info to the user
//    private val indicesWithLetter = mutableMapOf<Char, MutableSet<Int>>()
//    private val indicesWithoutLetter = mutableMapOf<Char, MutableSet<Int>>()
//    private val inWord = mutableSetOf<Char>()
//    private val notInWord = mutableSetOf<Char>()

    init {
        ClassLoader.getSystemClassLoader().getResource("words.txt")!!.path.let { path ->
            // First pass: read words from file, build matrix and determine letter frequencies
            val allStrings = mutableSetOf<String>()
            File(path).forEachLine { word ->
                allStrings.add(word)
                word.forEachIndexed { index, c ->
                    matrix[c - 'a'][index].add(word)
                    letterFrequency[c - 'a']++
                }
            }
            available = allStrings
            println("Loaded ${allStrings.size} words.")

            //initLetterRanking()
            //println("Letter Frequency: ${letterFrequency.contentDeepToString()}")
            //println("Letter Ranking: ${letterRanking.contentDeepToString()}")

            // If a letter appears more than once in the word, it should be valued less than the first appearance of any other letter
            val divisor = letterFrequency.maxOf { it } / (letterFrequency.minOf { it } - 2)

            // Second pass: loop through words in memory to build wordToScore map
            allStrings.forEach { word ->
                val counts = mutableMapOf<Char, Int>()
                var score = 0
                word.forEachIndexed { index, c ->
                    val count = counts.getOrDefault(c, 0)
                    when (count) {
                        0 -> score += letterFrequency[c - 'a']
                        1 -> score += letterFrequency[c - 'a'] / divisor
                    }
                    counts[c] = count + 1
                }
                wordToScore[word] = score
            }
            println("Assigned scores to words.")
        }
    }

    /**
     * Update What is known about the secret word
     * guess: String composed of 5 letters [A-Z][a-z]
     * score: String composed of 5 digits [0-2]
     *   - 0: Wrong letter
     *   - 1: Correct letter, wrong position
     *   - 2: Correct letter, correct position
     * returns number of possible words remaining
     */
    fun update(guess: String, score: String): Int {
        guess.lowercase().forEachIndexed { index, c ->
            when(score[index]) {
                '0' -> {
//                    notInWord.add(c)
                    matrix[c - 'a'].forEach { set ->
                        available = available.subtract(set)
                    }
                }
                '1' -> {
//                    inWord.add(c)
//                    indicesWithoutLetter[c]?.let { set ->
//                        set.add(index)
//                    } ?: run {
//                        indicesWithoutLetter[c] = mutableSetOf(index)
//                    }
                    var row = emptySet<String>()
                    matrix[c - 'a'].forEachIndexed { i, set ->
                        if (i != index) {
                            row = row.union(set)
                        }
                    }
                    available = available.intersect(row)
                }
                '2' -> {
//                    inWord.add(c)
//                    indicesWithLetter[c]?.let { set ->
//                        set.add(index)
//                    } ?: run {
//                        indicesWithLetter[c] = mutableSetOf(index)
//                    }
                    available = available.intersect(matrix[c - 'a'][index])
                }
            }
        }
        return available.size
    }

    fun guess(): List<String> {
        // Max Heap of guesses based on the frequency of their individual characters
        val topGuesses = PriorityQueue(compareByDescending<String> { word -> wordToScore[word] })
        topGuesses.addAll(available)

        val result = mutableListOf<String>()
        var index = 0
        while (topGuesses.isNotEmpty() && index < 5) {
            result.add(topGuesses.remove())
            index++
        }

        return result
    }

    private fun initLetterRanking() {
        val maxFrequency = PriorityQueue(compareBy<Char> { c -> letterFrequency[c - 'a'] })
        maxFrequency.addAll('a' .. 'z')

        var index = 0
        while (maxFrequency.isNotEmpty()) {
            letterRanking[maxFrequency.remove() - 'a'] = index + 1
            index++
        }
    }
}