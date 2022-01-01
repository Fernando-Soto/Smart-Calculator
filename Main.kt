package calculator

import java.util.*
import java.math.BigInteger


val vTable = mutableMapOf<String, BigInteger>()

val regexes = mapOf<String, Regex>(
    "variable" to Regex("[a-zA-Z]+"),
    "varOrNumber" to Regex("""[a-zA-Z]+|([+-]?\d+|[a-zA-Z]+)"""),
    "delimiterWithEquals" to Regex("\\s*=\\s*"),
    "commands" to Regex("/exit|/help|/EMPTY/|/print"),
    "validChar" to Regex("[a-z|A-Z|0-9]"),
    "cleanPostfix" to Regex("(([a-zA-Z]+)|([0-9]+)|([\\(\\)*/\\+\\-\\^]))"),
    "correctSign" to Regex("""(\s+[*/^+-]\s+-)(\s+)"""),
    "singleDigit" to Regex("""\s*-\s*\d+"""),
    "multiPlus" to Regex("""(\+{2,})"""),
    "multiMinus" to Regex("""(\-{2,})"""),
    "invalidMultOperators" to Regex("""(.*[*\/^]{2,}.*)"""),
    "removeSpaces" to Regex("""\s+""")
)

fun main() {
    while (true) {
        val input = readLine()!!.trim()
        try {
            checkInput(input)
            val list = if (input.isNotBlank()) input.split(Regex("""\s+""")) else listOf("/EMPTY/")
            when (list[0]) {
                "/EMPTY/" -> continue
                "/exit" -> break
                "/help" -> println("The program calculates the sum of numbers")
                "/print" -> println(vTable)
            }
        } catch (e: InvalidExpression) {
            println(e.message)
        } catch (e: UnknownCommand) {
            println(e.message)
        } catch (e: InvalidAssignment) {
            println(e.message)
        } catch (e: UnknownVariable) {
            println(e.message)
        }
    }
    println("Bye!")
}

fun validateSyntax(input: String): Boolean {
    var countRParens = 0
    var countLParens = 0
    for (char in input) {
        if (char == '(') countRParens++
        if (char == ')') countLParens++
    }
    return if (countRParens == countLParens) return true else false
}

fun checkInput(input: String) {
    val commands = regexes["commands"]!!
    val variable = regexes["variable"]!!

    when {
        input.isEmpty() -> return
        input.contains("=") -> assignVariables(input)
        regexes["variable"]!!.matches(input) -> getFromMemory(input, true)
        (input[0] == '/') && !commands.matches(input) -> throw UnknownCommand()
        (input[0] != '/') && !validateSyntax(input) -> throw InvalidExpression()
        commands.matches(input) -> return
        else -> {
            val pstq = parseStringToQueue(input)
            // replace variable name with it numerical value
            for (v in pstq.indices) {
                if (pstq[v] is String && variable.matches(pstq[v] as String)) {
                    val value = getFromMemory(pstq[v] as String, false)
                    pstq[v] = value
                }
            }
            // Convert a postfixed expression to infixed expression
            val infixed = infixToPostfix(pstq)
            println(sum(infixed))
        }
    }
}

fun assignVariables(input: String) {
    val variable = regexes["variable"]!!
    val varOrNumber = regexes["varOrNumber"]!!
    val delimiter = regexes["delimiterWithEquals"]!!
    val params = input.split(delimiter)
    if (params.size > 2 || !variable.matches(params[0]) || !varOrNumber.matches(params[1])) {
        throw InvalidAssignment()
    }
    val a = params[0]
    val b = params[1]
    if (variable.matches(b)) {
        try {
            vTable[a] = vTable[b]!!
        } catch (e: Exception) {
            throw UnknownVariable()
        }
    } else {
        vTable[a] = BigInteger(b)
    }
}

fun getFromMemory(input: String, printVal: Boolean): BigInteger {
    val value = vTable[input] ?: throw UnknownVariable()
    if (printVal) println(value)
    return value
}

fun sum(pfix: MutableList<Any>): BigInteger {
    val stack = Stack<BigInteger>()
    for (nextVal in pfix) {
        if (nextVal is BigInteger) {
            stack.push(nextVal as BigInteger)
        } else {
            val second = stack.pop()
            val first = stack.pop()
            when (nextVal as String) {
                "^" -> stack.push( first.pow(second.toInt()))
                "/" -> stack.push( first / second)
                "*" -> stack.push( first * second)
                "+" -> stack.push( first + second)
                "-" -> stack.push( first - second)
            }
        }
    }
    return stack.pop()
}

fun String.isNumber(): Boolean = this.last() in '0'..'9'

fun precedence(str: String): BigInteger {
    return when (str) {
        "^"      ->  BigInteger("3")
        "/", "*" ->  BigInteger("2")
        "+", "-" ->  BigInteger("1")
        else     ->  BigInteger("-1")
    }
}

fun infixToPostfix(obj: MutableList<Any>): MutableList<Any> {
    val goodChar = regexes["validChar"]!!
    val goodName = regexes["variable"]!!
    val stack = Stack<Any>()
    val postfixExp: MutableList<Any> = mutableListOf()
    for (idx in obj.indices) {
        val value = if (obj[idx] is String) obj[idx] as String else obj[idx] as BigInteger
        if (value !is String || value.matches(goodName)) {
            postfixExp += value
            continue
        }
        when {
            goodChar.matches(value.toString()) -> {
                postfixExp += value
            }
            value == "(" -> {
                stack.push(value)
            }
            value == ")" -> {
                while (stack.peek() != "(") {
                    postfixExp += stack.pop()
                }
                stack.pop()
            }
            else -> {
                while (!stack.empty() && precedence(obj[idx] as String) <= precedence(stack.peek() as String )) {
                    postfixExp += stack.pop()
                }
                stack.push(value)
            }
        }
    }

    while (!stack.empty()) {
        postfixExp += stack.pop()
    }

    return postfixExp
}

fun replaceMultipleOperators(input: String): String {
    var result = input.replace(regexes["multiPlus"]!!, "+")
    val groups = regexes["multiMinus"]!!.findAll(result).toList()
    var strValue = ""
    for (g in groups) {
        strValue = g.value
        result = when (strValue.count() % 2 == 0) {
            true -> result.replace(strValue, "+")
            false -> result.replace(strValue, "-")
        }
    }
    if (result.matches(regexes["invalidMultOperators"]!!)) throw InvalidExpression()
    return result
}

fun parseStringToQueue(infix: String): MutableList<Any> {
    var infixStr = replaceMultipleOperators(infix)
    infixStr = infixStr.replace(regexes["cleanPostfix"]!!, "$1 ")
    infixStr = infixStr.replace(regexes["correctSign"]!!, "$1").trim()
    val result = mutableListOf<Any>()
    if (infixStr.matches(regexes["singleDigit"]!!)) {
        infixStr = infixStr.replace(regexes["removeSpaces"]!!, "")
        result.add(BigInteger(infixStr) as Any)
        return result
    }
    val infixExpression = infixStr.replace("\\s+".toRegex(), " ").trim().split(" ")
    for (e in infixExpression) {
        if (e.isEmpty()) continue
        val element = if (e.isNumber()) BigInteger(e) else e
        result.add(element as Any)
    }
    return result
}