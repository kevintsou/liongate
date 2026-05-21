package com.liongate.budget.data.repository

import com.liongate.budget.data.local.dao.CategoryDao
import com.liongate.budget.data.local.entity.CategoryEntity
import com.liongate.budget.domain.model.Category
import com.liongate.budget.domain.model.CategoryType
import com.liongate.budget.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllCategoriesList(): List<Category> {
        return categoryDao.getAllCategoriesList().map { it.toDomain() }
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category.toEntity())
    }

    override suspend fun deleteCategoryById(id: Long) {
        categoryDao.deleteCategoryById(id)
    }

    private fun CategoryEntity.toDomain(): Category {
        return Category(
            id = id,
            name = name,
            colorHex = colorHex,
            icon = icon,
            type = when (type) {
                "INCOME" -> CategoryType.INCOME
                "EXPENSE" -> CategoryType.EXPENSE
                else -> CategoryType.BOTH
            }
        )
    }

    private fun Category.toEntity(): CategoryEntity {
        return CategoryEntity(
            id = id,
            name = name,
            colorHex = colorHex,
            icon = icon,
            type = type.name
        )
    }
}
