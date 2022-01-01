package calculator

class InvalidExpression : Exception("Invalid expression")
class UnknownCommand: Exception("Unknown command")
class InvalidAssignment: Exception("Invalid assignment")
class UnknownVariable: Exception("Unknown variable")