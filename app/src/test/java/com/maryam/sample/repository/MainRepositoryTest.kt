package com.maryam.sample.repository

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.maryam.sample.api.FakeGithubService
import com.maryam.sample.db.AppDatabase
import com.maryam.sample.db.PostDao
import com.maryam.sample.model.Post
import com.maryam.sample.util.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MainRepositoryTest : CoroutineTestBase() {
    private lateinit var repository: MainRepository
    private var apiService = FakeGithubService()
    private lateinit var postDao: PostDao
    private lateinit var sessionManager: SessionManager

//    @get:Rule
//    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        val app=ApplicationProvider.getApplicationContext<Application>()
        sessionManager=SessionManager(app)
        // using an in-memory database for testing, since it doesn't survive killing the process
        val db = Room.inMemoryDatabaseBuilder(
            app,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        postDao = db.getPostDao()

        repository = MainRepositoryImpl(apiService, postDao,sessionManager)
    }

    @Test
    fun getPostApiOnlyTest() {
        /** GIVEN  **/
        val postResponse = TestUtil.createPostResponse()

        /** WHEN **/
        val calledService = CompletableDeferred<Unit>()
        runBlocking {
            apiService.getPostImpl = {
                calledService.complete(Unit)
                GenericApiResponse.create(Response.success(postResponse))
            }

            repository.getPostsApiOnly(this.coroutineContext).addObserver().apply {
                calledService.await()
                advanceUntilIdle()

                /** THEN **/
                assertItems(
                    DataState.loading<List<Post>>(true)
                    ,
                      DataState.data<List<Post>>(postResponse.items)
                )
            }

        }

    }

    @Test
    fun getPostCashOnlyTest() {

        val list = ArrayList<Post>()
        val post1 = TestUtil.createPost(22, "new post", "fake body fake body fake body")
        val post2 = TestUtil.createPost(23, "new post2", "fake body fake body fake body2")
        list.add(post1)
        list.add(post2)
        runBlockingTest {
            postDao.insert(post1)
            postDao.insert(post2)
            repository.getPostsCashOnly(this.coroutineContext).addObserver().apply {
            //    assertItems(DataState.data(list))
            }
            val loaded = postDao.fetchListPost()

            assertThat(loaded[0], `is`(post1))

        }

    }

}
