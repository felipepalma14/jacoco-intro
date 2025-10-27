package com.felipepalma14.jacocointro.data

import com.felipepalma14.jacocointro.domain.IRepository

class FakeRepositoryImpl : IRepository {
    override fun getData(): String {
        return "Fake Data"
    }
}