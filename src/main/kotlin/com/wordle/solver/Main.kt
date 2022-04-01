package com.wordle.solver

fun main(args: Array<String>) {
    println("\n--- Welcome to Wordle Solver ---")

    val solver = Solver()

    println(help())
    do {
        val input = readln().split(" ")

        when {
            input[0].equals("Q", ignoreCase = true) || input[0].equals("QUIT", ignoreCase = true) -> break
            input[0].equals("H", ignoreCase = true) || input[0].equals("HELP", ignoreCase = true) -> println(help())
            input[0].equals("G", ignoreCase = true) || input[0].equals("GUESS", ignoreCase = true) -> println(solver.guess())
            input[0].equals("F", ignoreCase = true) || input[0].equals("FEEDBACK", ignoreCase = true) -> handleFeedback(input, solver)
            else -> println ("Invalid command")
        }
    } while (true)
    println("Goodbye.")
}

fun help(): String {
    return "\n(Q) Quit: Quit playing\n" +
            "(H) Help: Show this menu\n" +
            "(G) Guess: Request a guess\n" +
            "(F) Feedback: Give feedback on last guess. Example: \"f adieu 11020\"\n"
}

fun feedbackHelp(): String {
    return "Usage: \"Feedback { guess } { score }\" where score is composed of 5 digits from 0 to 2.\n" +
            "0: Wrong letter\n" +
            "1: Correct letter, wrong position\n" +
            "2: Correct letter, correct position\n" +
            "Example: \"f adieu 11020\"\n"
}

fun handleFeedback(input: List<String>, solver: Solver) {
    val errorStr = validateFeedback(input)

    if (errorStr.isNotEmpty()) {
        println(errorStr)
        return
    }

    println("Remaining word(s) ${solver.update(input[1], input[2])}")
    println(solver.guess())
}

fun validateFeedback(input: List<String>): String {
    if (input.size != 3) {
        return feedbackHelp()
    } else if (input[1].length != 5) {
        return "Word must be 5 digits."
    } else if (!input[1].matches(regex = Regex("[a-zA-Z]+"))) {
        return "Word must only contain letters"
    } else if (input[2].length != 5) {
        return "Score must be 5 digits."
    } else if (!input[2].matches(regex = Regex("[0-2]+"))) {
        return "Score must only contain digits between 0 and 2"
    }
    return ""
}



