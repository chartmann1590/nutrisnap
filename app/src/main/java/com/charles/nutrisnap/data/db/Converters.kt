package com.charles.nutrisnap.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMealType(value: MealType): String = value.name

    @TypeConverter
    fun toMealType(value: String): MealType = MealType.valueOf(value)

    @TypeConverter
    fun fromMealSource(value: MealSource): String = value.name

    @TypeConverter
    fun toMealSource(value: String): MealSource = MealSource.valueOf(value)
}
