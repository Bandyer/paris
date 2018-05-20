package com.airbnb.paris.test

import com.airbnb.paris.processor.ParisProcessor
import com.google.common.truth.Truth.assert_
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory.javaSource
import org.junit.Test


class ParisProcessorTest {

    private fun assertCase(folder: String) {
        val view = JavaFileObjects.forResource("$folder/MyView.java")
        val generatedParisClass = JavaFileObjects.forResource("$folder/Paris.java")
        val generatedStyleApplierClass =
            JavaFileObjects.forResource("$folder/MyViewStyleApplier.java")

        assert_().about(javaSource())
            .that(view)
            .processedWith(ParisProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(generatedParisClass)
            .and()
            .generatesSources(generatedStyleApplierClass)
    }

    private fun assertError(
        folder: String,
        errorCount: Int? = null,
        errorFragment: String? = null
    ) {
        val view = JavaFileObjects.forResource("$folder/MyView.java")

        assert_().about(javaSource())
            .that(view)
            .processedWith(ParisProcessor())
            .failsToCompile()
            .apply {
                errorCount?.let {
                    withErrorCount(it)
                }
                errorFragment?.let {
                    withErrorContaining(it)
                }
            }
    }

    @Test
    fun attrNonResValue() {
        // An @Attr with an arbitrary int value instead of a resource ID
        assertError(
            "attr_non_res_value",
            1,
            "Could not retrieve Android resource ID from annotation."
        )
    }

    @Test
    fun attrWrongValueType() {
        // An @Attr with an non-existent R.styleable field
        assertError(
            "attr_wrong_value_type",
            2,
            "Incorrectly typed @Attr value parameter"
        )
    }

    @Test
    fun attrs() {
        // TODO Add Drawable case
        assertCase("attrs")
    }

    @Test
    fun defaultValues() {
        assertCase("default_values")
    }

    @Test
    fun errorPrivateStyle() {
        // A private field annotated with @Style
        assertError(
            "error_private_style",
            1,
            "Fields annotated with @Style can't be private or protected."
        )
    }

    @Test
    fun styleableChildWrongValueType() {
        // A @StyleableChild with an non-existent R.styleable field
        assertError(
            "styleable_child_wrong_value_type",
            2,
            "Incorrectly typed @StyleableChild value parameter"
        )
    }

    @Test
    fun styleableFields() {
        assertCase("styleable_fields")
    }

    @Test
    fun styleableMinimal() {
        // A @Styleable view with no other annotations used
        assertCase("styleable_minimal")
    }

    @Test
    fun styleableOutsidePackageSingleAttr() {
        // A @Styleable view in an unexpected package (outside the package namespace of the module)
        // and a single @Attr method
        assertCase("styleable_outside_package_single_attr")
    }

    @Test
    fun styleableOutsidePackageNoR() {
        // A @Styleable view in an unexpected package (outside the package namespace of the module)
        // with no R (or R2) references as annotation parameters. Paris has no way of finding the R
        // package (which it needs to figure out the package of the generated Paris class) so this
        // should cause an error
        assertError("styleable_outside_package_no_R", 1, "R class")
    }

    @Test
    fun styles() {
        assertCase("styles")
    }
}