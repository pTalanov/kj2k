package org.jetbrains.jet.j2k.ast

public open class Block(val statements: List<Element>, val notEmpty: Boolean = false) : Statement() {
    public override fun isEmpty(): Boolean {
        return !notEmpty && (statements.size() == 0 || statements.all { it == Statement.EMPTY_STATEMENT })
    }

    public override fun toKotlin(): String {
        if (!isEmpty()) {
            return "{\n" + statements.toKotlin("\n") + "\n}"
        }

        return ""
    }

    class object {
        public val EMPTY_BLOCK: Block = Block(arrayList())
    }
}
