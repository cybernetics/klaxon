package com.beust.klaxon

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import kotlin.reflect.KClass

@Test
class TypeAdapterTest {
    open class Shape
    data class Rectangle(val width: Int, val height: Int): Shape()
    data class Circle(val radius: Int): Shape()

    class ShapeTypeAdapter: TypeAdapter<Shape> {
        override fun instantiate(type: Any): KClass<out Shape> = when(type as Int) {
            1 -> Rectangle::class
            2 -> Circle::class
            else -> throw IllegalArgumentException("Unknown type: $type")
        }
    }

    val json = """
            [
                { "type": 1, "shape": { "width": 100, "height": 50 } },
                { "type": 2, "shape": { "radius": 20} }
            ]
        """

    fun typeAdapterTest() {
        class Data (
                @TypeFor(field = "shape", adapter = ShapeTypeAdapter::class)
                val type: Integer,

                val shape: Shape
        )

        val shapes = Klaxon().parseArray<Data>(json)
        assertThat(shapes!![0].shape as Rectangle).isEqualTo(Rectangle(100, 50))
        assertThat(shapes[1].shape as Circle).isEqualTo(Circle(20))
    }

    @Test(expectedExceptions = [KlaxonException::class])
    fun typoInFieldName() {
        class BogusData (
            @TypeFor(field = "___nonexistentField", adapter = ShapeTypeAdapter::class)
            val type: Integer,

            val shape: Shape
        )
        val shapes = Klaxon().parseArray<BogusData>(json)
    }

    @Test(expectedExceptions = [IllegalArgumentException::class],
            expectedExceptionsMessageRegExp = ".*Unknown type.*")
    fun unknownDiscriminantValue() {
        class BogusShapeTypeAdapter: TypeAdapter<Shape> {
            override fun instantiate(type: Any): KClass<out Shape> = when(type as Int) {
                1 -> Rectangle::class
                else -> throw IllegalArgumentException("Unknown type: $type")
            }
        }

        class BogusData (
                @TypeFor(field = "shape", adapter = BogusShapeTypeAdapter::class)
                val type: Integer,

                val shape: Shape
        )

        val shapes = Klaxon().parseArray<BogusData>(json)
    }
}
