package com.felipepalma14.jacocointro.data

import com.felipepalma14.jacocointro.domain.IRepository
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class FakeRepositoryImplTest {

    val repository: IRepository = FakeRepositoryImpl()



    @Test
    fun testGetData() {
        val data = repository.getData()
        assert(data == "Fake Data")
    }
}