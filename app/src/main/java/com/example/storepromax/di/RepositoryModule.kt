package com.example.storepromax.di

import com.example.storepromax.data.repository.AuthRepositoryImpl
import com.example.storepromax.data.repository.CartRepositoryImpl
import com.example.storepromax.data.repository.ChatRepositoryImpl
import com.example.storepromax.data.repository.OrderRepositoryImpl
import com.example.storepromax.data.repository.PostRepositoryImpl
import com.example.storepromax.data.repository.ProductRepositoryImpl
import com.example.storepromax.data.repository.StatsRepositoryImpl
import com.example.storepromax.domain.repository.AuthRepository
import com.example.storepromax.domain.repository.CartRepository
import com.example.storepromax.domain.repository.ChatRepository
import com.example.storepromax.domain.repository.OrderRepository
import com.example.storepromax.domain.repository.PostRepository
import com.example.storepromax.domain.repository.ProductRepository
import com.example.storepromax.domain.repository.StatsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCartRepository(
        cartRepositoryImpl: CartRepositoryImpl
    ): CartRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        orderRepositoryImpl: OrderRepositoryImpl
    ): OrderRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        productRepositoryImpl: ProductRepositoryImpl
    ): ProductRepository

    @Binds
    @Singleton
    abstract fun bindPostRepository(
        postRepositoryImpl: PostRepositoryImpl
    ): PostRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(
        statsRepositoryImpl: StatsRepositoryImpl
    ): StatsRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

}