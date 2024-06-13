package coroutine

import kotlinx.coroutines.*
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.system.measureTimeMillis

class UseCoroutine {
    private fun log(msg: Any?) {
        println("[${Thread.currentThread().name}] $msg")
    }

    /**
     * 对外接口
     */
    fun test() {
//        testGlobalScope()
//        testRunBlocking()
//        testRunCoroutineScope()
//        testRunSupervisorScope()
//        testCoroutineScope()
//        testCoroutineBuilder()
//        testCoroutineContext()
//        testDispatchers()
//        testCancel()
        testExceptionHandler()
    }


    private fun testBase() {
        /**
         * 作用域可以用于跟踪协程声明周期
         */
        GlobalScope.launch(context = Dispatchers.IO) {
            delay(1000)
            log("launch")
        }
        Thread.sleep(2000)
        log("end")
    }

    private fun testGlobalScope() {
        log("start")
        GlobalScope.launch {
            launch {
                delay(400)
                log("launch A")
            }
            launch {
                delay(300)
                log("launch B")
            }
            log("GlobalScope")
        }
        log("end")
        Thread.sleep(500)
    }

    private fun testRunBlocking() {
        log("start")
        runBlocking {
            launch {
                repeat(3) {
                    delay(100)
                    log("launchA - $it")
                }
            }
            launch {
                repeat(3) {
                    delay(100)
                    log("launchB - $it")
                }
            }
            GlobalScope.launch {
                repeat(3) {
                    delay(120)
                    log("GlobalScope - $it")
                }
            }
        }
        log("end")
    }

    private fun testRunCoroutineScope() {
        runBlocking {
            launch {
                delay(100)
                log("Task from runBlocking")
            }
            coroutineScope {
                launch {
                    delay(500)
                    log("Task from nested launch")
                }
                delay(50)
                log("Task from coroutine scope")
            }
            log("Coroutine scope is over")
        }
    }

    private fun testRunSupervisorScope() {
        runBlocking {
            launch {
                delay(100)
                log("Task from runBlocking")
            }
            supervisorScope {
                launch {
                    delay(500)
                    log("Task throw Exception")
                    throw Exception("failed")
                }
                launch {
                    delay(600)
                    log("Task from nested launch")
                }
            }
            log("Coroutine scope is over")
        }
    }

    /**
     * 创建自定义作用域管理协程
     */
    private fun testCoroutineScope() {
        runBlocking {
            val mainScope = MainScope()
            mainScope.launch {
                repeat(5) {
                    delay(1000L * it)
                }
            }

            delay(3000)
            mainScope.cancel()
        }
    }

    private fun testCoroutineBuilder() {
        runBlocking {
            /**
             * 使用 launch 启动
             */
            val job: Job = GlobalScope.launch(
                context = EmptyCoroutineContext,
                start = CoroutineStart.LAZY, // 懒启动
            ) {
                log("懒加载模式执行")
            }
            log("开启协程")
            job.start()

            /**
             * 使用 async 启动
             */
            val time = measureTimeMillis {
                runBlocking {
                    val asyncA: Deferred<Int> = async(start = CoroutineStart.LAZY) {
                        delay(3000)
                        1
                    }
                    val asyncB = async(start = CoroutineStart.LAZY) {
                        delay(4000)
                        2
                    }
                    // 如果没有下列代码，await 函数将以阻塞模式等待完成，即 A 、 B 将是阻塞顺序执行的
//                    asyncA.start()
//                    asyncB.start()
                    log(asyncA.await() + asyncB.await())
                }
            }
            log(time)
        }
    }

    private fun testCoroutineContext() {
        runBlocking {
            val job = Job()
            val scope = CoroutineScope(job + Dispatchers.IO) // "+" 可用于连接上下文
            runBlocking {
                log("Job is $job")
                val anotherJob = scope.launch {
                    try {
                        delay(3000)
                    } catch (e: CancellationException) {
                        log("job is cancelled")
                        throw e
                    }
                }
                log("AnotherJob is $anotherJob")
                log("end")
            }
            delay(1000)
            log("scope job is ${scope.coroutineContext[Job]}")
            scope.coroutineContext[Job]?.cancel()
        }
    }

    private fun testDispatchers() {
        runBlocking<Unit>(CoroutineName("krxkCoroutine")) {
            log("name")
            launch {
                log("main runBlocking")
            }
            launch(Dispatchers.Default) {
                log("Default")
                launch(Dispatchers.Unconfined) {
                    log("Unconfined 1")
                }
            }
            launch(Dispatchers.IO) {
                log("IO")
                launch(Dispatchers.Unconfined) {
                    log("Unconfined 2")
                }
            }
            launch(newSingleThreadContext("MyOwnThread")) {
                log("newSingleThreadContext")
                launch(Dispatchers.Unconfined) {
                    log("Unconfined 4")
                }
            }
            launch(Dispatchers.Unconfined) {
                log("Unconfined 3")
            }
            GlobalScope.launch {
                log("GlobalScope")
            }

            withContext(Dispatchers.IO) {
                log("withContext IO")
            }
            val krxkThread = newSingleThreadContext("krxkThread")
            withContext(krxkThread) {
                log("withContext krxk")
            }
            krxkThread.close() // 释放线程
        }
    }

