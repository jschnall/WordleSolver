package com.wordle.solver

fun main(args: Array<String>) {
    println("\n--- Welcome to Wordle Solver ---")

    val wordLength = if (args.isNotEmpty()) args[0].toInt() else 5
    val path = if (args.size < 2) "" else args[1]
    val solver = if (args.isNotEmpty()) Solver(wordLength, path) else Solver()

    println(help())
    do {
        val input = readln().split(" ")

        when {
            input[0].equals("Q", ignoreCase = true) || input[0].equals("QUIT", ignoreCase = true) -> break
            input[0].equals("H", ignoreCase = true) || input[0].equals("HELP", ignoreCase = true) -> println(help())
            input[0].equals("G", ignoreCase = true) || input[0].equals("GUESS", ignoreCase = true) -> println(solver.guess())
            input[0].equals("F", ignoreCase = true) || input[0].equals("FEEDBACK", ignoreCase = true) -> handleFeedback(input, solver)
            input[0].equals("R", ignoreCase = true) || input[0].equals("RESET", ignoreCase = true) -> println("${solver.reset()} word(s) remain")
            else -> println ("Invalid command")
        }
    } while (true)
    println("Goodbye.")
}

fun help(): String {
    return "\n(Q) Quit: Quit playing\n" +
            "(H) Help: Show this menu\n" +
            "(G) Guess: Show top 5 guesses\n" +
            "(F) Feedback: Give feedback on last guess. Example: \"f adieu 11020\"\n" +
            "(R) Reset: Reset the solver state for a new puzzle"
}

fun feedbackHelp(wordLength: Int): String {
    return "Usage: \"feedback { guess } { score }\", where score is composed of $wordLength digits from 0 to 2.\n" +
            "0: Wrong letter\n" +
            "1: Correct letter, wrong position\n" +
            "2: Correct letter, correct position\n" +
            "Example: \"f adieu 11020\"\n"
}

fun handleFeedback(input: List<String>, solver: Solver) {
    val errorStr = validateFeedback(input, solver.wordLength)

    if (errorStr.isNotEmpty()) {
        println(errorStr)
        return
    }

    println("${solver.update(input[1], input[2])} word(s) remain")
    println(solver.guess())
}

fun validateFeedback(input: List<String>, wordLength: Int): String {
    if (input.size != 3) {
        return feedbackHelp(wordLength)
    } else if (input[1].length != wordLength) {
        return "Word must be $wordLength digits."
    } else if (!input[1].matches(regex = Regex("[a-zA-Z]+"))) {
        return "Word must only contain letters"
    } else if (input[2].length != wordLength) {
        return "Score must be $wordLength digits."
    } else if (!input[2].matches(regex = Regex("[0-2]+"))) {
        return "Score must only contain digits between 0 and 2"
    }
    return ""
}



