package com.airbnb.paris.processor.framework

import com.squareup.javapoet.ClassName
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror

internal val filer get() = SkyProcessor.INSTANCE.filer
internal val messager get() = SkyProcessor.INSTANCE.messager
internal val elements get() = SkyProcessor.INSTANCE.elements
internal val types get() = SkyProcessor.INSTANCE.types
internal val kaptOutputPath get() = SkyProcessor.INSTANCE.kaptOutputPath
internal val loggedMessages get() = SkyProcessor.INSTANCE.loggedMessages

internal fun erasure(type: TypeMirror): TypeMirror = types.erasure(type)

internal fun isSameType(type1: TypeMirror, type2: TypeMirror) = types.isSameType(type1, type2)

internal fun isSubtype(type1: TypeMirror, type2: TypeMirror) = types.isSubtype(type1, type2)

// ClassName

internal fun ClassName.toTypeElement(): TypeElement = elements.getTypeElement(reflectionName())

internal fun ClassName.toTypeMirror(): TypeMirror = toTypeElement().asType()

// Element

internal fun Element.getPackageElement(): PackageElement = elements.getPackageOf(this)

internal fun Element.isPublic(): Boolean = this.modifiers.contains(Modifier.PUBLIC)
internal fun Element.isNotPublic(): Boolean = !isPublic()

internal fun Element.isPrivate(): Boolean = this.modifiers.contains(Modifier.PRIVATE)
internal fun Element.isNotPrivate(): Boolean = !isPrivate()

internal fun Element.isProtected(): Boolean = this.modifiers.contains(Modifier.PROTECTED)
internal fun Element.isNotProtected(): Boolean = !isProtected()

internal fun Element.isStatic(): Boolean = Modifier.STATIC in modifiers
internal fun Element.isNotStatic(): Boolean = !isStatic()

internal fun Element.isFinal(): Boolean = Modifier.FINAL in modifiers
internal fun Element.isNotFinal(): Boolean = !isFinal()

internal fun Element.isClass(): Boolean = kind == ElementKind.CLASS
internal fun Element.isNotClass(): Boolean = !isClass()

internal fun Element.isField(): Boolean = kind == ElementKind.FIELD
internal fun Element.isNotField(): Boolean = !isField()

internal fun Element.isMethod(): Boolean = kind == ElementKind.METHOD
internal fun Element.isNotMethod(): Boolean = !isMethod()

internal fun Element.hasAnnotation(simpleName: String): Boolean {
    return this.annotationMirrors
        .map { it.annotationType.asElement().simpleName.toString() }
        .contains(simpleName)
}

internal fun Element.hasAnyAnnotation(simpleNames: Set<String>): Boolean {
    return this.annotationMirrors
        .map { it.annotationType.asElement().simpleName.toString() }
        .any { simpleNames.contains(it) }
}

internal val KOTLIN_METADATA_ANNOTATION =
    Class.forName("kotlin.Metadata").asSubclass(Annotation::class.java)

/**
 * True is [isJava] is false and vice-versa
 */
internal fun Element.isKotlin(): Boolean = when (this) {
    is TypeElement -> getAnnotation(KOTLIN_METADATA_ANNOTATION) != null
    is ExecutableElement, is VariableElement -> enclosingElement.isKotlin()
    else -> TODO()
}

/**
 * True is [isKotlin] is false and vice-versa
 */
internal fun Element.isJava(): Boolean = !isKotlin()

internal fun Element.siblings(): List<Element> = when (this) {
    is ExecutableElement,
    is VariableElement -> enclosingElement.enclosedElements.filterNot { it === this }
    else -> TODO()
}

// String

internal fun String.className(): ClassName =
    ClassName.get(this.substringBeforeLast("."), this.substringAfterLast("."))

// TypeElement

internal val TypeElement.className: ClassName get() = ClassName.get(this)

internal val TypeElement.packageName: String get() = className.packageName()

// TypeMirror

internal fun TypeMirror.asTypeElement(): TypeElement = types.asElement(this) as TypeElement

// Android specific

internal fun isView(type: TypeMirror): Boolean =
    isSubtype(type, AndroidClassNames.VIEW.toTypeMirror())