    private fun testCancel() {
        /**
         * 可取消情况
         */
        runBlocking {
            val job = launch {
                repeat(1000) { i ->
                    log("job: I'm sleeping $i ...")
                    delay(500L)
                }
            }
            delay(1300L)
            log("main: I'm tired of waiting!")
            job.cancel()
            job.join()
            log("main: Now I can quit.")
        }

        /**
         * 不可取消的情况
         */
        log("------------------------")
        runBlocking {
            val startTime = System.currentTimeMillis()
            val job = launch(Dispatchers.Default) {
                var nextPrintTime = startTime
                var i = 0
                while (i < 5) {
                    if (isActive) { // 执行计算任务前判断是否已经被取消
                        if (System.currentTimeMillis() >= nextPrintTime) {
                            log("job: I'm sleeping ${i++} ...")
                            nextPrintTime += 500L
                        }
                    } else {
                        return@launch
                    }
                }
            }
            delay(1300L)
            log("main: I'm tired of waiting!")
            job.cancelAndJoin() // 取消是外部向协程发送取消请求，需要协程配合执行
            log("main: Now I can quit.")
        }

        /**
         * 协程库的 suspend 函数均会在检查是否已经被取消，被取消将抛出 CancellationException
         * 可利用 finally 释放资源
         */
        log("----------------------------------")
        runBlocking {
            val job = launch {
                try {
                    repeat(1000) { i ->
                        log("job: I'm sleeping $i ...")
                        delay(500L)
                    }
                } catch (e: Throwable) {
                    log("Exception: ${e.message}")
                } finally {
                    withContext(NonCancellable) {
                        /**
                         * 如果协程已经被取消，则不可再调用 suspend 函数，可理解为 上一层 suspend 函数已结束返回（这也容易理解父协程会等待所有协程结束再结束）
                         * 否则将会导致抛出 CancellationException
                         * 利用该上下文可以创建不可取消的协程域
                         */
                        delay(50)
                    }
                    log("job: I'm running finally")
                }
            }
            delay(1300L)
            log("main: I'm tired of waiting!")
            job.cancelAndJoin()
            log("main: Now I can quit.")
        }

        /**
         * 不可取消的协程作用域
         */
        log("-----------------------------------------")
        runBlocking {
            log("start")
            val launchA = launch {
                try {
                    repeat(5) {
                        delay(50)
                        log("launchA-$it")
                    }
                } finally {
                    delay(50)
                    log("launchA isCompleted")
                }
            }
            val launchB = launch {
                try {
                    repeat(5) {
                        delay(50)
                        log("launchB-$it")
                    }
                } finally {
                    withContext(NonCancellable) {
                        delay(50)
                        log("launchB isCompleted")
                    }
                }
            }
        }

        /**
         * 设置协程超时时间
         */
        log("------------------------------")
        runBlocking {
            log("start")
            val result = withTimeout(300) {
                repeat(5) {
                    delay(100)
                }
                200
            }
            log(result)
            log("end")
        }
    }

    private fun testExceptionHandler() {
        /**
         * launch 默认直接抛出异常
         * async 默认直接忽略异常，直到 await 被调用才抛出异常
         */
        val ioScope = CoroutineScope(Dispatchers.IO)
        ioScope.async {
            delay(500)
            log("taskA throw AssertionError")
            throw AssertionError()
        }

        /**
         * CoroutineExceptionHandler
         */
        log("------------------------------------")
        runBlocking {
            val handler = CoroutineExceptionHandler { _, exception ->
                log("Caught $exception")
            }
            val job = GlobalScope.launch(handler) {
                throw AssertionError()
            }
            val deferred = GlobalScope.async(handler) {
                throw ArithmeticException()
            }
            joinAll(job, deferred)
        }

        /**
         * 单向取消
         */
        log("----------------------------------")
        runBlocking {
            val supervisor = SupervisorJob()
            with(CoroutineScope(coroutineContext + supervisor)) {
                val firstChild = launch(CoroutineExceptionHandler { _, _ -> }) {
                    log("First child is failing")
                    throw AssertionError("First child is cancelled")
                }
                val secondChild = launch {
                    firstChild.join()
                    log("First child is cancelled: ${firstChild.isCancelled}, but second one is still active")
                    try {
                        delay(Long.MAX_VALUE)
                    } finally {
                        log("Second child is cancelled because supervisor is cancelled")
                    }
                }
                firstChild.join()
                log("Cancelling supervisor")
                //取消所有协程
                supervisor.cancel()
                secondChild.join()
            }
        }
    }

    /**
     * suspend 函数只可在 scope 或 suspend 函数内调用
     */
    suspend fun fetchDocs() {
        val result = get("https://developer.android.com")
        println(result)
    }

    suspend fun get(url: String) = withContext(Dispatchers.IO) {
        return@withContext "result got!"
    }
}