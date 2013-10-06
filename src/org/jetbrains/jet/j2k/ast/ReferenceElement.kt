package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type

public open class ReferenceElement(val reference: Identifier, val types: List<Type>) : Element() {
    public override fun toKotlin() = reference.toKotlin() + types.toKotlin(", ", "<", ">")
}
