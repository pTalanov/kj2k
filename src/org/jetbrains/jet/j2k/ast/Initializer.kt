package org.jetbrains.jet.j2k.ast


public open class Initializer(val block: Block, modifiers: Set<Modifier>) : Member(modifiers) {
    public override fun toKotlin(): String {
        return block.toKotlin()
    }
}
